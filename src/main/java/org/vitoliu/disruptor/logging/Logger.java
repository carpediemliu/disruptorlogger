package org.vitoliu.disruptor.logging;

/**
 * 日志写入器
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface Logger {
	void write(LoggerEvent event);
}
