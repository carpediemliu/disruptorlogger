package org.vitoliu.disruptor.logging.handler;

import org.vitoliu.disruptor.logging.LoggerEvent;
import org.vitoliu.disruptor.logging.roll.pattern.format.FilePattern;

/**
 *
 * 参考Log4J里面可轮转日志文件
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface RollingFileHandler extends FileHandler {

	void initialize();

	void checkRollOver(LoggerEvent loggerEvent);

	FilePattern getFilePattern();
}
