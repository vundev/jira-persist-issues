package com.appfire.jpi.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import com.appfire.jpi.configuration.TestsConfig;
import com.appfire.jpi.entities.Issue;
import com.appfire.jpi.entities.IssueList;
import com.appfire.jpi.services.JiraExportService.FileType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ContextConfiguration(classes = TestsConfig.class)
public class JiraExportServiceTest {
    @MockBean
    private RestTemplate restTemplate;

    private List<IssueList> batches;

    private final int BUFFER_SIZE = 1024 * 1024; // 1mb
    private final PipedInputStream in = new PipedInputStream(BUFFER_SIZE);
    private final PipedOutputStream out = new PipedOutputStream();
    private long issueId = 0L;

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    @Qualifier("xml")
    private ObjectMapper xmlMapper;

    @Autowired
    private JiraExportService service;

    @BeforeEach
    public void setup() throws IOException {
        initIssueBatches();
        in.connect(out);
    }

    @Test
    void isServiceCreated() throws Exception {
        assertThat(service).isNotNull();
    }

    @Test
    void testExportToJson() throws Exception {
        testExport(FileType.JSON);
    }

    @Test
    void testExportToXml() throws Exception {
        testExport(FileType.XML);
    }

    private void testExport(FileType fileType) throws IOException, InterruptedException, ExecutionException {
        final Iterator<IssueList> batchesIterator = batches.iterator();
        // All calls for batches are placed in the main thread.
        when(restTemplate.getForObject(anyString(), any()))
                .then(i -> {
                    if (batchesIterator.hasNext()) {
                        return batchesIterator.next();
                    }
                    // If there are no batches return an empty batch.
                    return new IssueList();
                });

        service.export(fileType, out);

        final List<Issue> issues = readIssuesFromOutStream(fileType);

        assertTrue(areIssuesEqual(getAllIssues(), issues));
    }

    private void initIssueBatches() {
        // Create as many batches as you want here, and add them to the batches array
        // to play with different xml, json file variations.
        // Increase the buffer size if the batch size becomes larger.
        final IssueList issuesBatch1 = new IssueList();
        issuesBatch1.setIssueList(Arrays.asList(
                createRandomIssue(),
                createRandomIssue()));
        final IssueList issuesBatch2 = new IssueList();
        issuesBatch2.setIssueList(Arrays.asList(
                createRandomIssue()));
        final IssueList issuesBatch3 = new IssueList();
        issuesBatch3.setIssueList(Arrays.asList(
                createRandomIssue(),
                createRandomIssue(),
                createRandomIssue(),
                createRandomIssue()));

        batches = Arrays.asList(
                issuesBatch1,
                issuesBatch2,
                issuesBatch3);
    }

    private List<Issue> getAllIssues() {
        return batches.stream().flatMap(batch -> batch.getIssueList().stream()).toList();
    }

    private Issue createRandomIssue() {
        final Issue issue = new Issue();
        issue.setKey(UUID.randomUUID().toString());
        issue.setId(issueId++);
        return issue;
    }

    private List<Issue> readIssuesFromOutStream(FileType fileType) throws IOException {
        final String issuesToString = readFromPipedInputStream(in);
        return getObjectMapper(fileType).readValue(issuesToString, new TypeReference<List<Issue>>() {
        });
    }

    private static String readFromPipedInputStream(PipedInputStream pipedInputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pipedInputStream))) {
            int ch;
            while ((ch = reader.read()) != -1) {
                stringBuilder.append((char) ch);
            }
        }
        return stringBuilder.toString();
    }

    private boolean areIssuesEqual(List<Issue> listA, List<Issue> listB) {
        if (listA.size() != listB.size()) {
            return false;
        }

        return IntStream.range(0, listA.size())
                .allMatch(idx -> listA.get(idx).getKey().equals(listB.get(idx).getKey()));
    }

    private ObjectMapper getObjectMapper(FileType fileType) {
        if (FileType.XML.equals(fileType)) {
            return xmlMapper;
        }
        return jsonMapper;
    }
}
