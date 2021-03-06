/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.ffeedback;

import edu.umass.ciir.fws.types.TfQueryExpansion;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryExpansion")
public class RunExpandedQuery implements Processor<TfQueryExpansion> {

    Retrieval retrieval;
    Parameters p;
    ExpansionDirectory expansionDir;

    public RunExpandedQuery(TupleFlowParameters parameters) throws Exception {
        p = parameters.getJSON();
        expansionDir = new ExpansionDirectory(p);
        retrieval = RetrievalFactory.instance(p);
    }

    @Override
    public void process(TfQueryExpansion qe) throws IOException {
        Utility.infoProcessing(qe);
        File outfile = new File(Utility.getExpansionRunFileName(expansionDir.runDir, qe));
        Utility.createDirectoryForFile(outfile);

        String queryNumber = qe.qid;
        String queryText = qe.expanedQuery;

        // parse and transform query into runnable form
        List<ScoredDocument> results = null;

        Node root = StructuredQuery.parse(queryText);
        Node transformed;
        try {
            transformed = retrieval.transformQuery(root, p);
            // run query
            results = retrieval.executeQuery(transformed, p).scoredDocuments;
        } catch (Exception ex) {
            Logger.getLogger(RunOracleCandidateExpasions.class.getName()).log(Level.SEVERE, "error in running for "
                    + qe.toString(), ex);
        }

        Utility.infoOpen(outfile);
        BufferedWriter writer = Utility.getWriter(outfile);
        // if we have some results -- print in to output stream
        if (!results.isEmpty()) {
            for (ScoredDocument sd : results) {
                writer.write(sd.toTRECformat(queryNumber));
                writer.newLine();
            }
        } else {
            writer.write(String.format("%s Q0 clueweb09-xxxxxx-xx-xxxxx 1 -1 galago", queryNumber));
            writer.newLine();
        }
        writer.close();
        Utility.infoWritten(outfile);
    }

    @Override
    public void close() throws IOException {
    }
}
