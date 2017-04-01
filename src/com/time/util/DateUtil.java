/**
 * Copyright (c) 2016 21CN.COM . All rights reserved.<br>
 *
 * Description: calendar<br>
 *
 * Modified log:<br>
 * ------------------------------------------------------<br>
 * Ver.		Date		Author			Description<br>
 * ------------------------------------------------------<br>
 * 1.0		2016年3月8日		kexm		created.<br>
 */
package com.time.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 日程项目的时间工具类，继承公司的公共时间工具类
 * <p>
 *
 * @author <a href="mailto:kexm@corp.21cn.com">kexm</a>
 * @since 2016年3月8日
 */
public class DateUtil extends CommonDateUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtil.class);

    /**
     * 是否是今天
     *
     * @param date
     * @return
     */
    public static boolean isToday(final Date date) {
        return isTheDay(date, new Date());
    }

    /**
     * 是否是指定日期
     *
     * @param date
     * @param day
     * @return
     */
    public static boolean isTheDay(final Date date, final Date day) {
        return date.getTime() >= dayBegin(day).getTime() && date.getTime() <= dayEnd(day).getTime();
    }

    /**
     * 对时间中的分钟向上取整
     *
     * @param date
     * @param round 取整的值
     * @return
     */
    public static Date roundMin(final Date date, int round) {
        if (round > 60 || round < 0) {
            round = 0;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int min = c.get(Calendar.MINUTE);
        if ((min % round) >= (round / 2)) {
            min = round * (min / (round + 1));
        } else {
            min = round * (min / round);
        }
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        return c.getTime();
    }

    /**
     * 获得指定时间那天的某个小时（24小时制）的整点时间
     *
     * @param date
     * @param hourIn24
     * @return
     */
    public static Date getSpecificHourInTheDay(final Date date, int hourIn24) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, hourIn24);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /**
     * 得到本周周一
     *
     * @return Date
     */
    public static Date getFirstDayOfWeek(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int day_of_week = c.get(Calendar.DAY_OF_WEEK) - 1;
        if (day_of_week == 0)
            day_of_week = 7;
        c.add(Calendar.DATE, -day_of_week + 1);
        return c.getTime();
    }

    /**
     * 处理相对日期的相对运算（当前只支持周、月）
     *
     * @param date
     * @param hourIn24
     * @return
     */
    public static Date getRelativeTime(final Date date, final int calUnit, final int relative) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(calUnit, relative);
        return c.getTime();
    }

    /**
     * 获取指定时间的那天 00:00:00.000 的时间
     *
     * @param date
     * @return
     */
    public static Date dayBegin(final Date date) {
        return getSpecificHourInTheDay(date, 0);
    }

    /**
     * 获取指定时间的那天 23:59:59.999 的时间
     *
     * @param date
     * @return
     */
    public static Date dayEnd(final Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    /**
     * 默认时间格式化
     *
     * @param date
     * @param format
     * @return
     */
    public static String formatDateDefault(Date date) {
        return DateUtil.formatDate(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 检测日期格式字符串是否符合format
     * <p>
     * 主要逻辑为先把字符串parse为该format的Date对象，再将Date对象按format转换为string。如果此string与初始字符串一致，则日期符合format。
     * <p>
     * 之所以用来回双重逻辑校验，是因为假如把一个非法字符串parse为某format的Date对象是不一定会报错的。 比如 2015-06-29 13:12:121，明显不符合yyyy-MM-dd
     * HH:mm:ss，但是可以正常parse成Date对象，但时间变为了2015-06-29 13:14:01。增加多一重校验则可检测出这个问题。
     *
     * @param strDateTime
     * @param format      日期格式
     * @return boolean
     */
    public static boolean checkDateFormatAndValite(String strDateTime, String format) {
        if (strDateTime == null || strDateTime.length() == 0) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date ndate = sdf.parse(strDateTime);
            String str = sdf.format(ndate);
            LOGGER.debug("func<checkDateFormatAndValite> strDateTime<" + strDateTime + "> format<" + format +
                    "> str<" + str + ">");
            if (str.equals(strDateTime)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //日期格式为:年 月 日 ；如：2016年04月06日
    public static final String FORMAT_CALENDAR_DATE = "yyyy\u5E74MM\u6708dd\u65E5E";
    //时间格式 为：小时：分 ;如：12:30
    public static final String FORMAT_CALENDAR_TIME = "HH:mm";


    private final static List<Integer> TIMEUNITS = new ArrayList<Integer>();

    static {
        TIMEUNITS.add(Calendar.YEAR);
        TIMEUNITS.add(Calendar.MONTH);
        TIMEUNITS.add(Calendar.DATE);
        TIMEUNITS.add(Calendar.HOUR);
        TIMEUNITS.add(Calendar.MINUTE);
        TIMEUNITS.add(Calendar.SECOND);
    }
}
