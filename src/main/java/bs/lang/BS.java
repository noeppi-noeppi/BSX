package bs.lang;

import bsx.BsValue;
import bsx.resolution.Singleton;
import bsx.runtime.date.DateFormatHelper;
import bsx.type.StringType;
import bsx.value.StringValue;

import java.time.format.DateTimeFormatter;

@Singleton
public class BS {
    
    public static final BsValue EOL = new StringValue(StringType.ASCII, System.lineSeparator());
    
    @Singleton
    public static final class DateTime {
        
        public static Date Parse(String dateString, String formatString) {
            DateTimeFormatter formatter = DateFormatHelper.getFormatter(formatString);
            return new Date(DateFormatHelper.queryFromPartial(formatter.parse(dateString)));
        }
        
        public static String Format(Date date, String formatString) {
            DateTimeFormatter formatter = DateFormatHelper.getFormatter(formatString);
            return formatter.format(date.time);
        }
    }
}
