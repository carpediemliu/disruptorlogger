package org.vitoliu.disruptor.logging.roll.pattern.format;

/**
 *
 * 轮转器
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface Rollover {

	/**
	 *
	 * @param fileName
	 * @param pattern
	 * @return
	 */
	Description rollover(String fileName, FilePattern pattern);

	/**
	 * 描述
	 */
	interface Description {
		/**
		 * 同步轮转
		 * @return {@link Action}
		 */
		Action getSync();

		/**
		 * 异步轮转
		 * @return {@link Action}
		 */
		Action getASync();
	}
}
