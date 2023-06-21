package TextCleaning;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import io.github.pepperkit.corenlp.stopwords.StopWordsAnnotator;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class NlpTextProcessor {

    public static Set<String> removeUrl(Set<String> text) {

        String urlPattern = "((\\S+)((:\\/\\/)|(\\.)|(\\/))((\\S)+))";
        text.removeIf(t -> t.matches(urlPattern));
        return text;

    }

    public static Set<String> removePosAndStopWords(String str) {

        // stopwords list from Ranks NL
        final String stopWordsResourcePath = "stopwords.txt";

        final Properties props;
        props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, stopwords");
        props.setProperty("customAnnotatorClass.stopwords", "io.github.pepperkit.corenlp.stopwords.StopWordsAnnotator");
        props.setProperty("ssplit.isOneSentence", "true");

        // to filter out all the punctuation and simple words like be, so, etc.
        props.setProperty("stopwords.withLemmasShorterThan", "2");

        // to filter out all the common and simple words
        props.setProperty("stopwords.withPosCategories",
                "NNP,NNPS," + // proper noun singular and plural
                        "PDT," + // predeterminer
                        "IN,CC," + // conjunction and coordinating conjunction (but, and etc.)
                        "DT," + // determiner - the, a, etc.
                        "UH," + // interjection - my, his, oh, uh etc.
                        "FW," + // foreign word
                        "MD," + // modal verb
                        "RP," + // particle
                        "PRP,PRP$," + // personal pronoun
                        "EX," + // existential there
                        "POS," + // possessive ending: 's
                        "SYM," + // symbol
                        "WDT,WP,WP$," + // wh-determiner (who), wh-pronoun (who, what, whom) and possessive wh-pronoun (whose)
                        "WRB," + // wh-adverb
                        "CD," //cardinal number
        );

        // to provide stop words list
        props.setProperty("stopwords.customListResourcesFilePath", stopWordsResourcePath);

        // to annotate text with POS taggers
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(str);
        pipeline.annotate(document);

        // to return tokens
        Set<String> result = new HashSet<>();
        List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);

        // to return interesting words
        for (CoreLabel token : tokens) {
            if (!token.get(StopWordsAnnotator.class)) {
                result.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return result;

    }

}
