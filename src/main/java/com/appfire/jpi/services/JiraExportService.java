package com.appfire.jpi.services;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.appfire.jpi.entities.Issue;
import com.appfire.jpi.entities.IssueComment;
import com.appfire.jpi.entities.IssueList;
import com.appfire.jpi.entities.mixin.IssueCommentMixin;
import com.appfire.jpi.entities.mixin.IssueMixin;
import com.appfire.jpi.utils.XmlUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;

@Service
public class JiraExportService {
    Logger logger = LogManager.getLogger(JiraExportService.class);

    public static enum FileType {
        JSON, XML
    }

    @Value("${jpi.issues.para}")
    private int numParallelBatches;

    /*
     * Write this object to the exported file in case there are no Isses available.
     */
    private static class Empty {
    }

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    @Qualifier("xml")
    private ObjectMapper xmlMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ExecutorService task;

    public void export(FileType fileType, OutputStream out)
            throws IOException, InterruptedException, ExecutionException {
        final SequenceWriter writer = getSequenceWriter(fileType, out);

        final boolean isXmlPatchNeeded = FileType.XML.equals(fileType);

        if (isXmlPatchNeeded) {
            XmlUtils.writeXmlVersion(out);
            XmlUtils.writeRootStart(out);
            // When exporting to xml some key values need to be in CDATA.
            xmlMapper.addMixIn(Issue.class, IssueMixin.class);
            xmlMapper.addMixIn(IssueComment.class, IssueCommentMixin.class);
        }

        writeIssues(writer);

        if (isXmlPatchNeeded) {
            XmlUtils.writeRootEnd(out);
        }

        writer.close();
    }

    private ObjectMapper getObjectMapper(FileType fileType) {
        if (FileType.XML.equals(fileType)) {
            return xmlMapper;
        }
        return jsonMapper;
    }

    private SequenceWriter getSequenceWriter(FileType fileType, OutputStream out) throws IOException {
        ObjectWriter writer = getObjectMapper(fileType)
                .writer()
                .withDefaultPrettyPrinter();
        return writer
                .writeValues(out)
                .init(true);
    }

    private void writeIssues(SequenceWriter writer) throws IOException, InterruptedException, ExecutionException {
        int chunkId = 0;
        Long totalNumIssues = 0L;

        while (true) {
            logger.info("-- chunk --");

            // Get the next batch with issues.
            final IssueList issues = getIssuesChunk(chunkId);
            final List<Issue> issueList = issues.getIssueList();

            // Return if there are no issues.
            if (issueList.isEmpty()) {
                logger.info("-- close --");
                break;
            }

            // Write all issues to the stream.
            writer.writeAll(issueList);

            final int numIssues = issueList.size();
            chunkId++;
            totalNumIssues += numIssues;

            logger.info(String.format("-- end(%d) --", numIssues));

            // If we hit the last chunk of issues return.
            if (totalNumIssues.equals(issues.getTotal())) {
                logger.info("-- close --");
                break;
            }
        }

        // Pass Empty object to the stream in case there are no Issues.
        // If we close the writer stream without flushing anything jackson throws
        // exception: JsonGenerationException: No open start element, when trying
        // to write end element. This does not stand for the case with a json export.
        if (totalNumIssues == 0L) {
            writer.write(new Empty());
        }
    }

    private IssueList getIssuesBatch(int batchId) {
        final String url = "http://localhost:8080/issues";
        return restTemplate.getForObject(String.format("%s?batchId=%d", url, batchId),
                IssueList.class);
    }

    /**
     * If numParallelBatches := 3 then chunkId comprises of 3 batches with ids:
     * 3 * chunkId, 3 * chunkId + 1 and 3 * chunkId + 2.
     * chunkId -> Batches
     * 0 -> 0, 1, 2
     * 1 -> 3, 4, 5
     * 2 -> 6, 7, 8
     * 
     * @param chunkId
     * @param numParallelBatches
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private IssueList getIssuesChunk(int chunkId)
            throws InterruptedException, ExecutionException {
        final IssueList result = new IssueList();

        List<Future<IssueList>> futuresIssueList = new ArrayList<>();

        for (int i = 0; i < numParallelBatches; i++) {
            final int batchId = numParallelBatches * chunkId + i;
            logger.info("Process batch " + batchId);
            Future<IssueList> future = task.submit(() -> getIssuesBatch(batchId));
            futuresIssueList.add(future);
        }

        for (Future<IssueList> future : futuresIssueList) {
            IssueList issueList = future.get();
            // Set the total number of issues in the query from the server response to the
            // result.
            if (result.getTotal() == null) {
                result.setTotal(issueList.getTotal());
            }
            logger.info(String.format("Batch finish with %d issues.", issueList.getIssueList().size()));
            result.getIssueList().addAll(issueList.getIssueList());
        }

        return result;
    }
}
