package bsx.runtime.date;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

public class DateFormatHelper {
    
    public static final DateTimeFormatter DEFAULT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR)
            .appendLiteral("-")
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral("-")
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral("T")
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(":")
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(":")
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendOffset("+HHMM", "Z")
            .toFormatter(Locale.ROOT);
    
    private static final Map<Long, String> AM_PM_LOWER = Map.of(
            0L, "am",
            1L, "pm"
    );
    
    private static final Map<Long, String> AM_PM_UPPER = Map.of(
            0L, "AM",
            1L, "PM"
    );
    
    public static DateTimeFormatter getFormatter(String formattedSample) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        String current = formattedSample;
        while (!current.isEmpty()) current = addNext(formattedSample, current, builder);
        return builder.toFormatter(Locale.ROOT);
    }
    
    private static String addNext(String fullString, String str, DateTimeFormatterBuilder builder) {
        String whitespace = toString(str.codePoints().takeWhile(Character::isWhitespace));
        if (!whitespace.isEmpty()) builder.appendLiteral(whitespace);
        str = toString(str.codePoints().dropWhile(Character::isWhitespace));
        if (str.startsWith("2014")) {
            builder.appendValue(ChronoField.YEAR);
            return str.substring(4);
        } else if (str.startsWith("14")) {
            builder.appendValueReduced(ChronoField.YEAR, 2, 2, 0);
            return str.substring(2);
        } else if (str.startsWith("11")) {
            builder.appendValue(ChronoField.MONTH_OF_YEAR, 2);
            return str.substring(2);
        } else if (str.startsWith("November")) {
            builder.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL);
            return str.substring(8);
        } else if (str.startsWith("Nov")) {
            builder.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT);
            return str.substring(3);
        } else if (str.startsWith("21")) {
            builder.appendValue(ChronoField.DAY_OF_MONTH, 2);
            return str.substring(2);
        } else if (str.startsWith("Friday")) {
            builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL);
            return str.substring(6);
        } else if (str.startsWith("Fri")) {
            builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT);
            return str.substring(3);
        } else if (str.startsWith("17")) {
            builder.appendValue(ChronoField.HOUR_OF_DAY, 2);
            return str.substring(2);
        } else if (str.startsWith("05")) {
            builder.appendValue(ChronoField.HOUR_OF_AMPM, 2);
            return str.substring(2);
        } else if (str.startsWith("pm")) {
            builder.appendText(ChronoField.AMPM_OF_DAY, AM_PM_LOWER);
            return str.substring(2);
        } else if (str.startsWith("PM")) {
            builder.appendText(ChronoField.AMPM_OF_DAY, AM_PM_UPPER);
            return str.substring(2);
        } else if (str.startsWith("10")) {
            builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
            return str.substring(2);
        } else if (str.startsWith("00")) {
            builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
            return str.substring(2);
        } else if (str.startsWith("EET")) {
            builder.appendZoneText(TextStyle.SHORT);
            return str.substring(3);
        } else if (str.startsWith("+0200")) {
            builder.appendOffset("+HHMM", "Z");
            return str.substring(5);
        } else if (str.startsWith("+02:00")) {
            builder.appendOffset("+HH:MM", "Z");
            return str.substring(6);
        } else if (str.startsWith("+02")) {
            builder.appendOffset("+HH", "Z");
            return str.substring(3);
        } else if (!Character.isDigit(str.charAt(0))) {
            builder.appendLiteral("" + str.charAt(0));
            return str.substring(1);
        } else {
            throw new IllegalArgumentException("Invalid time zone pattern: " + fullString);
        }
    }
    
    public static ZonedDateTime queryFromPartial(TemporalAccessor accessor) {
        boolean hasMonthOrDay = accessor.isSupported(ChronoField.MONTH_OF_YEAR) || accessor.isSupported(ChronoField.DAY_OF_MONTH);
        boolean hasHour = accessor.isSupported(ChronoField.HOUR_OF_DAY);
        boolean hasAmPm = accessor.isSupported(ChronoField.AMPM_OF_DAY);
        boolean hasHourAmPm = accessor.isSupported(ChronoField.HOUR_OF_AMPM);
        boolean hasMinute = accessor.isSupported(ChronoField.MINUTE_OF_HOUR);
        boolean hasSecond = accessor.isSupported(ChronoField.SECOND_OF_MINUTE);
        
        if (((hasHourAmPm || hasAmPm) && !hasHour) || (!hasHour && (hasMinute || hasSecond)) || (!hasMinute && hasSecond)) {
            throw new IllegalStateException("Incomplete date format: Partially defined time");
        }
        
        LocalDate date = accessor.query(TemporalQueries.localDate());
        LocalTime time = accessor.query(TemporalQueries.localTime());
        ZoneId zone = accessor.query(TemporalQueries.zone());
        
        if (date == null && hasMonthOrDay) {
            throw new IllegalStateException("Incomplete date format: Partially defined date");
        }
        
        if (date == null && accessor.isSupported(ChronoField.YEAR)) date = LocalDate.of(accessor.get(ChronoField.YEAR), 1, 1);
        if (date == null) date = LocalDate.EPOCH;
        if (time == null) time = LocalTime.MIDNIGHT;
        if (zone == null) zone = ZoneId.of("UTC");
        return LocalDateTime.of(date, time).atZone(zone);
    }
    
    private static String toString(IntStream stream) {
        StringBuilder sb = new StringBuilder();
        stream.forEach(sb::appendCodePoint);
        return sb.toString();
    }
}
