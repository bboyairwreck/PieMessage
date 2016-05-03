import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by eric on 12/8/15.
 */
public class Constants {
    public static final String socketAddress = "127.0.0.1"; // INSERT YOUR PUBLIC IP HERE linked to OSX Client

    public static final long IncomingThreadResetDuration = 10 * 60 * 1000;  // 10 min

    public static long getNowEpochSeconds() {
        return (getNowMilliseconds() - get2001Milliseconds())/1000;
    }

    public static long get2001Milliseconds() {
        Date date2001 = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
            String dateInString = "01-01-2001 00:00:00";

            date2001 = sdf.parse(dateInString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        long time2001 = date2001.getTime();

        return time2001;
    }

    public static long get2001Seconds() {
        return get2001Milliseconds()/1000;
    }

    public static long getNowMilliseconds() {
        return System.currentTimeMillis();
    }
}
