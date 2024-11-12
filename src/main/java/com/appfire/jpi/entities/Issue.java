package com.appfire.jpi.entities;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Issue {
    @Getter
    @Setter
    private long id;

    @Getter
    private String key;

    @Getter
    private String summary;

    @Getter
    private String description;

    @Getter
    private String reporterName;

    @Getter
    private String created;

    @Getter
    private String issueUrl;

    @Getter
    private JsonNode issuePriority;

    @Getter
    private JsonNode issueType;

    @Getter
    private List<IssueComment> comments;

    public Issue() {
    }

    public void setKey(String key) {
        this.key = key;
        this.issueUrl = String.format("https://jira.atlassian.com/browse/%s", key);
    }

    @JsonProperty("fields")
    private void unpackFields(JsonNode fields) {
        this.summary = fields.get("summary").asText();
        this.description = fields.get("description").asText();
        this.reporterName = fields.at("/reporter/displayName").asText();
        this.created = fields.get("created").asText();
        this.issuePriority = fields.get("priority");
        this.issueType = fields.get("issuetype");

        unpackComments(fields.at("/comment/comments"));
    }

    private void unpackComments(JsonNode comments) {
        this.comments = StreamSupport.stream(comments.spliterator(), false)
                .map(jsonNode -> {
                    final IssueComment comment = new IssueComment();
                    final String author = jsonNode.at("/author/displayName").asText();
                    final String text = jsonNode.get("body").asText();
                    comment.setAuthor(author);
                    comment.setText(text);
                    return comment;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("[Issue id=%s key%s]", id, key);
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null || !(arg0 instanceof Issue)) {
            return false;
        }
        return id == (((Issue) arg0).id);
    }
}
