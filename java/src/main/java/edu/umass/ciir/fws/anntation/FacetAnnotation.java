/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.anntation;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.umass.ciir.fws.clustering.ScoredItem;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.lemurproject.galago.tupleflow.Parameters;

/**
 * Facets Created for one query by annotator
 *
 * @author wkong
 */
public class FacetAnnotation {

    public String annotatorID;
    public String qid;
    public ArrayList<AnnotatedFacet> facets;

    public FacetAnnotation(String annotatorID, String queryID) {
        this.annotatorID = annotatorID;
        this.qid = queryID;
        facets = new ArrayList<>();
    }

    public void addFacet(AnnotatedFacet facet) {
        this.facets.add(facet);
    }

    public String listAsString() {
        StringBuilder lists = new StringBuilder();
        for (AnnotatedFacet f : facets) {
            lists.append(String.format("%s\t%s\t%s\n", this.annotatorID, this.qid, f.toString()));
        }
        return lists.toString();
    }

    public static FacetAnnotation parseFromJson(String jsonDataString) throws IOException {
        return parseFromJson(jsonDataString, true);
    }

    public static FacetAnnotation parseFromJson(String jsonDataString, boolean filter) throws IOException {
        Parameters data = Parameters.parseString(jsonDataString);
        String annotatorID = data.getString("annotatorID");
        String queryID = data.getString("aolUserID");
        Parameters facetMap = data.getMap("explicitlySaved");

        if (filter && annotatorID.startsWith("test-")) {
            return null;
        }
        FacetAnnotation fa = new FacetAnnotation(annotatorID, queryID);

        // to sort ids
        ArrayList<Integer> facetIds = new ArrayList<>();
        for (String fid : facetMap.getKeys()) {
            facetIds.add(Integer.parseInt(fid));
        }
        Collections.sort(facetIds);

        for (Integer fid : facetIds) {
            Parameters facet = facetMap.getMap(fid.toString());
            int rating = Integer.parseInt(facet.getString("rating"));
            String description = facet.getString("description").replaceAll("\\s+", " ");

            AnnotatedFacet f = new AnnotatedFacet(rating, fid.toString(), description);

            // to sort ids
            ArrayList<Integer> itemIds = new ArrayList<>();
            for (String tid : facet.getKeys()) {
                if (!tid.equals("rating") && !tid.equals("description")) {
                    itemIds.add(Integer.parseInt(tid));
                }
            }
            Collections.sort(itemIds);

            for (Integer itemId : itemIds) {
                String item = facet.getMap(itemId.toString()).getAsList("queries", Parameters.class).get(0).getString("query");
                f.addItem(new ScoredItem(item, 0));
            }

            if (f.size() > 0) {
                fa.addFacet(f);
            }
        }

        return fa;
    }

    public static List<FacetAnnotation> load(File jsonFile) throws IOException {
        ArrayList<FacetAnnotation> annotations = new ArrayList<>();
        BufferedReader reader = Utility.getReader(jsonFile);
        String line;
        while ((line = reader.readLine()) != null) {
            FacetAnnotation facetAnnotation = FacetAnnotation.parseFromJson(line);
            if (facetAnnotation != null) {
                annotations.add(facetAnnotation);
            }
        }
        reader.close();
        return annotations;
    }

    public static HashMap<String, FacetAnnotation> loadAsMap(File jsonFile) throws IOException {
        HashMap<String, FacetAnnotation> map = new HashMap<>();
        List<FacetAnnotation> annotation = load(jsonFile);
        for (FacetAnnotation f : annotation) {
            map.put(f.qid, f);
        }
        return map;
    }
}