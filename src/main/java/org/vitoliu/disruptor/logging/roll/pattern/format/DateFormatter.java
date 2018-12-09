package org.vitoliu.disruptor.logging.roll.pattern.format;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 格式化器
 * @author vito.liu
 * @since 09 十二月 2018
 */
public class DateFormatter implements Formatter {

	private final String pattern;

	private final Frequency frequency;

	public DateFormatter(String pattern) {
		this.pattern = pattern;
		this.frequency = Frequency.of(pattern);
	}


	@Override
	public void format(StringBuilder source, Object... arguments) {
		for (Object argument : arguments) {
			if (argument instanceof Date) {
				source.append(DateFormatter.format(this.pattern, (Date) argument));
			}
		}
	}

	private static String format(String pattern, Date date) {
		return new SimpleDateFormat(pattern).format(date);
	}

	public Frequency getFrequency() {
		return frequency;
	}

	public enum Frequency {

		/**
		 * 分钟级别的formatter
		 */
		MINUTE("m") {
			@Override
			protected Map.Entry<Long, Long> doGetNextTime(Calendar calendar, Calendar current, int increment, boolean modulus) {
				setCalendar(calendar, current);
				calendar.set(Calendar.MINUTE, current.get(Calendar.MINUTE));

				increment(calendar, Calendar.MINUTE, increment, modulus);
				return getTimeEntry(calendar, Calendar.MINUTE);
			}

		},

		/**
		 * 小时级别
		 */
		HOUR("H") {
			@Override
			protected Map.Entry<Long, Long> doGetNextTime(Calendar calendar, Calendar current, int increment, boolean modulus) {
				setCalendar(calendar, current);

				increment(calendar, Calendar.HOUR_OF_DAY, increment, modulus);
				return getTimeEntry(calendar, Calendar.HOUR_OF_DAY);
			}
		},


		/**
		 * 日级别
		 */
		DAY("d") {
			@Override
			protected Map.Entry<Long, Long> doGetNextTime(Calendar calendar, Calendar current, int increment, boolean modulus) {
				calendar.set(Calendar.MONTH, current.get(Calendar.MONTH));
				calendar.set(Calendar.DAY_OF_YEAR, current.get(Calendar.DAY_OF_YEAR));

				increment(calendar, Calendar.DAY_OF_YEAR, increment, modulus);
				return getTimeEntry(calendar, Calendar.DAY_OF_YEAR);
			}
		},
		/**
		 * 周级别
		 */
		WEEK("w") {
			@Override
			protected Map.Entry<Long, Long> doGetNextTime(Calendar calendar, Calendar current, int increment, boolean modulus) {
				calendar.set(Calendar.MONTH, current.get(Calendar.MONTH));
				calendar.set(Calendar.WEEK_OF_YEAR, current.get(Calendar.WEEK_OF_YEAR));
				increment(calendar, Calendar.DAY_OF_YEAR, increment, modulus);
				calendar.set(Calendar.DAY_OF_WEEK, current.getFirstDayOfWeek());
				return getTimeEntry(calendar, Calendar.WEEK_OF_YEAR);
			}
		},
		/**
		 * 月级别
		 */
		MONTH("M") {
			@Override
			protected Map.Entry<Long, Long> doGetNextTime(Calendar calendar, Calendar current, int increment, boolean modulus) {
				calendar.set(Calendar.MONTH, current.get(Calendar.MONTH));
				increment(calendar, Calendar.MONTH, increment, modulus);
				return getTimeEntry(calendar, Calendar.MONTH);
			}
		},

		/**
		 * 年级别
		 */
		ANNUAL("y") {
			@Override
			protected Map.Entry<Long, Long> doGetNextTime(Calendar calendar, Calendar current, int increment, boolean modulus) {
				increment(calendar, Calendar.YEAR, increment, modulus);
				return getTimeEntry(calendar, Calendar.MONTH);
			}
		};

		private String match;

		protected abstract Map.Entry<Long, Long> doGetNextTime(Calendar calendar, Calendar current, int increment, boolean modulus);

		Frequency(String match) {
			this.match = match;
		}

		private static void setCalendar(Calendar calendar, Calendar current) {
			calendar.set(Calendar.MONTH, current.get(Calendar.MONTH));
			calendar.set(Calendar.DAY_OF_YEAR, current.get(Calendar.DAY_OF_YEAR));
			calendar.set(Calendar.HOUR_OF_DAY, current.get(Calendar.HOUR_OF_DAY));
		}

		private static Map.Entry<Long, Long> getTimeEntry(Calendar calendar, int timeField) {
			long nextTime = calendar.getTimeInMillis();
			calendar.add(timeField, -1);
			long nextFileTime = calendar.getTimeInMillis();
			return Maps.immutableEntry(nextTime, nextFileTime);
		}

		private static void increment(Calendar cal, int type, int increment, boolean modulate) {
			int interval = modulate ? (increment - (cal.get(type)) % increment) : increment;
			cal.add(type, interval);
		}

		public static Frequency of(String pattern) throws IllegalArgumentException {
			for (Frequency frequency : Frequency.values()) {
				if (frequency.match(pattern)) {
					return frequency;
				}
			}
			throw new IllegalArgumentException("pattern not supported!");
		}

		private boolean match(String pattern) {
			return pattern.contains(match);
		}

		public Map.Entry<Long, Long> getNextTime(long currentMills, int increment, boolean modulus) {
			Calendar current = Calendar.getInstance();
			current.setTimeInMillis(currentMills);
			Calendar calendar = Calendar.getInstance();
			calendar.set(current.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			return doGetNextTime(calendar, current, increment, modulus);
		}

	}
}
