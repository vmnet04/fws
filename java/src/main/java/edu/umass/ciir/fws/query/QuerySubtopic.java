/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.query;

import org.lemurproject.galago.tupleflow.Parameters;

/**
 *
 * @author wkong
 */
public class QuerySubtopic {

    public String description;
    public String sid;
    public String type;

    public QuerySubtopic(String sid, String description, String type) {
        this.sid = sid;
        this.description = description;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s\t%s\t%s", sid, description.replaceAll("\\s+", " "), type);
    }
    
    public Parameters toParameters() {
        Parameters subtopic = new Parameters();
        subtopic.put("number", sid);
        subtopic.put("type", type);
        subtopic.put("description", description);
        return subtopic;
    }

}
