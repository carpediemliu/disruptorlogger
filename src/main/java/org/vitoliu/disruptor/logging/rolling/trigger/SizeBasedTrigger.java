package org.vitoliu.disruptor.logging.rolling.trigger;

import org.vitoliu.disruptor.logging.LoggerEvent;
import org.vitoliu.disruptor.logging.handler.RollingFileHandler;
import org.vitoliu.disruptor.logging.roll.pattern.format.FilePattern;

/**
 *
 * @author vito.liu
 * @since 11 十二月 2018
 */
public class SizeBasedTrigger<T extends RollingFileHandler> implements Trigger<T> {

	private final int interval;

	private final boolean modulate;

	private FilePattern filePattern;

	private T handler;

	private long nextRollover;


	public SizeBasedTrigger(int interval, boolean modulate) {
		this.interval = interval;
		this.modulate = modulate;
	}

	@Override
	public void initialize(T handler) {
		this.handler = handler;
		this.filePattern = handler.getFilePattern();

		//初始化nextFileTime
		filePattern.getNextTime(handler.initialTime(), interval, modulate);
		//初始化prevFileTime
		filePattern.getNextTime(handler.initialTime(), interval, modulate);
	}


	@Override
	public boolean isTriggeringEvent(LoggerEvent loggerEvent) {
		if (handler.length() == 0) {
			return false;
		}
		final long now = loggerEvent.getTimeMills();
		if (now > nextRollover) {
			nextRollover = filePattern.getNextTime(now, interval, modulate);
			return true;
		}
		return false;
	}
}
