package org.vitoliu.disruptor.logging;

import java.io.Serializable;

/**
 *
 * 日志事件
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface LoggerEvent extends Serializable {

	long getTimeMills();

	byte[] toByteArray();
}
