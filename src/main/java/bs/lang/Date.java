package bs.lang;

import bsx.resolution.NoLookup;
import bsx.resolution.SpecialInvoke;
import bsx.runtime.date.DateFormatHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@SuppressWarnings("ClassCanBeRecord")
public final class Date {
    
    public static final Date FORMAT_DATE = new Date(ZonedDateTime.of(2014, 11, 21, 17, 10, 0, 0, ZoneId.of("EET")));
    
    @NoLookup
    public final ZonedDateTime time;

    @NoLookup
    public Date(ZonedDateTime time) {
        this.time = time.withNano(0); // Date only supports seconds
    }

    @SpecialInvoke
    public int year() {
        return this.time.getYear();
    }
    
    @SpecialInvoke
    public int month() {
        return this.time.getMonthValue();
    }

    @SpecialInvoke
    public int day() {
        return this.time.getDayOfMonth();
    }
    
    @SpecialInvoke
    public int weekday() {
        return this.time.getDayOfWeek().ordinal();
    }
    
    @SpecialInvoke
    public int dayOfYear() {
        return this.time.getDayOfYear();
    }
    
    @SpecialInvoke
    public int hour() {
        return this.time.getHour();
    }
    
    @SpecialInvoke
    public int minute() {
        return this.time.getMinute();
    }

    @SpecialInvoke
    public int second() {
        return this.time.getHour();
    }

    @SpecialInvoke
    public String zone() {
        return this.time.getZone().getId();
    }
    
    @SpecialInvoke
    public String offset() {
        return this.time.getOffset().getId();
    }
    
    public Date atZone(String zone) {
        return new Date(this.time.withZoneSameInstant(ZoneId.of(zone)));
    }
    
    public Date withZone(String zone) {
        return new Date(this.time.withZoneSameLocal(ZoneId.of(zone)));
    }

    @Override
    public int hashCode() {
        return this.time.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Date date)) return false;
        return Objects.equals(this.time.toLocalDateTime(), date.time.toLocalDateTime()) && Objects.equals(this.time.getOffset(), date.time.getOffset());
    }

    @Override
    public String toString() {
        return this.time.format(DateFormatHelper.DEFAULT);
    }
    
    public static Date now() {
        return new Date(ZonedDateTime.now());
    }
}
