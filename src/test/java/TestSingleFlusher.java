import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import com.lmax.disruptor.RingBuffer;
import org.junit.Assert;
import org.junit.Test;
import org.vitoliu.disruptor.concurrent.async.SingleFlusher;

/**
 *
 * {@link SingleFlusher 测试}
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class TestSingleFlusher {


	@Test
	public void test1() throws Exception {
		final AtomicInteger value = new AtomicInteger();
		SingleFlusher.Builder<Integer> builder = new SingleFlusher.Builder<Integer>().setBufferSize(1024).setNotifySize(128).setNamePrefix("test");
		SingleFlusher.EventListener[] groupOne = new SingleFlusher.EventListener[1];
		groupOne[0] = new SingleFlusher.EventListener<Integer>() {
			@Override
			public void onException(Throwable e, long sequence, Integer event) {
				e.printStackTrace();
				System.out.println(event);
			}

			@Override
			public void onEvent(Integer event, boolean endOfBatch) throws Exception {
				System.out.format(Thread.currentThread() + "\t1: %s + \t" + value.incrementAndGet() + "\t endOfBatch: %s", event, endOfBatch);
			}
		};

		SingleFlusher.EventListener[] groupTwo = new SingleFlusher.EventListener[2];
		groupTwo[0] = new SingleFlusher.EventListener<Integer>() {
			@Override
			public void onException(Throwable e, long sequence, Integer event) {
				e.printStackTrace();
				System.out.println(event);
			}

			@Override
			public void onEvent(Integer event, boolean endOfBatch) throws Exception {
				System.out.format(Thread.currentThread() + "\t2: %s + \t" + value.incrementAndGet() + "\t endOfBatch: %s", event, endOfBatch);
			}
		};

		groupTwo[1] = new SingleFlusher.EventListener<Integer>() {
			@Override
			public void onException(Throwable e, long sequence, Integer event) {
				e.printStackTrace();
				System.out.println(event);
			}

			@Override
			public void onEvent(Integer event, boolean endOfBatch) throws Exception {
				System.out.format(Thread.currentThread() + "\t3: %s + \t" + value.incrementAndGet() + "\t endOfBatch: %s", event, endOfBatch);
			}
		};

		builder.addListenerGroup(groupOne).addListenerGroup(groupTwo);
		SingleFlusher<Integer> flusher = builder.build();
		final AtomicInteger test = new AtomicInteger();
		for (int i = 0; i < 1; i++) {
			flusher.add(test.incrementAndGet());
		}
		check(flusher, false);

	}

	private void check(SingleFlusher<Integer> flusher, boolean expected) throws Exception {
		Field ringBufferField = flusher.getClass().getDeclaredField("ringBuffer");
		ringBufferField.setAccessible(true);
		RingBuffer<Object> ringBuffer = (RingBuffer<Object>) ringBufferField.get(flusher);
		Field resetHolderField = flusher.getClass().getDeclaredField("resetHolder");
		resetHolderField.setAccessible(true);
		Assert.assertEquals(expected, resetHolderField.get(flusher));

		Thread.sleep(5000);
		System.out.println(ringBuffer.get(0));
	}
}
