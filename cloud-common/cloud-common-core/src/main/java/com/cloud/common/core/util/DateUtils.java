package com.cloud.common.core.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.lang.management.ManagementFactory;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 时间工具类
 *
 * @author shengjie.tang
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    public static String YYYY = "yyyy";

    public static String YYYY_MM = "yyyy-MM";

    public static String YYYY_MM_DD = "yyyy-MM-dd";

    public static String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private static String[] parsePatterns = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM",
            "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "EEE MMM dd HH:mm:ss z yyyy",
    };


    /**
     * 获取当前Date型日期
     *
     * @return Date() 当前日期
     */
    public static Date getNowDate() {
        return new Date();
    }

    /**
     * 获取当前日期, 默认格式为yyyy-MM-dd
     *
     * @return String
     */
    public static String getDate() {
        return dateTimeNow(YYYY_MM_DD);
    }

    /**
     * 获取当前年份, 默认格式为yyyy
     *
     * @return String
     */
    public static String getYear() {
        return dateTimeNow(YYYY);
    }

    public static final String getTime() {
        return dateTimeNow(YYYY_MM_DD_HH_MM_SS);
    }

    public static final String dateTimeNow() {
        return dateTimeNow(YYYYMMDDHHMMSS);
    }

    public static final String dateTimeNow(final String format) {
        return parseDateToStr(format, new Date());
    }

    public static final String dateTime(final Date date) {
        return parseDateToStr(YYYY_MM_DD, date);
    }

    public static final String parseDateToStr(final String format, final Date date) {
        return new SimpleDateFormat(format).format(date);
    }

    public static final Date dateTime(final String format, final String ts) {
        try {
            return new SimpleDateFormat(format).parse(ts);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Date getStartOfYear(LocalDateTime date) {
        // 获取年份
        int year = date.getYear();

        // 创建该年1月1日 00:00:00的 LocalDateTime
        LocalDateTime startOfYear = LocalDateTime.of(year, Month.JANUARY, 1, 0, 0, 0, 0);

        // 将 LocalDateTime 转换为 Date
        ZonedDateTime zonedDateTime = startOfYear.atZone(ZoneId.systemDefault());
        return Date.from(zonedDateTime.toInstant());
    }


    /**
     * 日期路径 即年/月/日 如2018/08/08
     */
    public static final String datePath() {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyy/MM/dd");
    }

    /**
     * 日期路径 即年/月/日 如20180808
     */
    public static final String dateTime() {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyyMMdd");
    }

    /**
     * 日期型字符串转化为日期 格式
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        try {
            return parseDate(str.toString(), parsePatterns);
        } catch (ParseException e) {
            return null;
        }
    }

    public static void main(String[] args) throws ParseException {
        String date = "Mon Jan 01 00:00:00 CST 2024";
        DateUtils.parseDate(date, Locale.ENGLISH, "EEE MMM dd HH:mm:ss z yyyy");
        System.out.println(parseDate("Mon Jan 01 00:00:00 CST 2024"));
    }

    /**
     * 获取服务器启动时间
     */
    public static Date getServerStartDate() {
        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new Date(time);
    }

    /**
     * 计算时间差
     *
     * @param endDate   最后时间
     * @param startTime 开始时间
     * @return 时间差（天/小时/分钟）
     */
    public static String timeDistance(Date endDate, Date startTime) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - startTime.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return day + "天" + hour + "小时" + min + "分钟";
    }

    /**
     * 增加 LocalDateTime ==> Date
     */
    public static Date toDate(LocalDateTime temporalAccessor) {
        ZonedDateTime zdt = temporalAccessor.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    /**
     * 增加 LocalDate ==> Date
     */
    public static Date toDate(LocalDate temporalAccessor) {
        LocalDateTime localDateTime = LocalDateTime.of(temporalAccessor, LocalTime.of(0, 0, 0));
        ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    public static Date getBeginDayOfMonth(Date d) {
        Calendar calendar = Calendar.getInstance();
        if (null != d) {
            calendar.setTime(d);
        }
        calendar.set(getYearValue(calendar.getTime()), getMonthValue(calendar.getTime()) - 1, 1);
        return getDayStartTime(calendar.getTime());
    }

    /**
     * 获取两个日期之间的相隔天数
     *
     * @param date1
     * @param date2
     * @return days date2 -date1的天数
     */
    public static int getDifferDays(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        clearTimeField(cal1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        clearTimeField(cal2);

        long between_days = (cal2.getTimeInMillis() - cal1.getTimeInMillis()) / (1000 * 3600 * 24);
        return (int) between_days;
    }

    /**
     * 获取调整日期
     *
     * @param originalDate 被调整的日期，null表示当前时间
     * @param type         {@link Calendar}类定义的各种常量，年、月、周、日、时、分、秒、毫秒
     * @param value        调整值 正数表示日期之后，负数表示之前
     * @return
     */
    public static Date adjust(Date originalDate, int type, int value) {
        Calendar cal = Calendar.getInstance();

        if (originalDate != null) {
            cal.setTime(originalDate);
        }

        if (value != 0) {
            cal.add(type, value);
        }

        return cal.getTime();
    }

    public static String getNextMonth(String inputMonth) {
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        // 将输入字符串解析为 LocalDate
        LocalDate date = LocalDate.parse(inputMonth + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // 加一个月
        LocalDate nextMonth = date.plusMonths(1);
        // 返回格式化后的字符串
        return nextMonth.format(formatter);
    }

    private static void clearTimeField(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    // 获取本月的结束时间
    public static Date getEndDayOfMonth(Date d) {
        Calendar calendar = Calendar.getInstance();
        if (null != d) {
            calendar.setTime(d);
        }
        calendar.set(getYearValue(calendar.getTime()), getMonthValue(calendar.getTime()) - 1, 1);
        int day = calendar.getActualMaximum(5);
        calendar.set(getYearValue(calendar.getTime()), getMonthValue(calendar.getTime()) - 1, day);
        return getDayEndTime(calendar.getTime());
    }

    // 获取某个日期的结束时间
    public static Timestamp getDayEndTime(Date d) {
        Calendar calendar = Calendar.getInstance();
        if (null != d) {
            calendar.setTime(d);
        }
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 23,
                59, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return new Timestamp(calendar.getTimeInMillis());
    }

    /**
     * 获取当前日期的年份
     */
    public static int getYearValue(Date d) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d);
        return calendar.get(Calendar.YEAR);
    }

    /**
     * 获取当前日期的月份
     */
    public static int getMonthValue(Date d) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d);
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取当前日期的日份
     */
    public static int getDayValue(Date d) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    // 获取某个日期的开始时间
    public static Timestamp getDayStartTime(Date d) {
        Calendar calendar = Calendar.getInstance();
        if (null != d) {
            calendar.setTime(d);
        }
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0,
                0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTimeInMillis());
    }

}
