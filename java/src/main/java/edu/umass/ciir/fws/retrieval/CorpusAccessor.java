/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.umass.ciir.fws.retrieval;

import java.io.IOException;
import org.lemurproject.galago.core.parse.Document;

/**
 *
 * @author wkong
 */
public interface CorpusAccessor {
    String getSystemName();

    Document getHtmlDocument(String name) throws IOException;

    String getParsedDocumentFilename(String name);
    
    String getClistFileName(String docName, String suffix);
    
    void close() throws IOException;
}
