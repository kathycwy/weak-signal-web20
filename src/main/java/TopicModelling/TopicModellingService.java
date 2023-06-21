package TopicModelling;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class TopicModellingService {

    public static void main(String[] args) throws Exception {
        prepareTxt();
        runMallet();
    }

    // MALLET takes each document as individual TXT
    // this function splits each cleaned text into a separate TXT
    static void prepareTxt() throws IOException {

        BufferedReader br = new BufferedReader(new FileReader("src/main/output/raw-200-clean.csv"));

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreQuotations(false)
                .build();

        CSVReader csvReader = new CSVReaderBuilder(br)
                .withSkipLines(1)
                .withCSVParser(parser)
                .build();

        String[] line;
        while ((line = csvReader.readNext()) != null) {

            // path for TXT files of each cleaned text
            File file = new File("Mallet-202108/data/" + line[0] + ".txt");
            FileWriter myWriter = new FileWriter(file);
            myWriter.write(line[1]);
            myWriter.close();

        }

    }

    static public void runMallet() throws IOException, InterruptedException {

        // MALLET runs in command line
        // if this function is not working, please run the below two commands in terminal

        Runtime rt = Runtime.getRuntime();

        // transform individual txt files into a single MALLET format file
        Process pr = rt.exec("Mallet-202108/bin/mallet import-dir --input Mallet-202108/data --output Mallet-202108/output/input.mallet --keep-sequence");

        // trains MALLET to find 50 topics
        pr = rt.exec("Mallet-202108/bin/mallet train-topics  --input Mallet-202108/output/input.mallet --num-topics 100 --optimize-interval 10 --output-state Mallet-202108/output/topic-state.gz --output-topic-keys Mallet-202108/output/keys.txt --output-doc-topics Mallet-202108/output/composition.txt");

    }

    static public ArrayList<ArrayList<String>> getTopicState() throws IOException {

        ArrayList<ArrayList<String>> result = new ArrayList<>();
        ArrayList<String> lines = new ArrayList<>();

        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream("Mallet-202108/output/topic-state-all.gz"));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

        // skip the first 3 lines
        br.readLine();
        br.readLine();
        br.readLine();

        for (String line; (line = br.readLine()) != null; ) {
            lines.add(line);
        }

        for (String str : lines) {
            ArrayList<String> data = new ArrayList<>(Arrays.asList(str.split(" ")));
            // get docId from the filepath
            data.set(1, data.get(1).substring(19, data.get(1).length() - 4));
            // add W before word id
            data.set(3, "W" + data.get(3));
            // add T before topic id
            data.set(5, "T" + data.get(5));
            result.add(data);
        }

        return result;
    }

    static public ArrayList<ArrayList<String>> getCompositionTxt() throws IOException {

        ArrayList<ArrayList<String>> result = new ArrayList<>();
        ArrayList<String> lines = new ArrayList<>();

        // filepath for the composition.txt created in TopicModelling
        BufferedReader br = new BufferedReader(new FileReader("Mallet-202108/output/composition.txt"));

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreQuotations(false)
                .build();

        CSVReader csvReader = new CSVReaderBuilder(br)
                .withSkipLines(1)
                .withCSVParser(parser)
                .build();

        for (String line; (line = br.readLine()) != null; ) {
            lines.add(line);
        }

        for (String str : lines) {
            ArrayList<String> data = new ArrayList<>(Arrays.asList(str.split("\t")));
            result.add(data);
        }

        return result;
    }

    // get docId and publishYear
    static public ArrayList<ArrayList<String>> getDocIdPubYear() throws IOException {

        ArrayList<ArrayList<String>> result = new ArrayList<>();

        // filepath for the CSV created in DataCollection
        BufferedReader br = new BufferedReader(new FileReader("src/main/output/raw-200.csv"));

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreQuotations(false)
                .build();

        CSVReader csvReader = new CSVReaderBuilder(br)
                .withSkipLines(1)
                .withCSVParser(parser)
                .build();

        String[] line;
        while ((line = csvReader.readNext()) != null) {
            ArrayList<String> lines = new ArrayList<>();
            lines.add(line[0]);
            lines.add(line[1]);
            result.add(lines);
        }

        return result;
    }

    // get the composition value of a word within certain years
    static public double[] getCompositionArray(String[] years) throws IOException {

        ArrayList<ArrayList<String>> topicState = getTopicState();
        ArrayList<ArrayList<String>> compositionTxt = getCompositionTxt();
        ArrayList<ArrayList<String>> raw200 = getDocIdPubYear();

        // total number of words = 19127
        double[] compositions = new double[19127];

        int wordId;
        int docRow;
        int topicColumn;

        boolean withinYears = false;

        for (ArrayList<String> line : topicState) {

            String docId = line.get(1);

                for (ArrayList<String> rawLine : raw200) {

                    if (rawLine.get(0).equals(docId)) {

                        // check if PublishDate within 5 years
                        String publishYear = rawLine.get(1).substring(0,4);
                        withinYears = Arrays.asList(years).contains(publishYear);

                        // break when the required docId is found
                        break;
                    }

                }

            if (withinYears) {
                wordId = Integer.parseInt(line.get(3).substring(1));
                docRow = Integer.parseInt(line.get(0));
                topicColumn = Integer.parseInt(line.get(5).substring(1));

                ArrayList<String> compositionLine = compositionTxt.get(docRow);
                String value = compositionLine.get(topicColumn + 2);
                double val = Double.parseDouble(value);
                compositions[wordId] += val;
            }

        }

        return compositions;

    }
}
