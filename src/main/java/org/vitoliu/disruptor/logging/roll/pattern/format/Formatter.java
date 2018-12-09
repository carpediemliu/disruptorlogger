package org.vitoliu.disruptor.logging.roll.pattern.format;

/**
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface Formatter {

	void format(StringBuilder source,Object... arguments);
}
