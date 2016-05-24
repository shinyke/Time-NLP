/**
 * Copyright (c) 2016 21CN.COM . All rights reserved.<br>
 *
 * Description: calendarCommon<br>
 * 
 * Modified log:<br>
 * ------------------------------------------------------<br>
 * Ver.		Date		Author			Description<br>
 * ------------------------------------------------------<br>
 * 1.0		2016年4月25日		kexm		created.<br>
 */
package com.time.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * <p>
 * 日期工具类（来自公司公共项目）
 * <p>
 * @author <a href="mailto:kexm@corp.21cn.com">kexm</a>
 * @version 
 * @since 2016年4月25日
 * 
 */
public class CommonDateUtil {
	private static String defaultDatePattern = "yyyy-MM-dd";
	public static final long ONE_MINUTE_MILLISECOND = 60000L;
	public static final long ONE_HOUR_MILLISECOND = 3600000L;
	public static final long ONE_DAY_MILLISECOND = 86400000L;
	public static final long ONE_WEEK_MILLISECOND = 604800000L;
	public static final long ONE_MONTH_MILLISECOND = 2592000000L;
	public static final long ONE_YEAR_MILLISECOND = 31536000000L;
	private static final String[] SMART_DATE_FORMATS = { "yyyy-MM-dd HH:mm:ss", "yyyy.MM.dd HH:mm:ss",
			"yyyy-MM-dd HH:mm", "yyyy.MM.dd HH:mm", "yyyyMMddHHmmss", "yyyyMMddHHmm", "yyyy-MM-dd", "yyyy.MM.dd",
			"yyyyMMdd" };

	public static final String[] zodiacArray = { "猴", "鸡", "狗", "猪", "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊" };

	public static final String[] constellationArray = { "水瓶座", "双鱼座", "牡羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座",
			"天蝎座", "射手座", "魔羯座" };

	private static final int[] constellationEdgeDay = { 20, 19, 21, 21, 21, 22, 23, 23, 23, 23, 22, 22 };

	public static String getDatePattern() {
		return defaultDatePattern;
	}
	
	public static int getYear(Date date) {
		return getCalendar(date).get(1);
	}

	public static int getMonth(Date date) {
		return getCalendar(date).get(2);
	}

	public static int getDay(Date date) {
		return getCalendar(date).get(5);
	}

	public static int getWeek(Date date) {
		return getCalendar(date).get(7);
	}

	public static int getWeekOfFirstDayOfMonth(Date date) {
		return getWeek(getFirstDayOfMonth(date));
	}

	public static int getWeekOfLastDayOfMonth(Date date) {
		return getWeek(getLastDayOfMonth(date));
	}

	public static final Date parseDate(String strDate, String format) {
		SimpleDateFormat df = new SimpleDateFormat(format);
		try {
			return df.parse(strDate);
		} catch (ParseException pe) {
		}
		return null;
	}

	public static final Date parseDateSmart(String strDate) {
		if (StringUtil.isEmpty(strDate))
			return null;
		for (String fmt : SMART_DATE_FORMATS) {
			Date d = parseDate(strDate, fmt);
			if (d != null) {
				String s = formatDate(d, fmt);
				if (strDate.equals(s))
					return d;
			}
		}
		try {
			long time = Long.parseLong(strDate);
			return new Date(time);
		} catch (Exception e) {
		}
		return null;
	}

	public static Date parseDate(String strDate) {
		return parseDate(strDate, getDatePattern());
	}

	public static boolean isLeapYear(int year) {
		if (year / 4 * 4 != year) {
			return false;
		}
		if (year / 100 * 100 != year) {
			return true;
		}

		return (year / 400 * 400 == year);
	}

	public static boolean isWeekend(Date date) {
		Calendar c = Calendar.getInstance();
		if (date != null)
			c.setTime(date);
		int weekDay = c.get(7);
		return ((weekDay == 1) || (weekDay == 7));
	}

	public static boolean isWeekend() {
		return isWeekend(null);
	}

	public static String getCurrentTime() {
		return formatDate(new Date());
	}

	public static String getCurrentTime(String format) {
		return formatDate(new Date(), format);
	}

	public static String formatDate(Date date, String format) {
		if (date == null)
			date = new Date();
		if (format == null)
			format = getDatePattern();
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(date);
	}

	public static String formatDate(Date date) {
		long offset = System.currentTimeMillis() - date.getTime();
		String pos = "前";
		if (offset < 0L) {
			pos = "后";
			offset = -offset;
		}
		if (offset >= 31536000000L) {
			return formatDate(date, getDatePattern());
		}
		if (offset >= 5184000000L) {
			return ((offset + 1296000000L) / 2592000000L) + "个月" + pos;
		}
		if (offset > 604800000L) {
			return ((offset + 302400000L) / 604800000L) + "周" + pos;
		}
		if (offset > 86400000L) {
			return ((offset + 43200000L) / 86400000L) + "天" + pos;
		}
		if (offset > 3600000L) {
			return ((offset + 1800000L) / 3600000L) + "小时" + pos;
		}
		if (offset > 60000L) {
			return ((offset + 30000L) / 60000L) + "分钟" + pos;
		}
		return (offset / 1000L) + "秒" + pos;
	}

	public static Date getCleanDay(Date day) {
		return getCleanDay(getCalendar(day));
	}

	public static Calendar getCalendar(Date day) {
		Calendar c = Calendar.getInstance();
		if (day != null)
			c.setTime(day);
		return c;
	}

