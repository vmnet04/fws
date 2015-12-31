/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.gm.gmi;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.gm.GmIndependentClusterer;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiClusterParameters;
import edu.umass.ciir.fws.clustering.gm.gmi.GmiParameterSettings.GmiFacetParameters;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.DirectoryUtility;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 *
 * @author wkong
 */
@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
@OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class GmiClusterer implements Processor<TfQueryParameters> {

    String predictDir;
    String gmiClusterDir;
    String gmiRunDir;
    String gmiRunPredictDir;

    public GmiClusterer(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String gmDir = p.getString("gmDir");
        gmiRunDir = DirectoryUtility.getModelRunDir(gmDir, "gmi");
        predictDir = DirectoryUtility.getGmPredictDir(gmDir);
        gmiClusterDir = p.getString("gmiClusterDir");
        gmiRunPredictDir = DirectoryUtility.getGmPredictDir(gmiRunDir);
    }

    @Override
    public void process(TfQueryParameters queryParams) throws IOException {
        Utility.infoProcessing(queryParams);
        String[] folderIdOptionOthers = Utility.splitParameters(queryParams.text);
        String folderId = folderIdOptionOthers[0];
        String predictOrTune = folderIdOptionOthers[1];
        GmiClusterParameters params = new GmiClusterParameters(queryParams.parameters);
        double termProbTh = params.termProbTh;
        double pairProbTh = params.pairProbTh;

        File termPredictFile;
        File termPairPredictFile;
        File clusterFile;

        if (predictOrTune.equals("predict")) {
            termPredictFile = new File(DirectoryUtility.getGmTermPredictFileName(predictDir, queryParams.id));
            termPairPredictFile = new File(DirectoryUtility.getGmTermPairPredictFileName(predictDir, queryParams.id));
            //folderOptionRankerMetricIndex
            String ranker = folderIdOptionOthers[2];
            String metricIndex = folderIdOptionOthers[3];
            String gmiParams = Utility.parametersToFileNameString(ranker, metricIndex);
            clusterFile = new File(DirectoryUtility.getClusterFilename(gmiClusterDir, queryParams.id, "gmi", gmiParams));

            // move ranker to parameters
            GmiFacetParameters facetParams = new GmiFacetParameters(params.termProbTh, params.pairProbTh, ranker);
            queryParams.parameters = facetParams.toString();
            // do not skip for predicting, should overwrite for new tuning results.
            // if (clusterFile.exists()) { ...
        } else {
            termPredictFile = new File(DirectoryUtility.getGmTermPredictFileName(gmiRunPredictDir, queryParams.id));
            termPairPredictFile = new File(DirectoryUtility.getGmTermPairPredictFileName(gmiRunPredictDir, queryParams.id));
            clusterFile = new File(DirectoryUtility.getGmiFoldClusterFilename(gmiRunDir, folderId, queryParams.id, params.toFilenameString()));
        }

        Utility.infoOpen(clusterFile);
        Utility.createDirectoryForFile(clusterFile);
        GmIndependentClusterer gmi = new GmIndependentClusterer(termProbTh, pairProbTh);
        List<ScoredFacet> clusters = gmi.cluster(termPredictFile, termPairPredictFile);
        ScoredFacet.output(clusters, clusterFile);
        Utility.infoWritten(clusterFile);
    }

    @Override
    public void close() throws IOException {
    }
}
