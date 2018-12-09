import java.util.concurrent.atomic.AtomicInteger;

import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.FatalError;
import org.junit.Test;
import org.vitoliu.disruptor.concurrent.async.Flusher;

/**
 *
 * {@link org.vitoliu.disruptor.concurrent.async.Flusher 测试}
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class TestFlusher {


	@Test
	public void test1() throws Exception {
		final AtomicInteger value = new AtomicInteger();
		Flusher.Builder<Integer> builder = new Flusher.Builder<Integer>().setBufferSize(1024).setNotifySize(128).setNamePrefix("test");
		Flusher.EventListener[] groupOne = new Flusher.EventListener[1];
		groupOne[0] = new Flusher.EventListener<Integer>() {
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

		Flusher.EventListener[] groupTwo = new Flusher.EventListener[2];
		groupTwo[0] = new Flusher.EventListener<Integer>() {
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

		groupTwo[1] = new Flusher.EventListener<Integer>() {
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
		Flusher<Integer> flusher = builder.build();
		final AtomicInteger test = new AtomicInteger();
		for (int i = 0; i< 1;i++){
			flusher.add(test.incrementAndGet());
		}
		check(flusher, false);


	}

	private void check(Flusher<Integer> flusher, boolean expected) throws Exception {

	}
}
