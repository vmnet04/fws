/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm;

import edu.umass.ciir.fws.query.QueryFileParser;
import edu.umass.ciir.fws.types.TfFolder;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfFolder")
@OutputClass(className = "edu.umass.ciir.fws.types.TfFolder")
public class CollectTermTrainData extends StandardStep<TfFolder, TfFolder> {

    String predictDir;
    String trainDir;
    TrainDataSampler dataSampler;
    boolean sample;

    public CollectTermTrainData(TupleFlowParameters parameters) throws IOException {
        Parameters p = parameters.getJSON();
        String gmDir = Utility.getFileName(p.getString("facetRunDir"), "gm");
        trainDir = Utility.getFileName(gmDir, "train");
        predictDir = Utility.getFileName(gmDir, "predict");
        dataSampler = new TrainDataSampler();
        sample = p.getBoolean("gmTermSample");

    }

    @Override
    public void process(TfFolder folder) throws IOException {
        String folderDir = Utility.getFileName(trainDir, folder.id);
        String trainQueryFile = Utility.getFileName(folderDir, "train.query"); // input
        File outfile = new File(Utility.getFileName(folderDir, "train.t.data.gz")); // output

        ArrayList<File> trainFiles = new ArrayList<>();
        TfQuery[] queries = QueryFileParser.loadQueryList(trainQueryFile);
        for (TfQuery query : queries) {
            File dataFile = new File(Utility.getGmTermDataFileName(predictDir, query.id));
            trainFiles.add(dataFile);
        }

        if (sample) {
            dataSampler.sampleToFile(trainFiles, outfile);
        } else {
            TrainDataSampler.combine(trainFiles, outfile);
        }
        
        Utility.infoWritten(outfile);
        processor.process(folder);

    }
}
