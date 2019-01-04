package org.vitoliu.disruptor.logging.roll.action;

import java.io.File;
import java.io.IOException;

import org.vitoliu.disruptor.common.io.FileUtil;
import org.vitoliu.disruptor.logging.exception.ExceptionHandler;

/**
 *
 * 轮转时的重命名行为
 * @author vito.liu
 * @since 12 十二月 2018
 */
public class FileRenameAction extends AbstractAction {


	private final File source;

	private final File destination;


	public FileRenameAction(File source, File destination, ExceptionHandler exceptionHandler) {
		super(exceptionHandler);
		this.source = source;
		this.destination = destination;
	}

	@Override
	public boolean execute() throws Exception {
		if (source.exists() && source.length() > 0) {
			final File parentFile = destination.getParentFile();
			if (parentFile != null && parentFile.exists()) {
				parentFile.mkdirs();
			}
			if (!parentFile.exists()) {
				exceptionHandler.handle("unable to create directory " + parentFile.getPath());
			}
			try {
				if (!source.renameTo(destination)) {
					try {
						FileUtil.copyFile(source, destination);
						return source.delete();
					}
					catch (IOException e) {
						exceptionHandler.handleException("unable to rename file " + source.getPath() + " to " + destination.getPath(), e);
					}
				}
				return true;
			}
			catch (Exception ex) {
				try {
					FileUtil.copyFile(source, destination);
					return source.delete();
				}
				catch (IOException e) {
					exceptionHandler.handleException("Unable to rename file " + source.getPath() + " to " + destination.getPath(), e);
				}
			}
		}
		return false;
	}
}
