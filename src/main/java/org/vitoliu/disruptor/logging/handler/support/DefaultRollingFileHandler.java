package org.vitoliu.disruptor.logging.handler.support;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.Clock;
import java.util.concurrent.Semaphore;
import java.util.logging.Handler;

import org.vitoliu.disruptor.logging.LoggerEvent;
import org.vitoliu.disruptor.logging.exception.ExceptionHandler;
import org.vitoliu.disruptor.logging.handler.RollingFileHandler;
import org.vitoliu.disruptor.logging.roll.action.AbstractAction;
import org.vitoliu.disruptor.logging.roll.pattern.format.Action;
import org.vitoliu.disruptor.logging.roll.pattern.format.FilePattern;
import org.vitoliu.disruptor.logging.roll.pattern.format.Rollover;
import org.vitoliu.disruptor.logging.rolling.trigger.Trigger;

/**
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class DefaultRollingFileHandler extends DefaultFileHandler implements RollingFileHandler {

	private final Semaphore semaphore = new Semaphore(1);

	final Trigger<RollingFileHandler> trigger;

	private final Rollover rollover;

	private final FilePattern filePattern;


	public DefaultRollingFileHandler(String fileName, Rollover rollover, Trigger<RollingFileHandler> trigger, FilePattern filePattern, boolean isAppend, int bufferSize, boolean useDirectMemory, Clock clock, ExceptionHandler exceptionHandler) {
		super(fileName, isAppend, bufferSize, useDirectMemory, clock, exceptionHandler);
		this.trigger = trigger;
		this.rollover = rollover;
		this.filePattern = filePattern;
	}

	private boolean rolling() {
		try {
			semaphore.acquire();
		}
		catch (InterruptedException e) {
			exceptionHandler.handleException("thread interrupted when rolling!", e);
			return false;
		}

		boolean success = false;
		Thread thread = null;

		try {
			Rollover.Description descriptor = this.rollover.rollover(this.fileName, this.filePattern);
			if (descriptor != null) {
				super.close();
				//同步轮转
				if (descriptor.getSync() != null) {
					success = descriptor.getSync().execute();
				}
			}
			//异步轮转
			if (success && descriptor.getASync() != null) {
				//异步线程启动轮转行为，无需用线程池。
				thread = new Thread(new AsyncAction(descriptor.getASync()));
				thread.setDaemon(true);
				thread.start();
			}
			return true;
		}
		catch (Exception e) {
			exceptionHandler.handleException("error in synchronous task!", e);
			return false;
		}
		finally {
			if (thread == null || !thread.isAlive()) {
				semaphore.release();
			}
		}

	}


	@Override
	public void initialize() {
		trigger.initialize(this);
	}

	@Override
	public void checkRollOver(LoggerEvent loggerEvent) {
		if (trigger.isTriggeringEvent(loggerEvent)) {
			size = 0;
			initialTime = clock.millis();
			try {
				this.fileChannel = new FileOutputStream(fileName).getChannel();
			}
			catch (FileNotFoundException e) {
				exceptionHandler.handleException("create RandomAccessFile error!", e);
			}
		}
	}

	@Override
	public FilePattern getFilePattern() {
		return this.filePattern;
	}

	protected class AsyncAction extends AbstractAction {

		private final Action action;

		public AsyncAction(Action action) {
			super(DefaultRollingFileHandler.this.exceptionHandler);
			this.action = action;
		}

		@Override
		public boolean execute() throws Exception {
			try {
				return action.execute();
			}
			finally {
				semaphore.release();
			}
		}

		@Override
		public void close() {
			action.close();
		}

		@Override
		public boolean isCompleted() {
			return action.isCompleted();
		}
	}
}
