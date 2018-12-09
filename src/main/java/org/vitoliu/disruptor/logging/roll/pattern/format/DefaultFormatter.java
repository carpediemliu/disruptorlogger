package org.vitoliu.disruptor.logging.roll.pattern.format;

/**
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class DefaultFormatter implements Formatter {

	private final String pattern;

	public DefaultFormatter(String pattern) {
		this.pattern = pattern;
	}


	@Override
	public void format(StringBuilder source, Object... arguments) {
		source.append(this.pattern);
	}
}
