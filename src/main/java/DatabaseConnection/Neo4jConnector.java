package DatabaseConnection;

import com.opencsv.*;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.Neo4jException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Neo4jConnector implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(Neo4jConnector.class.getName());
    private final Driver driver;

    public Neo4jConnector(String uri, String user, String password, Config config) {

        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password), config);
    }

    @Override
    public void close() {
        driver.close();
    }


    // this function creates a CSV contain all word-topic-doc information that needed to be written into database
    // it takes so long for Neo4j to process a large CSV
    // therefore you might have to split the whole word-topic-doc.csv into smaller ones
    public static void prepareDbData() throws IOException {

        ArrayList<ArrayList<String>> topicState = TopicModellingService.getTopicState();

        File f = new File("src/main/output/word-topic-doc-db.csv");

        FileWriter file = new FileWriter(f);
        CSVWriter writer = new CSVWriter(file);

        String[] header = {"wordId", "word", "docId", "topicId"};
        writer.writeNext(header);

        for (ArrayList<String> line : topicState) {

            String[] record = new String[]{line.get(3), line.get(4), line.get(1), line.get(5)};

            writer.writeNext(record, true);

        }
        writer.close();


    }

    // this function creates lists of word.text and topicId ordered by wordId as TXT
    public void getWordIdList() {

        Query query = new Query(
                """
                          MATCH (n:Word)-[:belongs_to]->(t:Topic)
                          WITH n.wordId AS wordId, n.text AS text, t.topicId AS topicId
                          RETURN wordId, topicId, text
                          ORDER BY wordId ASC;
                        """);

        try (Session session = driver.session()) {

            String[] wordArray = new String[19127];
            Arrays.fill(wordArray, "");
            String[] topicArray = new String[19127];
            Arrays.fill(topicArray, "");

            Stream<Map<String, Object>> stream = session.executeRead(tx -> {
                Result result = tx.run(query);

                while (result.hasNext()) {
                    Record record = result.next();
                    int wordId = Integer.parseInt(record.get("wordId").asString().substring(1));
                    String topicId = record.get("topicId").asString();
                    String text = record.get("text").asString();
                    wordArray[wordId] = text;
                    topicArray[wordId] = (topicArray[wordId].isEmpty() ? topicId : topicArray[wordId]);
                }

                FileWriter writer = null;
                try {
                    writer = new FileWriter("src/main/output/wordArray.txt");
                    int wordArrayLen = wordArray.length;
                    for (int i = 0; i < wordArrayLen; i++) {

                        writer.write(wordArray[i] + " ");
                    }

                    writer.close();

                    writer = new FileWriter("src/main/output/topicArray.txt");
                    int topicLen = topicArray.length;
                    for (int i = 0; i < topicLen; i++) {

                        writer.write(topicArray[i] + " ");
                    }

                    writer.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

//                System.out.println(Arrays.toString(wordArray));
//                System.out.println(Arrays.toString(topicArray));

                return result.list(r -> r.asMap()).stream();});

            session.close();

        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, query + " raised an exception", ex);
            throw ex;
        }

    }

    // this function creates lists of numDocs ordered by wordId as TXT
    // numDocs = number of documents that a word appeared, i.e. document frequency of a word
    public int[] getNumOfDocWordAppears() {

        int year = 2018;
        int endYear = 2023;

        Query query = new Query(
                """
                          MATCH (w:Word)-[:found_in]->(d:Document)
                          WHERE d.publishDate >= date({year: $year}) AND d.publishDate < date({year: $endYear})
                          WITH w.wordId AS wordId, count(d) AS numDocs
                          RETURN wordId, numDocs;
                        """,
                Map.of("year", year, "endYear", endYear));

        try (Session session = driver.session()) {

            int[] numDocsArray = new int[19127];
            Arrays.fill(numDocsArray, 0);

            Stream<Map<String, Object>> stream = session.executeRead(tx -> {
                Result result = tx.run(query);

                ArrayList<String[]> resultList = new ArrayList<>();
                String[] line = new String[2];



                while (result.hasNext()) {
                    Record record = result.next();
                    int wordId = Integer.parseInt(record.get("wordId").asString().substring(1));
                    int numDocs = record.get("numDocs").asInt();
                    numDocsArray[wordId] = numDocs;
                }

                FileWriter writer = null;
                try {
                    writer = new FileWriter("src/main/output/numDocsArray.txt");
                    int len = numDocsArray.length;
                    for (int i = 0; i < len; i++) {

                        writer.write(numDocsArray[i] + " ");
                    }

                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

//                System.out.println(Arrays.toString(numDocsArray));

                return result.list(r -> r.asMap()).stream();});

            session.close();

            return numDocsArray;


        } catch (Neo4jException ex) {
            LOGGER.log(Level.SEVERE, query + " raised an exception", ex);
            throw ex;
        }

    }

    // this function send the Cypher command to write :Topic, :Word and [:found_in] to database
    public void createTopicAndWord() throws Exception {

        System.out.println("Start Time: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date()));

        for (int i = 0; i < 6; i++) {

            System.out.print("Start sending Cypher " + i + "...  ");

            // the online address of the required CSV file
            String url = "https://raw.githubusercontent.com/kathycwy/Master-Thesis/master/src/main/output/word-topic-doc-db-" + i + ".csv";

            Query query = new Query(
                """
                           LOAD CSV WITH HEADERS FROM $url AS line
                           MERGE (t:Topic { topicId: line.topicId })
                           MERGE (w:Word { wordId: line.wordId, text: line.word })
                           MERGE (w)-[:belongs_to]->(t)
                           WITH w, line
                           MATCH (d:Document WHERE d.docId = line.docId)
                           MERGE (w)-[:found_in]->(d);
                        """,
                    Map.of("url", url));
            runCypher(query);

            System.out.println("Completed");
        }

        System.out.println("End Time: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date()));

    }

    // this function send the Cypher command to write :Website, :Document and [:contains] to database
    public void createWebsiteAndDocument() {

        System.out.println("Start Time: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date()));

        System.out.print("Start sending Cypher...  ");

        // the online address of the required CSV file
        String url = "https://raw.githubusercontent.com/kathycwy/Master-Thesis/master/src/main/output/raw-200.csv";

        Query query = new Query(
            """
                        LOAD CSV WITH HEADERS FROM $url AS line
                        MERGE (s:Website { siteId: line.SiteId, siteName: line.SiteName, siteUrl: line.SiteUrl })
                        CREATE (d:Document { docId: line.DocId, publishDate: date(line.PublishDate), visitDate: date(line.VisitDate), url: line.Url })
                        CREATE (s)-[:contains]->(d);
                    """,
                Map.of("url", url));
        runCypher(query);

        System.out.println("Completed");

        System.out.println("End Time: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date()));

    }

    // this function send the Cypher command to write property score to database
    public void createScore() throws Exception {

        System.out.println("Start Time: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date()));

        System.out.print("Start sending Cypher...  ");

        // the online address of the required CSV file
        String url = "https://raw.githubusercontent.com/kathycwy/Master-Thesis/master/src/main/output/calWeakSignals/WeakSignalValues_17.csv";

        Query query = new Query(
                """
                           LOAD CSV WITH HEADERS FROM $url AS line
                           MATCH (w:Word {wordId: line.wordId})
                           SET w.score = line.score;
                        """,
                Map.of("url", url));
        runCypher(query);

        System.out.println("Completed");

        System.out.println("End Time: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date()));

    }

    // this function send the Cypher command to write property label to database
    public void createTopicLabel() throws Exception {

        System.out.println("Start Time: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date()));

        System.out.print("Start sending Cypher...  ");

        // the online address of the required CSV file
        String url = "https://raw.githubusercontent.com/kathycwy/Master-Thesis/master/Mallet-202108/output/keys_topic_label.csv";

        Query query = new Query(
                """
                           LOAD CSV FROM $url AS line
                           MATCH (t:Topic {topicId: line[0]})
                           SET t.label = line[1];
                        """,
                Map.of("url", url));
        runCypher(query);

        System.out.println("Completed");

        System.out.println("End Time: " + new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new java.util.Date()));

    }

    public Result runCypher(final Query query) {

        Result result;

        try (Session session = driver.session()) {

            // Write transactions allow the driver to handle retries and transient errors
            result = session.executeWrite(tx -> tx.run(query));

        } catch (Neo4jException ex) {
            // capture any errors along with the query and data for traceability
            LOGGER.log(Level.SEVERE, query + " raised an exception", ex);
            throw ex;
        }

        return result;
    }


    public static void main(String... args) throws IOException {

        // change here with your database credentials
        String uri = "neo4j+s://7ffde07c.databases.neo4j.io:7687";
        String user = "neo4j";
        String password = "password";

        try (Neo4jConnector app = new Neo4jConnector(uri, user, password, Config.defaultConfig())) {

        // prepare the required CSV files
//            app.prepareDbData();

        // write data into the database
//            app.createWebsiteAndDocument();
//            app.createTopicAndWord();
//            app.createScore();
//            app.createTopicLabel();

        // get data from database
//            app.getNumOfDocWordAppears();
//            app.getWordIdList();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}