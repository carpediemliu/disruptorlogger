package org.vitoliu.disruptor.concurrent.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

/**
 *
 * 异步刷新器，基于生产者/消费者模式（无锁队列）。同一事件被所有消费者消费，多个消费者之间可控制消费顺序。支持批量通知模式
 *
 * 策略选择：performance: higher->lower
 * {@link com.lmax.disruptor.BusySpinWaitStrategy}:自旋等待，延迟最低，占用资源相对最多;用于处理线程数小于物理内核的场景（禁用超线程）
 * {@link com.lmax.disruptor.YieldingWaitStrategy}:忙循环等待，默认前100次只检测，之后让出CPU，等待下次调度;低延迟，平均了资源占用;推荐使用
 * {@link com.lmax.disruptor.SleepingWaitStrategy}:忙循环等待，默认前100次只检测，后100次让出CPU，等待下次调度，之后每次sleep 1ns。平均了使用率，但是延迟不均衡
 * {@link com.lmax.disruptor.BlockingWaitStrategy}:锁和条件;CPU使用率最低，适用于吞吐量和延迟要求不高的场景。
 *
 * <pre>
 *    生产者类型：
 *    Single:单个生产者，生产端完全无锁。
 *    Multi:多个生产者，底层通过AtomicLong进行资源管理。
 * </pre>
 * <pre>
 *     事件监听器:
 *     EventListener接口:提供处理单个实体的方法
 *     AbstractBatchEventListener:抽象类，EventListener实现,提供聚合为批量
 * </pre>
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class ParallelFlusher<T> {

	private final Disruptor<Holder> disruptor;

	private final EventListener<T> eventListener;

	private final EventTranslatorOneArg<Holder, T> eventTranslator;

	/**
	 * 根据自己的情况设置线程池的大小
	 */
	private final ExecutorService executorService;


	private volatile RingBuffer<Holder> ringBuffer;

	private ParallelFlusher(Builder<T> builder) {
		//根据自己实际情况设置线程池大小
		//Common Thread Pool
		ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("parallelFlusher-" + builder.namePrefix + "-pool-%d").build();
		executorService = new ThreadPoolExecutor(10, 50,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(200), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
		EventFactory<Holder> eventFactory = new HolderEventFactory();
		HolderExceptionHandler exceptionHandler = new HolderExceptionHandler();
		this.eventListener = builder.listener;
		this.eventTranslator = new HolderEventTranslator();

		int bufferSize = builder.bufferSize;
		ProducerType producerType = builder.producerType;
		WaitStrategy waitStrategy = builder.waitStrategy;
		Disruptor<Holder> disruptor = new Disruptor<>(eventFactory, bufferSize, executorService, producerType, waitStrategy);
		disruptor.handleExceptionsWith(exceptionHandler);

		WorkHandler[] workHandlers = new WorkHandler[builder.threads];
		for (int i = 0; i < workHandlers.length; i++) {
			workHandlers[i] = new HolderWorkHandler();
		}

		disruptor.handleEventsWithWorkerPool(workHandlers);

		this.ringBuffer = disruptor.start();
		this.disruptor = disruptor;
	}

	private static boolean hasAvailableCapacity(RingBuffer ringBuffer) {
		return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
	}

	@SafeVarargs
	private static <T> void process(EventListener<T> listener, Throwable e, T... events) {
		for (T event : events) {
			process(listener, e, event);
		}
	}

	private static <T> void process(EventListener<T> listener, Throwable e, T event) {
		listener.onException(e, -1, event);
	}

	public void add(T event) {
		RingBuffer<Holder> temp = ringBuffer;
		if (temp == null) {
			process(this.eventListener, new IllegalStateException("disruptor is closed!"), event);
			return;
		}

		//关闭后会产生NullPointerException.
		try {
			//不使用临时变量，确保及时设置null
			ringBuffer.publishEvent(eventTranslator, event);
		}
		catch (NullPointerException e) {
			process(this.eventListener, new IllegalStateException("disruptor is closed!"), event);
		}
	}

	public boolean tryAdd(T event) {
		RingBuffer<Holder> temp = ringBuffer;
		if (temp == null) {
			return false;
		}
		//关闭后会产生NullPointerException.
		try {
			//不使用临时变量，确保及时设置null
			return ringBuffer.tryPublishEvent(eventTranslator, event);
		}
		catch (NullPointerException e) {
			return false;
		}
	}

	public void shutdown() {
		RingBuffer<Holder> temp = ringBuffer;
		ringBuffer = null;
		if (temp == null) {
			return;
		}
		final int maxAvailableCapacity = 200;
		for (int i = 0; hasAvailableCapacity(temp) && i < maxAvailableCapacity; i++) {
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {

			}
		}
		disruptor.shutdown();

		executorService.shutdown();

		try {
			executorService.awaitTermination(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}


	private class Holder {
		private T event;

		private void setEvent(T event) {
			this.event = event;
		}

		@Override
		public String toString() {
			return "Holder{" +
					"event=" + event +
					'}';
		}
	}


	public static class Builder<T> {

		private ProducerType producerType = ProducerType.MULTI;

		private int bufferSize = 2 * 1024;

		private int threads = 1;

		private String namePrefix = "";

		private WaitStrategy waitStrategy = new YieldingWaitStrategy();

		private EventListener<T> listener;

		public Builder<T> setThreads(int threads) {
			Preconditions.checkArgument(threads > 0);
			this.threads = threads;
			return this;
		}

		public final Builder<T> setListener(EventListener<T> listener) {
			this.listener = Preconditions.checkNotNull(listener);
			return this;
		}

		public Builder<T> setNamePrefix(String namePrefix) {
			this.namePrefix = Preconditions.checkNotNull(namePrefix);
			return this;
		}

		public Builder<T> setWaitStrategy(WaitStrategy waitStrategy) {
			this.waitStrategy = Preconditions.checkNotNull(waitStrategy);
			return this;
		}

		public Builder<T> setProducerType(ProducerType producerType) {
			this.producerType = Preconditions.checkNotNull(producerType);
			return this;
		}

		public Builder<T> setBufferSize(int bufferSize) {
			Preconditions.checkArgument(bufferSize > 0);
			this.bufferSize = bufferSize;
			return this;
		}

		private ParallelFlusher<T> build() {
			Preconditions.checkNotNull(listener);
			return new ParallelFlusher<>(this);
		}
	}


	private class HolderEventFactory implements EventFactory<Holder> {

		@Override
		public Holder newInstance() {
			return new Holder();
		}
	}

	private class HolderEventTranslator implements EventTranslatorOneArg<Holder, T> {

		@Override
		public void translateTo(Holder event, long sequence, T arg0) {
			event.setEvent(arg0);
		}
	}

	private class HolderWorkHandler implements WorkHandler<Holder> {

		@Override
		public void onEvent(Holder event) throws Exception {
			eventListener.onEvent(event.event);
			event.setEvent(null);
		}
	}

	private class HolderExceptionHandler implements ExceptionHandler {

		@Override
		public void handleEventException(Throwable ex, long sequence, Object event) {
			@SuppressWarnings("unchecked")
			Holder holder = (Holder) event;
			try {
				eventListener.onException(ex, sequence, holder.event);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				holder.setEvent(null);
			}
		}

		@Override
		public void handleOnStartException(Throwable ex) {
			throw new UnsupportedOperationException(ex);
		}

		@Override
		public void handleOnShutdownException(Throwable ex) {
			throw new UnsupportedOperationException(ex);
		}
	}

	public interface EventListener<T> {
		/**
		 *  异常触发
		 * @param e
		 * @param sequence
		 * @param event
		 */
		void onException(Throwable e, long sequence, T event);

		/**
		 * 事件触发
		 * @param event
		 * @throws Exception
		 */
		void onEvent(T event) throws Exception;
	}

}
