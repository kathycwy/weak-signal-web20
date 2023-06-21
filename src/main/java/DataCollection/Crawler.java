package DataCollection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Crawler {

    public static int countTotal = 1;
    public static int countSaved = 1;


    // this crawler crawls and scrapes any static webpages by parsing their HTML content
    public static void crawlStaticPage(int level, WebsiteDetailsClass site, String url, ArrayList<String> visited) {
        if (level <= site.level) {
            Document doc = request(site, url, visited, level);
            if (doc != null) {
                for (Element link : doc.select("a[href]")) {
                    String next_link = link.absUrl("href");
                    if (next_link.startsWith(site.rootUrl)) {
                        if (!visited.contains(next_link)) {
                            crawlStaticPage(level++, site, next_link, visited);
                        }
                    }
                }
            }
        }
    }

    public static Document request(WebsiteDetailsClass site, String url, ArrayList<String> v, int level) {
        try {
            Connection con = Jsoup.connect(url);
            Document doc = con.get();
            if (con.response().statusCode() == 200) {
                if (url.matches(site.regex)) {
                    Date date = Scraper.scrapeDate(site.dateSelector, doc, site.dateParser);
                    Date boundary = DateParser.finalFormatter.parse("2013-01-01");
                    if (date.after(boundary)) {
                        String title = Scraper.scrape(site.titleSelector, doc);
                        String content = Scraper.scrape(site.contentSelectors, doc);
                        Calendar today = Calendar.getInstance();
                        today.set(Calendar.HOUR_OF_DAY, 0);
                        save(url, DateParser.finalFormatter.format(date), DateParser.finalFormatter.format(today.getTime()), content, countSaved++);
                    }
                }

                System.out.println(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + "; Site " + site.dateParser + "; Level " + level + "; Trying the [" + countTotal++ + "] links; Saved [" + (countSaved - 1) + "] links.");

                v.add(url);
                return doc;

            } else {
                return null;
            }
        } catch (IOException | ParseException e) {
            System.out.println(e);
            return null;
        }
    }


    // this crawler crawls the textual transcripts on Podscribe specifically
    public static void crawlPodscribeTranscript(String channelLink) throws Exception {

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);

        // get the JSON response containing the whole list of podcasts of a channel
        StringBuilder podcastListResult = new StringBuilder();
        URL channelUrl = new URL(channelLink);
        HttpURLConnection conn = (HttpURLConnection) channelUrl.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                podcastListResult.append(line);
            }
        }
        String podcastList = podcastListResult.toString();

        // parse the JSON response to get the "podcasts" object
        JSONObject podcastListObj = new JSONObject(podcastList);
//        JSONObject data = podcastListObj.getJSONObject("data");
        JSONArray podcastListArray = podcastListObj.getJSONObject("data").getJSONArray("podcasts");

        // loop each podcast item through the entire object
        for(int i = 0 ; i < podcastListArray.length() ; i++) {

            JSONObject podcastItem = (JSONObject) podcastListArray.get(i);

            // get the upload date of a podcast item
            String uploadedAt = podcastItem.getString("uploadedAt").substring(0, 10);

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date date = formatter.parse(uploadedAt);
            Date boundary = DateParser.finalFormatter.parse("2013-01-01");

            // get the transcription status of a podcast item
            String status = podcastItem.getJSONObject("transcription").getString("status");

            // proceed if the podcast item is dated after the boundary and its transcription is ready
            if (date.after(boundary) && status.equals("Done")) {

                // link to the webpage that displays the transcript
                int id = podcastItem.getInt("id");
                String link = "https://app.podscribe.ai/episode/" + id;

                // link to the endpoint that returns the transcript in JSON
                String transcriptEndpoint = "https://backend.podscribe.ai/api/episode?id=" + id;

                // get the JSON response containing the transcript
                StringBuilder transcriptResult = new StringBuilder();
                URL transcriptUrl = new URL(transcriptEndpoint);
                HttpURLConnection conn2 = (HttpURLConnection) transcriptUrl.openConnection();
                conn2.setRequestMethod("GET");
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn2.getInputStream()))) {
                    for (String line; (line = reader.readLine()) != null; ) {
                        transcriptResult.append(line);
                    }
                }
                String transcriptResponse = transcriptResult.toString();

                // parse the JSON response to get the transcription text
                JSONObject transcriptObj = new JSONObject(transcriptResponse);
                String text = transcriptObj.getJSONObject("transcription").getString("text");

                JSONArray textArray = new JSONArray(text);

                StringBuilder word = new StringBuilder();
                for (int k = 0; k < textArray.length(); k++) {
                    JSONObject t = (JSONObject) textArray.get(k);
                    word.append(t.getString("word") + " ");
                }

                // save to the CSV file
                save(link, DateParser.finalFormatter.format(date), DateParser.finalFormatter.format(today.getTime()), word.toString(), countSaved++);
            }


        }
    }

    public static void save(String url, String publishDate, String visitDate, String content, int count) throws IOException {

        File f = new File("src/main/output/raw-200.csv");
        if (!f.exists()) {
            FileWriter file = new FileWriter(f);
            CSVWriter writer = new CSVWriter(file);

            String[] header = { "DocId", "PublishDate", "VisitDate", "Url", "Content" };
            writer.writeNext(header);

            writer.close();
        }

        CSVWriter writer = new CSVWriter(new FileWriter(f, true));
        String[] record = new String[]{"D"+count, publishDate, visitDate, url, String.valueOf(content)};
        writer.writeNext(record, true);
        writer.close();

    }

}
