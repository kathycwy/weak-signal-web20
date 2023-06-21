package TextCleaning;

import com.opencsv.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class NlpService {

    // filepath for the CSV created in DataCollection
    private static String filepath = "src/main/output/raw-200.csv";

    public static void main(String[] args) throws IOException {

        ArrayList<String> rawText = getRawText(filepath);

        System.out.println("Text cleaning and tokenizing STARTED.");

        int count = rawText.size();

        // filepath for the CSV created in TextCleaning
        File f = new File("src/main/output/report/raw-200-clean.csv");
        FileWriter file = new FileWriter(f);
        CSVWriter writer = new CSVWriter(file);
        String[] header = { "DocId", "Tokens" };
        writer.writeNext(header);

        Set<String> tokens = null;
        int index = 0;
        for (String text : rawText) {

            tokens = NlpTextProcessor.removePosAndStopWords(text);

            tokens = NlpTextProcessor.removeUrl(tokens);

            String[] record = new String[]{"D"+index, String.valueOf(tokens)};

            writer.writeNext(record, true);

            System.out.println("[" + index++ + "/" + count + "] " + Arrays.toString(tokens.toArray()));

            }

        writer.close();

        System.out.println("Text cleaning and tokenizing COMPLETED.");

    }

    // get only the words from raw-200.csv
    static ArrayList<String> getRawText(String path) {

        ArrayList<String> rawText = new ArrayList<>();

        try {

            BufferedReader br = new BufferedReader(new FileReader(filepath));

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
                if (line != null) {
                    rawText.add(line[7]);
                }
            }

        } catch (IOException e) {
            System.out.println("Error - Input source not found.");
        }

        return rawText;

    }

}
