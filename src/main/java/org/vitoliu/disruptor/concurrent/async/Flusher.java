package org.vitoliu.disruptor.concurrent.async;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceReportingEventHandler;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
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
 * @since 08 十二月 2018
 */
public class Flusher<T> {
	private final Disruptor<Holder> disruptor;

	private final int MAX_AVAIABLE_CAPACITY = 200;

	private final List<EventListener<T>[]> listenerGroups;

	private final EventTranslatorOneArg<Holder, T> eventTranslator;

	/**
	 * 根据自己的情况设置线程池的大小
	 */
	private final ExecutorService executorService;

	private final boolean resetHolder;

	private volatile RingBuffer<Holder> ringBuffer;

	private Flusher(Builder<T> builder) {
		//根据自己实际情况设置线程池大小
		//Common Thread Pool
		ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("flusher-" + builder.namePrefix + "-pool-%d").build();
		executorService = new ThreadPoolExecutor(10, 50,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(200), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
		EventFactory<Holder> eventFactory = new HolderEventFactory();

		this.listenerGroups = builder.listenerGroups;
		this.eventTranslator = new HolderEventTranslator();

		int bufferSize = builder.bufferSize;
		ProducerType producerType = builder.producerType;
		WaitStrategy waitStrategy = builder.waitStrategy;
		Disruptor<Holder> disruptor = new Disruptor<>(eventFactory, bufferSize, executorService, producerType, waitStrategy);
		disruptor.handleExceptionsWith(new HolderExceptionHandler());

		final int notifySize = builder.notifySize;
		List<EventHandler<Holder>[]> handlerGroups = listenerGroups.stream().map(input -> {
			@SuppressWarnings("unchecked")
			EventHandler<Holder>[] result = new EventHandler[input.length];
			for (int i = 0; i < input.length; i++) {
				result[i] = new HolderEventHandler(input[i], notifySize);
			}
			return result;
		}).collect(Collectors.toList());
		EventHandlerGroup<Holder> handlerGroup = disruptor.handleEventsWith(handlerGroups.get(0));
		for (int i = 1; i < listenerGroups.size(); i++) {
			handlerGroup = handlerGroup.then(handlerGroups.get(i));
		}

		resetHolder = handlerGroups.size() <= 1;

		this.ringBuffer = disruptor.start();
		this.disruptor = disruptor;
	}

	private static boolean hasAvailableCapacity(RingBuffer<?> ringBuffer) {
		return !ringBuffer.hasAvailableCapacity(ringBuffer.getBufferSize());
	}

	@SafeVarargs
	private static <T> void process(List<EventListener<T>[]> listenerGroups, Throwable e, T... events) {
		for (T event : events) {
			process(listenerGroups, e, event);
		}
	}

	private static <T> void process(List<EventListener<T>[]> listenerGroups, Throwable e, T event) {
		for (EventListener<T>[] listeners : listenerGroups) {
			for (EventListener<T> listener : listeners) {
				listener.onException(e, -1, event);
			}
		}
	}

	public void add(T event) {
		RingBuffer<Holder> temp = ringBuffer;
		if (temp == null) {
			process(this.listenerGroups, new IllegalStateException("disruptor is closed!"), event);
			return;
		}

		//关闭后会产生NullPointerException.
		try {
			//不使用临时变量，确保及时设置null
			ringBuffer.publishEvent(eventTranslator, event);
		}
		catch (NullPointerException e) {
			process(this.listenerGroups, new IllegalStateException("disruptor is closed!"), event);
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
		for (int i = 0; hasAvailableCapacity(temp) && i < MAX_AVAIABLE_CAPACITY; i++) {
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


	public static class Builder<T> {

		private ProducerType producerType = ProducerType.MULTI;

		private int bufferSize = 2 * 1024;

		private int notifySize = 50;

		private String namePrefix = "";

		private WaitStrategy waitStrategy = new BlockingWaitStrategy();

		private List<EventListener<T>[]> listenerGroups = Lists.newArrayList();

		@SafeVarargs
		public final Builder<T> addListenerGroup(EventListener<T>... listenerGroup) {
			this.listenerGroups.add(Preconditions.checkNotNull(listenerGroup));
			Preconditions.checkArgument(listenerGroup.length != 0);
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
			Preconditions.checkArgument(notifySize > 0);
			this.bufferSize = bufferSize;
			return this;
		}

		public Builder<T> setNotifySize(int notifySize) {
			Preconditions.checkArgument(notifySize > 0);
			this.notifySize = notifySize;
			return this;
		}

		private int getThreads() {
			int i = 0;
			for (EventListener<T>[] listenerGroup : listenerGroups) {
				i += listenerGroup.length;
			}
			return i;
		}

		public Flusher<T> build() {
			Preconditions.checkArgument(!listenerGroups.isEmpty());
			return new Flusher<>(this);
		}
	}

	private class Holder {
		private volatile T event;

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

	private class HolderEventHandler implements SequenceReportingEventHandler<Holder> {

		private int notifySize;

		private final EventListener<T> listener;

		private Sequence sequence;

		private int counter;

		public HolderEventHandler(EventListener<T> listener, int notifySize) {
			this.listener = listener;
			this.notifySize = notifySize;
		}

		@Override
		public void setSequenceCallback(Sequence sequence) {
			this.sequence = sequence;
		}

		@Override
		public void onEvent(Holder event, long sequence, boolean endOfBatch) throws Exception {
			try {
				listener.onEvent(event.event, endOfBatch);
				if (++counter > notifySize) {
					this.sequence.set(sequence);
					counter = 0;
				}
			}
			catch (Throwable e) {
				try {
					listener.onException(e, sequence, event.event);
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				finally {
					this.sequence.set(sequence);
				}
			}
			finally {
				if (resetHolder) {
					event.setEvent(null);
				}
			}
		}
	}

	private class HolderExceptionHandler implements ExceptionHandler {

		@Override
		public void handleEventException(Throwable ex, long sequence, Object event) {
			@SuppressWarnings("unchecked")
			Holder holder = (Holder) event;
			throw new UnsupportedOperationException("Sequence: " + sequence + ". Event: " + (holder == null ? null : holder.event), ex);
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
		 * @param endOfBatch
		 * @throws Exception
		 */
		void onEvent(T event, boolean endOfBatch) throws Exception;
	}
}
