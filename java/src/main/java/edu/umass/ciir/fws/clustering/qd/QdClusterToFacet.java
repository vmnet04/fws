package edu.umass.ciir.fws.clustering.qd;

import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.tool.app.ProcessQueryParametersApp;
import edu.umass.ciir.fws.types.TfQuery;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.TextProcessing;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

/**
 * Tupleflow application that does reads in query dimension clusters and output
 * query facets.
 *
 *
 * @author wkong
 */
public class QdClusterToFacet extends ProcessQueryParametersApp {

    @Override
    protected Class getProcessClass() {
        return QdClusterToFacetConverter.class;
    }

    @Override
    public String getName() {
        return "facet-qd";
    }

    @Override
    protected Class getQueryParametersGeneratorClass() {
        return GenerateQdFacetParameters.class;
    }

    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQuery")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class GenerateQdFacetParameters extends StandardStep<TfQuery, TfQueryParameters> {

        List<Double> distanceMaxs;
        List<Double> websiteCountMins;
        List<Double> itemRatios;

        public GenerateQdFacetParameters(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            distanceMaxs = p.getList("qdDistanceMaxs");
            websiteCountMins = p.getList("qdWebsiteCountMins");
            itemRatios = p.getList("qdItemRatios");

        }

        @Override
        public void process(TfQuery query) throws IOException {
            for (double distanceMax : distanceMaxs) {
                for (double websiteCountMin : websiteCountMins) {
                    for (double itemRatio : itemRatios) {
                        String parameters = Utility.parametersToString(distanceMax, websiteCountMin, itemRatio);
                        processor.process(new TfQueryParameters(query.id, query.text, parameters));
                    }
                }
            }

        }

    }

    /**
     * Terms are selected if it pass the condition (score > NumSite *
     * itemRatio). See paper "Finding dimensions for queries".
     */
    @Verified
    @InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
    public static class QdClusterToFacetConverter implements Processor<TfQueryParameters> {

        String facetDir;
        String clusterDir;
        String runDir;

        public QdClusterToFacetConverter(TupleFlowParameters parameters) {
            Parameters p = parameters.getJSON();
            runDir = p.getString("qdRunDir");
            facetDir = Utility.getFileName(runDir, "facet");
            clusterDir = Utility.getFileName(runDir, "cluster");
        }

        @Override
        public void process(TfQueryParameters queryParameters) throws IOException {
            System.err.println(String.format("Processing qid:%s parameters:%s", queryParameters.id, queryParameters.parameters));
            String qid = queryParameters.id;
            String[] fields = Utility.splitParameters(queryParameters.parameters);
            double distanceMax = Double.parseDouble(fields[0]);
            double websiteCountMin = Double.parseDouble(fields[1]);
            double itemRatio = Double.parseDouble(fields[2]);

            File facetFile = new File(Utility.getQdFacetFileName(facetDir, qid, distanceMax, websiteCountMin, itemRatio));
            if (facetFile.exists()) {
                Utility.infoFileExists(facetFile);
                return;
            }

            // loadClusters clusters
            String clusterFileName = Utility.getQdClusterFileName(clusterDir, qid, distanceMax, websiteCountMin);
            BufferedReader reader = Utility.getReader(clusterFileName);

            String line;
            ArrayList<ScoredFacet> facets = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                String[] fields2 = line.split("\t");
                double score = Double.parseDouble(fields2[0]);
                int siteNum = Integer.parseInt(fields2[1]);
                double threshold = siteNum * itemRatio;
                String facetTermList = fields2[2];

                ArrayList<ScoredItem> items = new ArrayList<>();
                for (String scoredItemStr : facetTermList.split("\\|")) {
                    ScoredItem scoredItem = new ScoredItem(scoredItemStr);
                    if (scoredItem.score > 1 && scoredItem.score > threshold) {
                        items.add(scoredItem);
                    }
                }

                if (items.size() > 0) {
                    facets.add(new ScoredFacet(items, score));
                }

            }

            Utility.infoOpen(facetFile);
            Utility.createDirectoryForFile(facetFile);
            ScoredFacet.outputAsFacets(facets, facetFile);
            Utility.infoWritten(facetFile);
        }

        @Override
        public void close() throws IOException {

        }

    }

}
