/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A more richer representation for a candidate list than
 * edu.umass.ciir.fws.types.CandidateList generated by Tupleflow-type-builder.
 *
 * @author wkong
 */
public class CandidateList {

    public String qid;
    public long docRank;
    public String listType;
    public String itemList;
    public String[] items; // candidate list items
    public final static int MAX_TERM_SIZE = 10; // maxium number of word in an candidate item/a facet term

    public CandidateList() {

    }

    public CandidateList(String qid, long docRank, String listType, List<String> items) {
        this.qid = qid;
        this.docRank = docRank;
        this.listType = listType;
        this.items = items.toArray(new String[0]);
        this.itemList = joinItemList(items);
    }

    public CandidateList(String qid, long docRank, String listType, String itemList) {
        this.qid = qid;
        this.docRank = docRank;
        this.listType = listType;
        this.items = splitItemList(itemList);
        this.itemList = itemList;
    }

    public boolean valid() {
        if (items.length > 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * if the candidate list is html type (extracted based on html patterns).
     *
     * @return
     */
    public boolean isHtmlType() {
        return !isTextType();
    }

    public boolean isTextType() {
        return listType.equals(CandidateListTextExtractor.type);
    }

    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%s", qid, docRank, listType, itemList);
    }

    public static void output(List<CandidateList> clists, File file) throws IOException {
        BufferedWriter writer = Utility.getWriter(file);
        for (CandidateList clist : clists) {
            writer.write(clist.toString() + "\n");
        }
        writer.close();
    }

    public static List<CandidateList> loadCandidateLists(File clistFile) throws IOException {
        ArrayList<CandidateList> clist = new ArrayList<>();
        String[] lines = Utility.readFileToString(clistFile).split("\n");
        for (String line : lines) {
            String[] fields = line.split("\t");
            String qid = fields[0];
            long docRank = Long.parseLong(fields[1]);
            String listType = fields[2];
            String itemList = fields[3];
            clist.add(new CandidateList(qid, docRank, listType, itemList));
        }
        return clist;
    }

    public static List<CandidateList> loadCandidateLists(File clistFile, long topNum) throws IOException {
        ArrayList<CandidateList> clist = new ArrayList<>();
        String[] lines = Utility.readFileToString(clistFile).split("\n");
        for (String line : lines) {
            String[] fields = line.split("\t");
            String qid = fields[0];
            long docRank = Long.parseLong(fields[1]);
            String listType = fields[2];
            String itemList = fields[3];
            if (docRank <= topNum) {
                clist.add(new CandidateList(qid, docRank, listType, itemList));
            }
        }
        return clist;
    }

    public static String[] splitItemList(String itemList) {
        return itemList.split("\\|");
    }

    public static String joinItemList(String[] items) {
        return Utility.join(items, "|");
    }

    public static String joinItemList(List<String> items) {
        return Utility.join(items, "|");
    }

    /**
     * if the candidate list is extracted based on html patterns.
     *
     * @param candidateList
     * @return
     */
    public static boolean isHtmlCandidateList(edu.umass.ciir.fws.types.CandidateList candidateList) {
        return !candidateList.listType.equals(CandidateListTextExtractor.type);
    }
}
