/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.crawl.QuerySetDocuments;
import edu.umass.ciir.fws.crawl.Document;
import edu.umass.ciir.fws.types.CandidateList;
import edu.umass.ciir.fws.types.Query;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Candidate lists extractor that will be called by Tupleflow jobs to extract
 * candidate lists. 
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.Query")
@OutputClass(className = "edu.umass.ciir.fws.types.CandidateList")
public class CandidateListExtractor extends StandardStep<Query, CandidateList> {

    QuerySetDocuments querySetDocuments;
    CandidateListHtmlExtractor cListHtmlExtractor;
    CandidateListTextExtractor cListTextExtractor;
    String parseDir;

    public CandidateListExtractor(TupleFlowParameters parameters) throws Exception {
        Parameters p = parameters.getJSON();
        querySetDocuments = new QuerySetDocuments(p);
        cListHtmlExtractor = new CandidateListHtmlExtractor();
        cListTextExtractor = new CandidateListTextExtractor();
        parseDir = p.getString("parseDir");
    }

    @Override
    public void process(Query query) throws IOException {
        List<Document> documents = querySetDocuments.get(query.id);
        for (Document doc : documents) {
            extractHtml(query, doc);
            extractText(query, doc);

        }
    }

    private void extractHtml(Query query, Document doc) throws IOException {
        List<edu.umass.ciir.fws.clist.CandidateList> lists
                = cListHtmlExtractor.extract(doc, query);
        for (edu.umass.ciir.fws.clist.CandidateList list : lists) {
            processor.process(list);
        }
    }

    private void extractText(Query query, Document doc) throws IOException {
        String parseFileName = String.format("%s%s%s%s%s.parse",
                parseDir, File.separator, query.id, File.separator, doc.name);
        String parseFileContent = Utility.readFileToString(new File(parseFileName));

        List<edu.umass.ciir.fws.clist.CandidateList> lists
                = cListTextExtractor.extract(doc, query, parseFileContent);
        for (edu.umass.ciir.fws.clist.CandidateList list : lists) {
            processor.process(list);
        }
    }
}
