package org.vitoliu.disruptor.logging.exception;

import org.vitoliu.disruptor.logging.LoggerEvent;

/**
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface ExceptionHandler {

	void handleEventException(String msg, Throwable ex, LoggerEvent event);

	void handleException(String msg, Throwable ex);

	void handleEvent(String msg, LoggerEvent loggerEvent);

	void handle(String msg);
}
