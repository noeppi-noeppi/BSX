package bsx.runtime.date;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
    
    private static final Map<Long, String> ERA_LOWER = Map.of(
            0L, "bc",
            1L, "ad"
    );
    
    private static final Map<Long, String> ERA_UPPER = Map.of(
            0L, "BC",
            1L, "AD"
    );
    
    private static final Map<Long, String> ISO_ERA_LOWER = Map.of(
            0L, "ce",
            1L, "bce"
    );
    
    private static final Map<Long, String> ISO_ERA_UPPER = Map.of(
            0L, "CE",
            1L, "BCE"
    );
    
    private static final Map<Long, String> DAYS_WITH_SUFFIX;

    static {
        Map<Long, String> map = new HashMap<>();
        for (int i = 1; i <= 31; i++) map.put((long) i, String.format("%02d", i) + switch (i % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        });
        // 11 and 12 are the exceptions
        map.put(11L, "11th");
        map.put(12L, "12th");
        DAYS_WITH_SUFFIX = Map.copyOf(map);
    }

    public static DateTimeFormatter getFormatter(String formattedSample) {
        String current = formattedSample;
        Builder builder = new Builder();
        while (!current.isEmpty()) current = addNext(formattedSample, current, builder);
        return builder.build(Locale.ENGLISH); // Must use english, for full text month and weekday to work
    }
    
    private static String addNext(String fullString, String str, Builder builder) {
        String whitespace = toString(str.codePoints().takeWhile(Character::isWhitespace));
        if (!whitespace.isEmpty()) builder.add(b -> b.appendLiteral(whitespace));
        str = toString(str.codePoints().dropWhile(Character::isWhitespace));
        if (str.startsWith("1416582600")) {
            builder.add(b -> b.appendValue(ChronoField.INSTANT_SECONDS));
            return str.substring(10);
        } else if (str.startsWith("2014")) {
            builder.add((ctx, b) -> b.appendValue(ctx.hasEra() ? ChronoField.YEAR_OF_ERA : ChronoField.YEAR));
            return str.substring(4);
        } else if (str.startsWith("14")) {
            builder.add(b -> b.appendValueReduced(ChronoField.YEAR, 2, 2, 0));
            return str.substring(2);
        } else if (str.startsWith("11")) {
            builder.add(b -> b.appendValue(ChronoField.MONTH_OF_YEAR, 2));
            return str.substring(2);
        } else if (str.startsWith("November")) {
            builder.add(b -> b.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL));
            return str.substring(8);
        } else if (str.startsWith("Nov")) {
            builder.add(b -> b.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT));
            return str.substring(3);
        } else if (str.startsWith("21st")) {
            builder.add(b -> b.appendText(ChronoField.DAY_OF_MONTH, DAYS_WITH_SUFFIX));
            return str.substring(4);
        } else if (str.startsWith("21")) {
            builder.add(b -> b.appendValue(ChronoField.DAY_OF_MONTH, 2));
            return str.substring(2);
        } else if (str.startsWith("Friday")) {
            builder.add(b -> b.appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL));
            return str.substring(6);
        } else if (str.startsWith("Fri")) {
            builder.add(b -> b.appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT));
            return str.substring(3);
        } else if (str.startsWith("17")) {
            builder.add(b -> b.appendValue(ChronoField.HOUR_OF_DAY, 2));
            return str.substring(2);
        } else if (str.startsWith("05")) {
            builder.add(b -> b.appendValue(ChronoField.HOUR_OF_AMPM, 2));
            return str.substring(2);
        } else if (str.startsWith("pm")) {
            builder.add(b -> b.appendText(ChronoField.AMPM_OF_DAY, AM_PM_LOWER));
            return str.substring(2);
        } else if (str.startsWith("PM")) {
            builder.add(b -> b.appendText(ChronoField.AMPM_OF_DAY, AM_PM_UPPER));
            return str.substring(2);
        } else if (str.startsWith("ad")) {
            builder.add(b -> b.appendText(ChronoField.ERA, ERA_LOWER));
            builder.setHasEra();
            return str.substring(2);
        } else if (str.startsWith("AD")) {
            builder.add(b -> b.appendText(ChronoField.ERA, ERA_UPPER));
            builder.setHasEra();
            return str.substring(2);
        } else if (str.startsWith("ce")) {
            builder.add(b -> b.appendText(ChronoField.ERA, ISO_ERA_LOWER));
            builder.setHasEra();
            return str.substring(2);
        } else if (str.startsWith("CE")) {
            builder.add(b -> b.appendText(ChronoField.ERA, ISO_ERA_UPPER));
            builder.setHasEra();
            return str.substring(2);
        } else if (str.startsWith("10")) {
            builder.add(b -> b.appendValue(ChronoField.MINUTE_OF_HOUR, 2));
            return str.substring(2);
        } else if (str.startsWith("00")) {
            builder.add(b -> b.appendValue(ChronoField.SECOND_OF_MINUTE, 2));
            return str.substring(2);
        } else if (str.startsWith("EET")) {
            builder.add(b -> b.appendZoneText(TextStyle.SHORT));
            return str.substring(3);
        } else if (str.startsWith("+0200")) {
            builder.add(b -> b.appendOffset("+HHMM", "Z"));
            return str.substring(5);
        } else if (str.startsWith("+02:00")) {
            builder.add(b -> b.appendOffset("+HH:MM", "Z"));
            return str.substring(6);
        } else if (str.startsWith("+02")) {
            builder.add(b -> b.appendOffset("+HH", "Z"));
            return str.substring(3);
        } else if (!Character.isDigit(str.charAt(0))) {
            String chr = Character.toString(str.charAt(0));
            builder.add(b -> b.appendLiteral(chr));
            return str.substring(1);
        } else {
            throw new IllegalArgumentException("Invalid date pattern: " + fullString);
        }
    }
    
    public static ZonedDateTime queryFromPartial(TemporalAccessor accessor) {
        LocalDate date;
        LocalTime time;
        
        if (accessor.isSupported(ChronoField.INSTANT_SECONDS)) {
            date = accessor.query(TemporalQueries.localDate());
            time = accessor.query(TemporalQueries.localTime());
        } else {
            boolean hasMonthOrDay = accessor.isSupported(ChronoField.MONTH_OF_YEAR) || accessor.isSupported(ChronoField.DAY_OF_MONTH);
            boolean hasHour = accessor.isSupported(ChronoField.HOUR_OF_DAY);
            boolean hasAmPm = accessor.isSupported(ChronoField.AMPM_OF_DAY);
            boolean hasHourAmPm = accessor.isSupported(ChronoField.HOUR_OF_AMPM);
            boolean hasMinute = accessor.isSupported(ChronoField.MINUTE_OF_HOUR);
            boolean hasSecond = accessor.isSupported(ChronoField.SECOND_OF_MINUTE);

            if (((hasHourAmPm || hasAmPm) && !hasHour) || (!hasHour && (hasMinute || hasSecond)) || (!hasMinute && hasSecond)) {
                throw new IllegalStateException("Incomplete date format: Partially defined time");
            }

            date = accessor.query(TemporalQueries.localDate());
            time = accessor.query(TemporalQueries.localTime());

            if (date == null && hasMonthOrDay) {
                throw new IllegalStateException("Incomplete date format: Partially defined date");
            }
        }
        
        ZoneId zone = accessor.query(TemporalQueries.zone());
        
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
    
    private static class Builder {
        
        private final List<BiConsumer<BuildContext, DateTimeFormatterBuilder>> actions;
        private boolean hasEra;
        
        public Builder() {
            this.actions = new ArrayList<>();
            this.hasEra = false;
            
        }
        
        public void setHasEra() {
            this.hasEra = true;
        }
        
        public void add(Consumer<DateTimeFormatterBuilder> action) {
            this.actions.add((ctx, builder) -> action.accept(builder));
        }
        
        public void add(BiConsumer<BuildContext, DateTimeFormatterBuilder> action) {
            this.actions.add(action);
        }
        
        public DateTimeFormatter build(Locale locale) {
            BuildContext ctx = new BuildContext(this.hasEra);
            DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
            for (BiConsumer<BuildContext, DateTimeFormatterBuilder> action : this.actions) {
                action.accept(ctx, builder);
            }
            return builder.toFormatter(locale);
        }
    }
    
    private record BuildContext(boolean hasEra) {}
}
