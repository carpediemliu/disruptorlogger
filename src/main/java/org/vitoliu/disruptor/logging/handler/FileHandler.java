package org.vitoliu.disruptor.logging.handler;

/**
 *
 * 日志文件
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface FileHandler {

	void close();

	void write(byte[] data);

	void flush();

	String getName();

	long length();

	long initialTime();
}
