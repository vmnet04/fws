/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.eval;

import edu.umass.ciir.fws.anntation.AnnotatedFacet;
import edu.umass.ciir.fws.clustering.ScoredFacet;
import java.util.List;

/**
 * PRF_{alpha=1, beta=1}, and corresponding TP, TR, TF, PT, PR, PF
 * wPRF_{alpha=1, beta=1}
 * @author wkong
 */
public class PrfEqualEvaluator extends PrfNewEvaluator {

    private static final int metricNum = 14; // 2 weighting X (6+1) metrics
    static double[][] alphaBetas = new double[][]{
        {1.0, 1.0},
    };

    static TermWeighting[] weightings = new TermWeighting[]{TermWeighting.TermEqual, TermWeighting.TermRating};

    @Override
    public double[] eval(List<AnnotatedFacet> afacets, List<ScoredFacet> sfacets, int numTopFacets, String... params) {
        loadFacets(afacets, sfacets, numTopFacets);
        loadItemWeightMap();
        loadItemSets();
        loadSystemItemSet();
        cumulateTermWeights();
        cumulatePairWeights();

        double[] all = new double[metricNum];
        int i = 0;
        for (TermWeighting weighting : weightings) {
            double[] termPRF = termPrecisionRecallF1(weighting);
            double[] pairPRFOverlap = pairPrecisionRecallF1(weighting, true);

            i = append(all, i, termPRF);
            i = append(all, i, pairPRFOverlap);

            //System.err.println("tP\ttR\tpF\tPRF\talpha\tbeta");
            for (double[] alphaBeta : alphaBetas) {
                double prfOverlap = harmonicMean(termPRF[0], termPRF[1], pairPRFOverlap[2], alphaBeta[0], alphaBeta[1]);
                //System.err.println(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f",
                //        termPRF[0], termPRF[1], pairPRFOverlap[2], prfOverlap, alphaBeta[0], alphaBeta[1]));
                i = append(all, i, prfOverlap);
            }
        }

        return all;
    }

    private int append(double[] all, int start, double[] part) {
        for (double res : part) {
            all[start++] = res;
        }
        return start;
    }

    private int append(double[] all, int start, double one) {
        all[start++] = one;
        return start;
    }

    @Override
    public int metricNum() {
        return metricNum;
    }

}
