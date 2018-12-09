package org.vitoliu.common.batch;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

/**
 *
 * 批量转发器（同步聚合单个元素到批量）
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class BatchForwarder<T> {
	final int batchSize;

	final long duration;

	final Clock clock;

	final Processor<T> processor;

	final List<T> events;

	volatile long batchEndTime;

	public BatchForwarder(int batchSize, long duration, TimeUnit timeUnit, Clock clock, Processor processor) {
		this.events = Lists.newArrayListWithCapacity(batchSize);
		this.batchSize = batchSize;
		this.duration = duration;
		this.clock = clock;
		this.processor = processor;
		this.batchEndTime = clock.millis() + this.duration;
	}

	public void add(T event) {
		add(event, false);
	}

	private void add(T event, boolean force) {
		doAdd(event);
		if (force || forward()) {
			processor.process(events);
			reset();
		}
	}

	private void reset() {
		this.events.clear();
		this.batchEndTime = this.clock.millis() + this.duration;
	}

	private boolean forward() {
		return this.events.size() > this.batchSize || this.clock.millis() > this.batchEndTime;
	}

	private void doAdd(T event) {
		this.events.add(event);
	}

	private interface Processor<T> {
		void process(List<T> events);
	}
}
