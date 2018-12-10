package org.vitoliu.disruptor.logging;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import org.vitoliu.disruptor.logging.exception.ExceptionHandler;
import org.vitoliu.disruptor.logging.handler.FileHandler;
import org.vitoliu.disruptor.logging.handler.RollingFileHandler;
import org.vitoliu.disruptor.logging.handler.support.DefaultFileHandler;
import org.vitoliu.disruptor.logging.roll.pattern.format.FilePattern;
import org.vitoliu.disruptor.logging.rolling.trigger.CompositeTrigger;
import org.vitoliu.disruptor.logging.rolling.trigger.SizeBasedTrigger;
import org.vitoliu.disruptor.logging.rolling.trigger.TimeBasedTrigger;
import org.vitoliu.disruptor.logging.rolling.trigger.Trigger;
import org.vitoliu.disruptor.logging.support.AsyncLogger;
import org.vitoliu.disruptor.logging.support.DefaultLogger;

/**
 * {@link Logger} 构造器
 * @author vito.liu
 * @since 09 十二月 2018
 */
public abstract class LoggerBuilder {

	private static DefaultLoggerBuilder of(String fileName, ExceptionHandler exceptionHandler) {
		return new DefaultLoggerBuilder(fileName, exceptionHandler);
	}

	protected abstract Logger build();

	public static class DefaultLoggerBuilder extends LoggerBuilder {

		private boolean immeiadteFlush = false;

		private String fileName;

		private boolean isAppend = true;

		private int fileBufferSize = 512 * 1024;

		private boolean useDirectMemory = true;

		Clock clock = Clock.systemDefaultZone();

		private ExceptionHandler exceptionHandler;

		public DefaultLoggerBuilder(String fileName, ExceptionHandler exceptionHandler) {
			this.fileName = Preconditions.checkNotNull(fileName);
			this.exceptionHandler = exceptionHandler;
		}

		private RollingLoggerBuilder rolling(String filePattern) {
			return new RollingLoggerBuilder(this, filePattern);
		}

		@Override
		protected Logger build() {
			FileHandler fileHandler = new DefaultFileHandler(fileName, isAppend, fileBufferSize, useDirectMemory, clock, exceptionHandler);
			return new DefaultLogger<>(immeiadteFlush, fileHandler);
		}

		public static class RollingLoggerBuilder extends LoggerBuilder {

			private final DefaultLoggerBuilder builder;

			private String filePattern;

			private long maxFileSize = 50 * 1024 * 1024;

			private int interval = 1;

			private boolean modulate = true;

			private int minIndex = 1;

			private int maxIndex = 30;

			private boolean useMax = false;

			private int compressionBufferSize = 512 * 1024;

			public RollingLoggerBuilder(DefaultLoggerBuilder defaultLoggerBuilder, String filePattern) {
				this.builder = Preconditions.checkNotNull(defaultLoggerBuilder);
				this.filePattern = Preconditions.checkNotNull(filePattern);
			}

			public AsyncLoggerBuilder async() {
				return new AsyncLoggerBuilder(build(), builder.exceptionHandler);
			}

			@Override
			public Logger build() {
				ExceptionHandler exceptionHandler = builder.exceptionHandler;
				String fileName = builder.fileName;
				boolean isAppend = builder.isAppend;
				boolean useDirectMemory = builder.useDirectMemory;
				int fileBufferSize = builder.fileBufferSize;
				Clock clock = builder.clock;
				boolean immeiadteFlush = builder.immeiadteFlush;

				FilePattern filePattern = new FilePattern(this.filePattern, clock);
				List<Trigger<RollingFileHandler>> triggers = Lists.newArrayListWithCapacity(2);
				if (maxFileSize > 0) {
					triggers.add(new TimeBasedTrigger<>(interval, modulate));
				}
				Trigger<RollingFileHandler> trigger = new CompositeTrigger<>(triggers.toArray(new Trigger[triggers.size()]));

				//TODO:滚动策略

				DefaultFileHandler fileHandler = new DefaultFileHandler(fileName, isAppend, fileBufferSize, useDirectMemory, clock, exceptionHandler);
				//TODO:返回一个滚动日志对象
				return null;
			}
		}
	}

	private static class AsyncLoggerBuilder extends LoggerBuilder {
		private final Logger logger;

		private final ExceptionHandler exceptionHandler;

		private WaitStrategy waitStrategy = new BlockingWaitStrategy();

		private ProducerType producerType = ProducerType.MULTI;

		private int bufferSize = 512 * 1024;

		private int notifySize = 1024;

		private AsyncLogger.AddAction addAction = new AsyncLogger.DefaultAddAction();

		public AsyncLoggerBuilder(Logger build, ExceptionHandler exceptionHandler) {
			this.logger = Preconditions.checkNotNull(build);
			this.exceptionHandler = Preconditions.checkNotNull(exceptionHandler);
		}

		@Override
		protected Logger build() {
			return new AsyncLogger(logger, exceptionHandler, addAction, waitStrategy, producerType, bufferSize, notifySize);
		}
	}
}
