package com.appfire.jpi.controllers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.appfire.jpi.entities.Issue;
import com.appfire.jpi.entities.IssueList;

@RestController
public class JiraApiController {
        @Value("${jpi.jira.endpoint}")
        private String jiraEndpoint;
        @Value("${jpi.issues.jql}")
        private String jqlQuery;
        @Value("${jpi.issues.fields}")
        private String fields;
        @Value("${jpi.issues.batchsize}")
        private int batchSize;

        @Autowired
        private RestTemplate restTemplate;

        @GetMapping("/issues")
        public IssueList getAllIssuesInBatch(@RequestParam Integer batchId)
                        throws InterruptedException, ExecutionException, IOException {
                return getAllIssuesInRange(batchId * batchSize, batchSize);
        }

        /**
         * Request all issues in a given range. Based on the jira docs:
         * https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-issue-search/#api-rest-api-2-search-get
         * maxResults does not grant that all available issues will be delivered. If we have many fields defined we could
         * get less results than the expected.
         * 
         * We repeat the fetch issues request until we get all issues within the given range recursively.
         * Each time we decrease the range based on the amount of already delivered issues.
         * @param startAt
         * @param length
         * @return
         * @throws InterruptedException
         * @throws ExecutionException
         * @throws IOException
         */
        private IssueList getAllIssuesInRange(int startAt, int length)
                        throws InterruptedException, ExecutionException, IOException {
                final IssueList issueList = fetchIssues(startAt, length);

                final List<Issue> issues = issueList.getIssueList();
                final int numIssues = issues.size();

                final boolean hasMissingIssuesEventually = numIssues < length && numIssues > 0;
                
                if (hasMissingIssuesEventually) {
                        issues.addAll(getAllIssuesInRange(startAt + numIssues, length - numIssues)
                                        .getIssueList());
                }

                return issueList;
        }

        private IssueList fetchIssues(int startAt, int maxResults) {
                String url = String.format(
                                "%s/search?jql=%s&maxResults=%d&startAt=%d&fields=%s",
                                jiraEndpoint, jqlQuery, maxResults, startAt, fields);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json; charset=UTF-8");

                ResponseEntity<IssueList> responseEntity = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                IssueList.class);

                return responseEntity.getBody();
        }
}
