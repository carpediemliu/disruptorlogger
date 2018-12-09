package org.vitoliu.disruptor.common.io;

import java.io.Closeable;
import java.io.IOException;

/**
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class IOUtil {

	public static void closeQuietly(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		}
		catch (IOException e) {
			//ignore
		}
	}
}
