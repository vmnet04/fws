/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clustering;

import edu.umass.ciir.fws.demo.search.GalagoSearchEngine;
import edu.umass.ciir.fws.demo.search.SearchEngine;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class FacetRefinerFactory {

    public static FacetRefiner instance(Parameters p, SearchEngine searchEngine) {
        String facetModel = p.getString("facetModel");
        GalagoSearchEngine galago;
        if (searchEngine instanceof GalagoSearchEngine) {
            galago = (GalagoSearchEngine) searchEngine;
        } else {
            galago = new GalagoSearchEngine(p);
        }
        
        if (facetModel.equals("gmj") || facetModel.equals("gmi")) {
            return new GmFacetRefiner(p, galago);
        } else if (facetModel.equals("qd")){
            return new QDFacetRefiner(p, galago);
        } else {
            return null;
        }
    }

}
