package org.vitoliu.disruptor.logging.roll.action;

import org.vitoliu.disruptor.logging.exception.ExceptionHandler;
import org.vitoliu.disruptor.logging.roll.pattern.format.Action;

/**
 *
 * 轮转行为的抽象实现
 * @author vito.liu
 * @since 09 十二月 2018
 */
public abstract class AbstractAction implements Action {

	protected final ExceptionHandler exceptionHandler;

	private boolean complete = false;

	private boolean interrupted = false;

	public AbstractAction(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}


	@Override
	public void run() {
		if (!interrupted) {
			try {
				execute();
			}
			catch (Exception e) {
				exceptionHandler.handleException(this.getClass().getName() + " execute error.", e);
			}
			complete = true;
			interrupted = true;
		}
	}


	@Override
	public abstract boolean execute() throws Exception;


	@Override
	public void close() {
		interrupted = true;
	}

	@Override
	public boolean isCompleted() {
		return complete;
	}

}
