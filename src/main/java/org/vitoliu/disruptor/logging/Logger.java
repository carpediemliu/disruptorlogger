package org.vitoliu.disruptor.logging;

/**
 * 日志写入器
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface Logger {
	void write(LoggerEvent event);
	void write(LoggerEvent event,boolean endOfBatch);
	void  close();

	/**
	 * 日志写入器
	 */
	interface Filter{
		boolean isWrite(LoggerEvent event);
	}
}
