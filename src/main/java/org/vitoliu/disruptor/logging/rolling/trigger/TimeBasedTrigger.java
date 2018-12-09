package org.vitoliu.disruptor.logging.rolling.trigger;

import org.vitoliu.disruptor.logging.LoggerEvent;
import org.vitoliu.disruptor.logging.handler.RollingFileHandler;
import org.vitoliu.disruptor.logging.roll.pattern.format.FilePattern;

/**
 *
 * 基于时间的触发器
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class TimeBasedTrigger<T extends RollingFileHandler> implements Trigger<T> {

	private final int interval;

	private final boolean modulate;

	private FilePattern filePattern;

	private long nextRollover;

	private T handler;


	public TimeBasedTrigger(int interval, boolean modulate) {
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
		nextRollover = filePattern.getNextTime(handler.initialTime(), interval, modulate);
	}

	@Override
	public boolean isTriggeringEvent(LoggerEvent loggerEvent) {
		return false;
	}
}
