package DataCollection;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.util.Date;

public class Scraper {

    public static String scrape(String[] contentSelectors, Document doc) {
        Elements elements = new Elements();
        for (String selector : contentSelectors) {
            if (selector == contentSelectors[0]) {
                elements = doc.select(selector);
            }
            elements = elements.select(selector);
        }

        return (elements.text().isEmpty() ? null : elements.text());
    }

    public static Date scrapeDate(String[] contentSelectors, Document doc, int dateParser) throws ParseException {
        String str;
        Elements elements = new Elements();
        for (String selector : contentSelectors) {
            if (selector == contentSelectors[0]) {
                elements = doc.select(selector);
            }
            elements = elements.select(selector);
        }

        // this applied for [site5 - https://www.section.io/blog] only
        if (elements.text().isEmpty()) {
            str = elements.toString();
            str = StringUtils.substringAfter(str, "datePublished\": \"");
            str = StringUtils.substringBefore(str, " ");
        } else {
            str = elements.text();
        }

        // this applied for [site4 - https://scottaaronson.blog] only
        if (str.startsWith("This entry was posted on ")) {
            str = elements.text();
            str = StringUtils.substringAfter(str, ", ");
            str = StringUtils.substringBefore(str, " at");
        }

        Date date = DateParser.parseDate(dateParser, str);

        return date;
    }

}
