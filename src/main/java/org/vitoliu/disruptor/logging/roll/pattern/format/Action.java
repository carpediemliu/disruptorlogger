package org.vitoliu.disruptor.logging.roll.pattern.format;

/**
 *
 * 轮转动作
 * @author vito.liu
 * @since 09 十二月 2018
 */
public interface Action extends Runnable {

	/**
	 * 执行轮转
	 * @return
	 * @throws Exception
	 */
	boolean execute() throws Exception;

	/**
	 * 结束轮转
	 */
	void close();

	/**
	 * 轮转状态 true表示完成
	 * @return
	 */
	boolean isCompleted();
}