	private static Date getCleanDay(Calendar c) {
		c.set(11, 0);
		c.clear(12);
		c.clear(13);
		c.clear(14);
		return c.getTime();
	}

	public static Date makeDate(int year, int month, int day) {
		Calendar c = Calendar.getInstance();
		getCleanDay(c);
		c.set(1, year);
		c.set(2, month - 1);
		c.set(5, day);
		return c.getTime();
	}

	private static Date getFirstCleanDay(int datePart, Date date) {
		Calendar c = Calendar.getInstance();
		if (date != null)
			c.setTime(date);
		c.set(datePart, 1);
		return getCleanDay(c);
	}

	private static Date add(int datePart, int detal, Date date) {
		Calendar c = Calendar.getInstance();
		if (date != null)
			c.setTime(date);
		c.add(datePart, detal);
		return c.getTime();
	}

	public static Date getFirstDayOfWeek(Date date) {
		return getFirstCleanDay(7, date);
	}

	public static Date getFirstDayOfWeek() {
		return getFirstDayOfWeek(null);
	}

	public static Date getFirstDayOfMonth(Date date) {
		return getFirstCleanDay(5, date);
	}

	public static Date getFirstDayOfMonth() {
		return getFirstDayOfMonth(null);
	}

	public static Date getLastDayOfMonth() {
		return getLastDayOfMonth(null);
	}

	public static Date getLastDayOfMonth(Date date) {
		Calendar c = getCalendar(getFirstDayOfMonth(date));
		c.add(2, 1);
		c.add(5, -1);
		return getCleanDay(c);
	}

	public static Date getFirstDayOfSeason(Date date) {
		Date d = getFirstDayOfMonth(date);
		int delta = getMonth(d) % 3;
		if (delta > 0)
			d = getDateAfterMonths(d, -delta);
		return d;
	}

	public static Date getFirstDayOfSeason() {
		return getFirstDayOfMonth(null);
	}

	public static Date getFirstDayOfYear(Date date) {
		return makeDate(getYear(date), 1, 1);
	}

	public static Date getFirstDayOfYear() {
		return getFirstDayOfYear(new Date());
	}

	public static Date getDateAfterWeeks(Date start, int weeks) {
		return getDateAfterMs(start, weeks * 604800000L);
	}

	public static Date getDateAfterMonths(Date start, int months) {
		return add(2, months, start);
	}

	public static Date getDateAfterYears(Date start, int years) {
		return add(1, years, start);
	}

	public static Date getDateAfterDays(Date start, int days) {
		return getDateAfterMs(start, days * 86400000L);
	}

	public static Date getDateAfterMs(Date start, long ms) {
		return new Date(start.getTime() + ms);
	}

	public static long getPeriodNum(Date start, Date end, long msPeriod) {
		return (getIntervalMs(start, end) / msPeriod);
	}

	public static long getIntervalMs(Date start, Date end) {
		return (end.getTime() - start.getTime());
	}

	public static int getIntervalDays(Date start, Date end) {
		return (int) getPeriodNum(start, end, 86400000L);
	}

	public static int getIntervalWeeks(Date start, Date end) {
		return (int) getPeriodNum(start, end, 604800000L);
	}

	public static boolean before(Date base, Date date) {
		return ((date.before(base)) || (date.equals(base)));
	}

	public static boolean after(Date base, Date date) {
		return ((date.after(base)) || (date.equals(base)));
	}

	public static Date max(Date date1, Date date2) {
		if (date1.getTime() > date2.getTime()) {
			return date1;
		}
		return date2;
	}

	public static Date min(Date date1, Date date2) {
		if (date1.getTime() < date2.getTime()) {
			return date1;
		}
		return date2;
	}

	public static boolean inPeriod(Date start, Date end, Date date) {
		return ((((end.after(date)) || (end.equals(date)))) && (((start.before(date)) || (start.equals(date)))));
	}

	public static String date2Zodica(Date time) {
		Calendar c = Calendar.getInstance();
		c.setTime(time);
		return year2Zodica(c.get(1));
	}

	public static String year2Zodica(int year) {
		return zodiacArray[(year % 12)];
	}

	public static String date2Constellation(Date time) {
		Calendar c = Calendar.getInstance();
		c.setTime(time);
		int month = c.get(2);
		int day = c.get(5);
		if (day < constellationEdgeDay[month]) {
			--month;
		}
		if (month >= 0) {
			return constellationArray[month];
		}

		return constellationArray[11];
	}

	public static void main(String[] args) {
		System.out.println(year2Zodica(1973));
		System.out.println(date2Zodica(new Date()));
		System.out.println(date2Constellation(makeDate(1973, 5, 12)));
		System.out.println(Calendar.getInstance() == Calendar.getInstance());
		System.out.println(getCleanDay(new Date()));
		System.out.println(new Date());
		Calendar c = Calendar.getInstance();
		c.set(5, 1);
		System.out.println(getFirstDayOfMonth());
		System.out.println(getLastDayOfMonth(makeDate(1996, 2, 1)));

		System.out.println(formatDate(makeDate(2009, 5, 1)));
		System.out.println(formatDate(makeDate(2010, 5, 1)));
		System.out.println(formatDate(makeDate(2010, 12, 21)));
		System.out.println(before(makeDate(2009, 5, 1), new Date()));
		System.out.println(after(makeDate(2009, 5, 1), new Date()));
		System.out.println(inPeriod(makeDate(2009, 11, 24), makeDate(2009, 11, 30), makeDate(2009, 11, 25)));
	}
}
