package org.vitoliu.disruptor.logging.roll.pattern.format;

/**
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class IndexFormatter implements Formatter {
	@Override
	public void format(StringBuilder source, Object... arguments) {
		for (Object argument : arguments) {
			if (argument instanceof Integer) {
				source.append(argument);
				break;
			}
		}
	}
}
