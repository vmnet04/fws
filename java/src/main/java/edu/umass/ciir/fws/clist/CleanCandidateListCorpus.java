package edu.umass.ciir.fws.clist;

import edu.umass.ciir.fws.types.TfCandidateList;
import edu.umass.ciir.fws.utility.Utility;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.lemurproject.galago.core.tools.AppFunction;
import org.lemurproject.galago.tupleflow.FileSource;
import org.lemurproject.galago.tupleflow.InputClass;
import org.lemurproject.galago.tupleflow.OutputClass;
import org.lemurproject.galago.tupleflow.Parameters;
import org.lemurproject.galago.tupleflow.StandardStep;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.tupleflow.execution.ConnectionAssignmentType;
import org.lemurproject.galago.tupleflow.execution.InputStep;
import org.lemurproject.galago.tupleflow.execution.Job;
import org.lemurproject.galago.tupleflow.execution.OutputStep;
import org.lemurproject.galago.tupleflow.execution.Stage;
import org.lemurproject.galago.tupleflow.execution.Step;
import org.lemurproject.galago.tupleflow.execution.Verified;
import org.lemurproject.galago.tupleflow.types.FileName;

/**
 * Tupleflow application for clean corpus candidate list.
 *
 * @author wkong
 */
public class CleanCandidateListCorpus extends AppFunction {

    private static final String name = "clean-clist-corpus";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHelpString() {
        return "fws " + name + " [parameters...]\n"
                + AppFunction.getTupleFlowParameterString();
    }

    @Override
    public void run(Parameters p, PrintStream output) throws Exception {
        Job job = createJob(p);
        AppFunction.runTupleFlowJob(job, p, output);
    }

    private Job createJob(Parameters parameters) {
        Job job = new Job();

        job.add(getSplitStage(parameters));
        job.add(getProcessStage(parameters));
        job.add(getWriteStage(parameters));

        job.connect("split", "process", ConnectionAssignmentType.Each);
        job.connect("process", "write", ConnectionAssignmentType.Combined);

        return job;
    }

    private Stage getSplitStage(Parameters parameter) {
        Stage stage = new Stage("split");

        stage.addOutput("fileNames", new FileName.FilenameOrder());

        List<String> inputFiles = parameter.getAsList("clistCorpusDir");

        Parameters p = new Parameters();
        p.set("input", new ArrayList());
        for (String input : inputFiles) {
            p.getList("input").add(new File(input).getAbsolutePath());
        }

        stage.add(new Step(FileSource.class, p));
        stage.add(Utility.getSorter(new FileName.FilenameOrder()));
        stage.add(new OutputStep("fileNames"));

        return stage;
    }

    private Stage getProcessStage(Parameters parameters) {
        Stage stage = new Stage("process");

        stage.addInput("fileNames", new FileName.FilenameOrder());
        stage.addOutput("clists", new TfCandidateList.QidDocRankDocNameListTypeItemListOrder());

        stage.add(new InputStep("fileNames"));
        stage.add(new Step(CandidateListCorpusCleaner.class, parameters));
        stage.add(Utility.getSorter(new TfCandidateList.QidDocRankDocNameListTypeItemListOrder()));
        stage.add(new OutputStep("clists"));
        return stage;
    }

    private Stage getWriteStage(Parameters parameters) {
        Stage stage = new Stage("write");
        stage.addInput("clists", new TfCandidateList.QidDocRankDocNameListTypeItemListOrder());

        stage.add(new InputStep("clists"));
        parameters.set("suffix", "clean.clist");
        stage.add(new Step(CandidateListCorpusWriter.class, parameters));
        return stage;
    }

    /**
     * clean corpus candidate list
     */
    @Verified
    @InputClass(className = "org.lemurproject.galago.tupleflow.types.FileName")
    @OutputClass(className = "edu.umass.ciir.fws.types.TfCandidateList")
    public static class CandidateListCorpusCleaner extends StandardStep<FileName, TfCandidateList> {

        CandidateListCleaner cleaner;

        public CandidateListCorpusCleaner(TupleFlowParameters parameters) throws Exception {
            Parameters p = parameters.getJSON();
            p.remove("stopwordsFile");
            cleaner = new CandidateListCleaner(p);
        }

        @Override
        public void process(FileName fileName) throws IOException {
            if (fileName.filename.endsWith(".clist.gz") && !fileName.filename.endsWith(".clean.clist.gz")) {
                BufferedReader reader = Utility.getReader(fileName.filename);
                while (true) {
                    CandidateList clist = CandidateList.readOne(reader);
                    if (clist == null) { // end of file
                        break;
                    } else {
                        // clean
                        CandidateList cleaned = cleaner.clean(clist);
                        if (cleaned != null) {
                            processor.process(cleaned.toTfCandidateList());
                        }
                    }
                }
                reader.close();
            }
        }
    }

}
