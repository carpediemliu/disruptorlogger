package org.vitoliu.disruptor.logging.handler.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Clock;

import org.vitoliu.disruptor.common.io.IOUtil;
import org.vitoliu.disruptor.logging.exception.ExceptionHandler;
import org.vitoliu.disruptor.logging.exception.LoggerException;
import org.vitoliu.disruptor.logging.handler.FileHandler;

/**
 *
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class DefaultFileHandler implements FileHandler {

	protected final String fileName;

	protected final boolean isAppend;

	protected final ByteBuffer buffer;

	protected final Clock clock;

	protected final ExceptionHandler exceptionHandler;

	protected FileChannel fileChannel;

	protected long size;

	protected long initialTime;

	public DefaultFileHandler(String fileName, boolean isAppend, int bufferSize, boolean useDireceMemory, Clock clock, ExceptionHandler exceptionHandler) {
		File file = new File(fileName);
		File parentFile = file.getParentFile();
		if (null != parentFile && !parentFile.exists()) {
			boolean mkdirs = parentFile.mkdirs();
			if (!mkdirs){
				throw  new RuntimeException();
			}
		}

		if (!isAppend) {
			if (!file.delete()){
				throw  new RuntimeException();
			}
		}

		long initialSize = isAppend ? file.length() : 0;
		long initialTime = file.exists() ? file.lastModified() : clock.millis();
		FileChannel fileChannel = null;
		try {
			fileChannel = new FileOutputStream(file, isAppend).getChannel();
			fileChannel.position(initialSize);
		}
		catch (IOException e) {
			IOUtil.closeQuietly(fileChannel);
			throw new LoggerException(e);
		}
		this.fileName = fileName;
		this.isAppend = isAppend;
		this.fileChannel = fileChannel;
		if (useDireceMemory) {
			//是否启用NIO的堆外内存
			this.buffer = ByteBuffer.allocateDirect(bufferSize);
		}
		else {
			this.buffer = ByteBuffer.allocate(bufferSize);
		}
		this.size = initialSize;
		this.initialTime = initialTime;
		this.clock = clock;
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public void close() {
		flush();
		try {
			fileChannel.close();
		}
		catch (IOException e) {
			exceptionHandler.handleException("Unable to close RandomAccessFile!", e);
		}
	}

	@Override
	public final void write(byte[] data) {
		writes(data, 0, data.length);
	}

	private synchronized void writes(byte[] data, int offset, int length) {
		size += length;
		int chunk;
		while (length > 0) {
			if (length > buffer.remaining()) {
				flush();
			}
			chunk = Math.min(length, buffer.remaining());
			buffer.put(data, offset, chunk);
			offset += chunk;
			length -= chunk;
		}
	}


	@Override
	public synchronized void flush() {
		buffer.flip();
		try {
			fileChannel.write(buffer);
		}
		catch (IOException e) {
			exceptionHandler.handleException("Error in flush buffer to randomAccessFile", e);
		}
		buffer.clear();
	}

	@Override
	public String getName() {
		return this.fileName;
	}

	@Override
	public long length() {
		return this.size;
	}

	@Override
	public long initialTime() {
		return this.initialTime;
	}
}
