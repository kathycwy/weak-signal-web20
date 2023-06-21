package DataCollection;

import java.io.File;
import java.util.ArrayList;

public class CrawlService {

    public static WebsiteDetailsClass[] websiteDetails = new WebsiteDetails().getAllWebsiteDetails();

    public static void main(String[] args) throws Exception {

        // delete existing CSV file if any
        File f = new File("src/main/output/raw.csv");
        if (f.delete()) {
            System.out.println("Old CSV file deleted");
        }

        // crawl for the first 5 blog websites
        for (WebsiteDetailsClass site : websiteDetails) {
            Crawler.crawlStaticPage(1, site, site.rootUrl, new ArrayList<String>());
        }

        // crawl for the podcast website
        Crawler.crawlPodscribeTranscript(WebsiteDetails.site6);

    }

}
