package org.vitoliu.disruptor.logging.support;

import org.vitoliu.disruptor.logging.Logger;
import org.vitoliu.disruptor.logging.LoggerEvent;
import org.vitoliu.disruptor.logging.handler.FileHandler;

/**
 *
 * @author vito.liu
 * @since 10 十二月 2018
 */
public class DefaultLogger<T extends FileHandler> implements Logger {

	private boolean immediateFlush;

	private final T handler;

	public DefaultLogger(boolean immediateFlush, T handler) {
		this.immediateFlush = immediateFlush;
		this.handler = handler;
	}

	@Override
	public void write(LoggerEvent event) {
		write(event, true);
	}

	@Override
	public void write(LoggerEvent event, boolean endOfBatch) {
		byte[] bytes = event.toByteArray();
		if (bytes.length > 0){
			handler.write(bytes);
			if (this.immediateFlush || endOfBatch){
				handler.flush();
			}
		}
	}

	@Override
	public void close() {
		handler.close();
	}
}
