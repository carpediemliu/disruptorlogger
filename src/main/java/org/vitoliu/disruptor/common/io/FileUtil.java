package org.vitoliu.disruptor.common.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * {@link java.io.File}工具类
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class FileUtil {


	private static final char UNIX_SEPARATOR = '/';

	private static final char WINDOWS_SEPARATOR = '\\';

	public static void compressFile(File source, File dest, int bufferSize) throws Exception {
		checkFile(source, dest);

		InputStream input = null;
		OutputStream output = null;

		try {
			input = new BufferedInputStream(new FileInputStream(source));
			output = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(dest)));
			final byte[] buffer = new byte[bufferSize];
			int n;
			while ((n = input.read(buffer)) != -1) {
				output.write(buffer, 0, n);
			}
		}
		finally {
			IOUtil.closeQuietly(input);
			IOUtil.closeQuietly(output);
		}
	}

	public static void copyFile(File source, File dest) throws Exception {
		checkFile(source, dest);
		if (!dest.exists()) {
			dest.createNewFile();
		}
		FileInputStream input = null;
		FileOutputStream output = null;

		try {
			input = new FileInputStream(source);
			output = new FileOutputStream(dest);
			FileChannel outputChannel = output.getChannel();
			FileChannel inputChannel = input.getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		}
		finally {
			IOUtil.closeQuietly(input);
			IOUtil.closeQuietly(output);
		}

	}

	private static void checkFile(File source, File dest) {
		if (source == null || !source.exists()) {
			return;
		}
		if (dest == null) {
			return;
		}
	}


	public static boolean isExtension(String fileName, String[] extensions) {
		if (StringUtils.isEmpty(fileName)) {
			return false;
		}
		if (extensions == null || extensions.length == 0) {
			return (indexOfExtension(fileName) == -1);
		}
		String fileExtension = getExtension(fileName);
		for (String extension : extensions) {
			if (fileExtension.equals(extension)) {
				return true;
			}
		}
		return false;
	}

	private static String getExtension(String fileName) {
		if (StringUtils.isEmpty(fileName)) {
			return null;
		}
		int index = indexOfExtension(fileName);
		if (index == -1) {
			return "";
		}
		else {
			return fileName.substring(index + 1);
		}
	}

	private static int indexOfExtension(String fileName) {
		if (StringUtils.isEmpty(fileName)) {
			return -1;
		}
		int lastUnixPos = fileName.lastIndexOf(UNIX_SEPARATOR);
		int lastWindowsPos = fileName.lastIndexOf(WINDOWS_SEPARATOR);
		return Math.max(lastUnixPos, lastWindowsPos);
	}
}
