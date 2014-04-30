/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.crawl;

import edu.umass.ciir.fws.nlp.HtmlContentExtractor;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.core.eval.QueryResults;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Represents a document.
 *
 * @author wkong
 */
public class Document {

    public long rank;
    public String name;
    public String html;
    public String title;
    public String url;
    public String site;
    public List<String> terms;
    public HashMap<String, Integer> ngramMap; // ngram -> frequency

    public Document(ScoredDocument sd, org.lemurproject.galago.core.parse.Document document) {
        name = sd.documentName;
        rank = sd.rank;
        html = document.text;
        url = document.metadata.get("url");
        site = getSiteUrl(url);
        terms = TextProcessing.tokenize(HtmlContentExtractor.extractFromContent(html));
        title = TextProcessing.clean(HtmlContentExtractor.extractTitle(html));
    }

    public Document() {

    }

    public static String getSiteUrl(String url) {
        url = url.replaceAll("^https?://", "");
        url = url.replaceAll("/.*?$", "");
        url = url.replaceAll("\\|", "");
        url = url.toLowerCase();
        return url;
    }

    public static List<Document> loadDocumentsFromFiles(QueryResults queryResults, String docDir, String qid) throws IOException {
        ArrayList<Document> docs = new ArrayList<>();
        for (ScoredDocument sd : queryResults.getIterator()) {
            DataInputStream data = new DataInputStream(new FileInputStream(Utility.getDocDataFileName(docDir, qid, sd.documentName)));
            org.lemurproject.galago.core.parse.Document doc
                    = org.lemurproject.galago.core.parse.Document.deserialize(data, new Parameters(),
                            new org.lemurproject.galago.core.parse.Document.DocumentComponents(true, true, false));
            docs.add(new Document(sd, doc));
        }
        return docs;
    }
}
