package org.vitoliu.disruptor.logging.roll.action;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;
import org.vitoliu.disruptor.common.io.FileUtil;
import org.vitoliu.disruptor.logging.exception.ExceptionHandler;
import org.vitoliu.disruptor.logging.roll.pattern.format.Action;
import org.vitoliu.disruptor.logging.roll.pattern.format.FilePattern;
import org.vitoliu.disruptor.logging.roll.pattern.format.Rollover;

/**
 *
 * 默认轮转器
 * @author vito.liu
 * @since 12 十二月 2018
 */
public class DefaultRollover implements Rollover {

	private final int maxIndex;

	private final int minIndex;

	private final boolean useMax;

	private final int bufferSize;

	private final ExceptionHandler exceptionHandler;

	public DefaultRollover(int maxIndex, int minIndex, boolean useMax, int bufferSize, ExceptionHandler exceptionHandler) {
		this.maxIndex = maxIndex;
		this.minIndex = minIndex;
		this.useMax = useMax;
		this.bufferSize = bufferSize;
		this.exceptionHandler = exceptionHandler;
	}

	private int purge(int lowIndex, int highIndex, FilePattern filePattern) {
		//TODO
		return 0;
	}

	private int purgeAscending(int lowIndex, int highIndex, FilePattern filePattern) {
		List<FileRenameAction> renameActionList = Lists.newArrayList();
		StringBuilder buf = new StringBuilder();
		filePattern.format(buf, highIndex);
		String highFileName = buf.toString();

		int maxIndex = 0;

		for (int i = highIndex; i >= lowIndex; i--) {
			File toRename = new File(highFileName);
			if (i == highIndex && toRename.exists()) {
				maxIndex = highIndex;
			}
			else if (maxIndex == 0 && toRename.exists()) {
				maxIndex = i + 1;
				break;
			}

			boolean isBase = false;

			if (filePattern.isGunZip()) {
				File toRenameBase = new File(FileUtil.removeExtension(highFileName));
				if (toRename.exists()) {
					if (toRenameBase.exists()) {
						toRenameBase.delete();
					}
				}
				else {
					toRename = toRenameBase;
					isBase = true;
				}
			}

			if (toRename.exists()) {
				if (i == lowIndex) {
					if (!toRename.delete()) {
						return -1;
					}
					break;
				}
				buf.setLength(0);
				filePattern.format(buf, -1);
				String lowFileName = buf.toString();
				String renameTo = lowFileName;

				if (isBase) {
					if (filePattern.isGunZip()) {
						renameTo = FileUtil.removeExtension(lowFileName);
					}
					else {
						renameTo = lowFileName;
					}
				}

				renameActionList.add(new FileRenameAction(toRename, new File(renameTo), exceptionHandler));
				highFileName = lowFileName;
			}
			else {
				buf.setLength(0);
				filePattern.format(buf, i - 1);
				highFileName = buf.toString();
			}
		}
		if (maxIndex == 0) {
			maxIndex = lowIndex;
		}

		for (int i = renameActionList.size() - 1; i >= 0; i--) {
			Action action = renameActionList.get(i);
			try {
				if (!action.execute()){
					return -1;
				}
			}catch (Exception e){
				exceptionHandler.handleException("Exception during purge in rolling ", e);
				return -1;
			}
		}
		return maxIndex;
	}

	@Override
	public Description rollover(String fileName, FilePattern pattern) {
		if (maxIndex < 0){
			return null;
		}
		int fileIndex = purge(minIndex, maxIndex, pattern);
		if (fileIndex < 0){
			return null;
		}
		StringBuilder buf = new StringBuilder(255);
		pattern.format(buf, fileIndex);

		String renameTo = buf.toString();
		String compressedName = renameTo;
		Action compressAction = null;

		if (pattern.isGunZip()){
			renameTo = FileUtil.removeExtension(renameTo);
		}
		return null;
	}
}
