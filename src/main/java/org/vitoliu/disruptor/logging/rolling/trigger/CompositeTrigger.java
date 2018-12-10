package org.vitoliu.disruptor.logging.rolling.trigger;

import org.vitoliu.disruptor.logging.LoggerEvent;
import org.vitoliu.disruptor.logging.handler.RollingFileHandler;

/**
 *
 * @author vito.liu
 * @since 11 十二月 2018
 */
public class CompositeTrigger<T extends RollingFileHandler> implements Trigger<T> {

	private final Trigger<T>[] triggers;

	@SafeVarargs
	public CompositeTrigger(Trigger<T>... triggers) {
		this.triggers = triggers;
	}


	@Override
	public void initialize(T handler) {
		for (Trigger<T> trigger : triggers) {
			trigger.initialize(handler);
		}
	}

	@Override
	public boolean isTriggeringEvent(LoggerEvent loggerEvent) {
		for (Trigger<T> trigger : triggers) {
			if (trigger.isTriggeringEvent(loggerEvent)) {
				return true;
			}
		}
		return false;
	}
}
