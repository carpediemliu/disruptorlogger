package org.vitoliu.disruptor.logging.rolling.trigger;

import org.vitoliu.disruptor.logging.LoggerEvent;
import org.vitoliu.disruptor.logging.handler.RollingFileHandler;

/**
 *
 * 触发器
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface Trigger<T extends RollingFileHandler> {

	void initialize(T handler);

	boolean isTriggeringEvent(LoggerEvent loggerEvent);
}
