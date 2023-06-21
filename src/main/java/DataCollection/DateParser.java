package DataCollection;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateParser {

    public static SimpleDateFormat finalFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    public DateParser() throws ParseException {
    }

    public static Date parseDate(int parser, String str) throws ParseException {
        switch (parser) {
            case 1:
                return dateParser1(str);
            case 2:
                return dateParser2(str);
            case 3:
                return dateParser3(str);
            case 4:
                return dateParser4(str);
            case 5:
                return dateParser5(str);
        }

        return new Date();
    }

    public static Date dateParser1(String str) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
        Date date = formatter.parse(str);

        return date;
    }

    public static Date dateParser2(String str) throws ParseException {

        str = StringUtils.substringBefore(str, " at");

        SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        Date date = formatter.parse(str);

        return date;
    }

    public static Date dateParser3(String str) throws ParseException {

        SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        Date date = formatter.parse(str);

        return date;
    }

    public static Date dateParser4(String str) throws ParseException {

        str = StringUtils.replace(str, "st", "");
        str = StringUtils.replace(str, "nd", "");
        str = StringUtils.replace(str, "rd", "");
        str = StringUtils.replace(str, "th", "");

        SimpleDateFormat formatter = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        Date date = formatter.parse(str);

        return date;
    }

    public static Date dateParser5(String str) throws ParseException {

        str = StringUtils.substringBefore(str, " ");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-M-d", Locale.ENGLISH);
        Date date = formatter.parse(str);

        return date;
    }

}
