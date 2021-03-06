/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering.lda;

/**
 * Parts of the codes are taken
 * from:http://code.google.com/p/mltool4j/source/browse/trunk/src/edu/thu/mltool4j/topicmodel/plsa
 * . The original code has a bug, in E-step when computing P(z). It is fixed
 * here.
 *
 * @author wkong
 */
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.umass.ciir.fws.clist.CandidateList;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.clustering.lda.LdaParameterSettings.LdaClusterParameters;
import edu.umass.ciir.fws.types.TfQueryParameters;
import edu.umass.ciir.fws.utility.DirectoryUtility;
import edu.umass.ciir.fws.utility.Utility;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.Verified;

@Verified
@InputClass(className = "edu.umass.ciir.fws.types.TfQueryParameters")
public class LdaClusterer implements Processor<TfQueryParameters> {

    public final static String modelName = "lda";
    String clusterDir;
    String clistDir;
    long topNum; // top num of docs
    long iterNum;
    long topTermNum; // #terms to output for each topic

    public LdaClusterer(TupleFlowParameters parameters) {
        Parameters p = parameters.getJSON();
        String facetRunDir = p.getString("facetRunDir");
        clusterDir = DirectoryUtility.getCluterDir(facetRunDir, modelName);
        iterNum = p.getLong("ldaIterNum");
        clistDir = p.getString("clistDir");
        topNum = p.getLong("topNum");
        topTermNum = p.getLong("ldaTopTermNum");
    }

    @Override
    public void process(TfQueryParameters queryParameters) throws IOException {
        String qid = queryParameters.id;
        System.err.println(String.format("Processing qid:%s parameters:%s", qid, queryParameters.parameters));

        LdaClusterParameters params = new LdaClusterParameters(queryParameters.parameters);
        int topicNum = (int) params.topicNum;

        File clusterFile = new File(DirectoryUtility.getClusterFilename(clusterDir, qid, modelName, params.toFilenameString()));
//        if (clusterFile.exists()) {
//            Utility.infoFileExists(clusterFile);
//            return;
//        }
        // loadClusters candidate lists
        File clistFile = new File(Utility.getCandidateListCleanFileName(clistDir, qid));
        List<CandidateList> clist = CandidateList.loadCandidateLists(clistFile, topNum);

        // clustering
        Lda lda = new Lda(clist, iterNum, topTermNum);
        List<ScoredFacet> facets = lda.cluster(topicNum);

        // output
        Utility.infoOpen(clusterFile);
        Utility.createDirectoryForFile(clusterFile);
        ScoredFacet.output(facets, clusterFile);
        Utility.infoWritten(clusterFile);
    }

    @Override
    public void close() throws IOException {
    }

    public static class Lda {

        final Pattern tokenPattern = Pattern.compile("[^\\|]+");

        long iterNum;
        long topTermNum;
        InstanceList instances; // documents (candidate lists) data

        public Lda(List<CandidateList> clists, long iterNum, long topTermNum) {
            this.iterNum = iterNum;
            this.topTermNum = topTermNum;
            loadData(clists);
        }

        private List<ScoredFacet> cluster(int topicNum) throws IOException {
            double alpha = 50.0;
            double beta = 0.01;
            ParallelTopicModel topicModel;
            topicModel = new ParallelTopicModel(topicNum, alpha, beta);
            topicModel.addInstances(instances);
            topicModel.setNumIterations((int) iterNum);
            topicModel.setSymmetricAlpha(false);
            topicModel.setTopicDisplay(100, 7);

            /**
             * This is the default setting for OptimizeInterval in mallet
             * train-topics. Specified here again b/c it is different from
             * ParallelTopicModel constructor.
             */
            topicModel.setOptimizeInterval(0);
            topicModel.estimate();

            // output
            ArrayList<ScoredFacet> facets = new ArrayList<>();

            for (int topic = 0; topic < topicModel.numTopics; topic++) {

                List<ScoredItem> items = new ArrayList<>();
                double weightSum = 0;
                for (int type = 0; type < topicModel.numTypes; type++) {

                    int[] topicCounts = topicModel.typeTopicCounts[type];

                    double weight = beta;

                    int index = 0;
                    while (index < topicCounts.length
                            && topicCounts[index] > 0) {

                        int currentTopic = topicCounts[index] & topicModel.topicMask;

                        if (currentTopic == topic) {
                            weight += topicCounts[index] >> topicModel.topicBits;
                            break;
                        }

                        index++;
                    }

                    String item = topicModel.alphabet.lookupObject(type).toString();
                    items.add(new ScoredItem(item, weight));
                    weightSum += weight;
                }

                // normalization for P(word|topic)
                for (ScoredItem item : items) {
                    item.score /= weightSum;
                }
                Collections.sort(items);
                items = items.subList(0, Math.min(items.size(), (int) topTermNum));

                // unnormalizated  P(a word from topic)
                double score = weightSum;

                facets.add(new ScoredFacet(items, score));
            }

            double allWeightSum = 0;
            for (ScoredFacet f : facets) {
                allWeightSum += f.score;
            }
            for (ScoredFacet f : facets) {
                f.score /= allWeightSum;
            }
            Collections.sort(facets);

            return facets;
        }

        private void loadData(List<CandidateList> clists) {
            ArrayList<Pipe> pipeList = new ArrayList<>();
            // Add the tokenizer
            pipeList.add(new CharSequence2TokenSequence(tokenPattern));
            // --keep-sequence
            pipeList.add(new TokenSequence2FeatureSequence());

            Pipe instancePipe = new SerialPipes(pipeList);

            instances = new InstanceList(instancePipe);

            instances.addThruPipe(new CandidateListIterator(clists));

        }

        static class CandidateListIterator implements Iterator<Instance> {

            Iterator<CandidateList> clistIterator;
            int clistNum;

            private CandidateListIterator(List<CandidateList> clists) {
                clistIterator = clists.iterator();
                clistNum = 0;
            }

            @Override
            public boolean hasNext() {
                return clistIterator.hasNext();
            }

            @Override
            public Instance next() {
                // next clist
                CandidateList clist = clistIterator.next();
                clistNum++;

                // construct instance
                String data = clist.itemList;
                String uri = "clistnum:" + clistNum;

                return new Instance(data, null, uri, null);
            }

            @Override
            public void remove() {
                throw new IllegalStateException("This Iterator<Instance> does not support remove().");
            }
        }

    }
}
