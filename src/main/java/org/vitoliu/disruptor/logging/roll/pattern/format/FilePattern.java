package org.vitoliu.disruptor.logging.roll.pattern.format;

import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.vitoliu.disruptor.common.io.FileUtil;

/**
 *
 * 轮转文件格式，参考Self4J
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class FilePattern {

	private static final String[] GUN_ZIP = new String[] {"GZ", "gz"};

	private static final String SPLIT = ".";

	private static final String REXEX_SPLIT = "\\" + SPLIT;

	private final Formatter[] formatters;

	private final DateFormatter.Frequency frequency;

	private final boolean isGunZip;

	private final Clock clock;

	private long PreFileTime = 0;

	private long nextFileTime = 0;

	public FilePattern(String filePattern, Clock clock) {
		Preconditions.checkNotNull(filePattern, "patameter 'filePattern' must not be null or empty");
		DateFormatter.Frequency frequency = null;
		String[] filePatterns = filePattern.split(REXEX_SPLIT);

		List<Formatter> formatters = Lists.newArrayListWithCapacity(filePatterns.length);
		for (String pattern : filePatterns) {
			Formatter formatter = FormatterFactory.valueOf(pattern);
			if (formatter instanceof DateFormatter) {
				frequency = ((DateFormatter) formatter).getFrequency();
			}
			formatters.add(formatter);
		}
		this.formatters = formatters.toArray(new Formatter[formatters.size()]);
		this.frequency = frequency;
		this.isGunZip = FileUtil.isExtension(filePattern, GUN_ZIP);
		this.clock = clock;
	}


	public long getNextTime(long mills, int increment, boolean modulus) {
		PreFileTime = nextFileTime;
		Map.Entry<Long, Long> nextTimePair = frequency.getNextTime(mills, increment, modulus);
		nextFileTime = nextTimePair.getValue();
		return nextTimePair.getKey();
	}

	public void format(StringBuilder source, int index) {
		Date fileTime = PreFileTime == 0 ? new Date(clock.millis()) : new Date(PreFileTime);
		for (int i = 0; i < formatters.length; i++) {
			Formatter formatter = formatters[i];
			if (i != 0) {
				source.append(SPLIT);
			}
			formatter.format(source, fileTime, index);
		}
	}

	private static class FormatterFactory {
		private static Pattern datePattern = Pattern.compile("%d\\{(.*\\})");

		private static Pattern indexPattern = Pattern.compile("%index");

		private static Formatter valueOf(String pattern) {
			Matcher dateMatcher = datePattern.matcher(pattern);
			if (dateMatcher.find()) {
				return new DateFormatter(dateMatcher.group(1));
			}
			if (indexPattern.matcher(pattern).matches()) {
				return new IndexFormatter();
			}
			return new DefaultFormatter(pattern);
		}
	}

	public boolean isGunZip(){
		return isGunZip;
	}
}
