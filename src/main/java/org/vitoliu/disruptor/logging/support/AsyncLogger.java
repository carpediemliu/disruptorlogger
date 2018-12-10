package org.vitoliu.disruptor.logging.support;

import java.util.concurrent.atomic.AtomicInteger;

import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import org.vitoliu.disruptor.concurrent.async.SingleFlusher;
import org.vitoliu.disruptor.logging.Logger;
import org.vitoliu.disruptor.logging.LoggerEvent;
import org.vitoliu.disruptor.logging.exception.ExceptionHandler;

/**
 *
 * 异步logger模型
 * @author vito.liu
 * @since 08 十二月 2018
 */
public class AsyncLogger implements Logger {

	private final SingleFlusher<LoggerEvent> flusher;

	private final Logger logger;

	private final ExceptionHandler exceptionHandler;

	private final AddAction addAction;

	public AsyncLogger(Logger logger, ExceptionHandler exceptionHandler, AddAction addAction, WaitStrategy waitStrategy, ProducerType producerType, int bufferSize, int notifySize) {
		this.logger = logger;
		this.exceptionHandler = exceptionHandler;
		this.addAction = addAction;
		SingleFlusher.Builder<LoggerEvent> builder = new SingleFlusher.Builder<LoggerEvent>().setBufferSize(bufferSize).setNotifySize(notifySize)
				.setProducerType(producerType).setWaitStrategy(waitStrategy)
				.setNamePrefix("asyncLogger");
		builder.addListenerGroup(new LoggerEventListener());
		this.flusher = builder.build();
	}


	private void syncWrite(LoggerEvent event, boolean endOfBatch) {
		this.logger.write(event, endOfBatch);
	}

	@Override
	public void write(LoggerEvent event) {
		addAction.add(flusher, event, exceptionHandler);
	}

	@Override
	public void write(LoggerEvent event, boolean endOfBatch) {
		if (endOfBatch) {
			syncWrite(event, true);
		}
		else {
			write(event);
		}
	}

	@Override
	public void close() {
		this.flusher.shutdown();
		this.logger.close();
	}

	/**
	 * 添加行为，用于控制刷新起添加日志的策略（等待写入，尝试写入，丢弃日志等）
	 *
	 */
	public interface AddAction {
		/**
		 * 添加行为
		 * @param flusher
		 * @param event
		 * @param handler
		 */
		void add(SingleFlusher<LoggerEvent> flusher, LoggerEvent event, ExceptionHandler handler);
	}

	/**
	 * 默认添加行为。
	 * 小于0：等待写入
	 * 等于0：尝试写入
	 * 大于1：先进行尝试写入，写入失败后进行等待写入，超过尝试次数，进行丢弃。
	 */
	public static class DefaultAddAction implements AddAction {
		/**
		 * 行为标识
		 */
		private final AtomicInteger syncPerm = new AtomicInteger(0);

		private volatile int syncSize = -1;


		@Override
		public void add(SingleFlusher<LoggerEvent> flusher, LoggerEvent event, ExceptionHandler handler) {
			if (syncSize < 0) {
				flusher.add(event);
				return;
			}
			if (syncSize == 0) {
				if (!flusher.tryAdd(event)) {
					handler.handleEvent("discard", event);
				}
				return;
			}
			if (flusher.tryAdd(event)) {
				syncPerm.set(0);
			}
			else if (syncPerm.incrementAndGet() <= syncSize) {
				flusher.add(event);
			}
			else {
				handler.handleEvent("discard", event);
			}
		}
	}

	public class LoggerEventListener implements SingleFlusher.EventListener<LoggerEvent> {

		@Override
		public void onException(Throwable e, long sequence, LoggerEvent event) {
			AsyncLogger.this.exceptionHandler.handleEventException(e.getMessage(), e, event);
		}

		@Override
		public void onEvent(LoggerEvent event, boolean endOfBatch) {
			syncWrite(event, endOfBatch);
		}
	}

}
