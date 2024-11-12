package com.appfire.jpi.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueList {
    @JsonProperty("issues")
    @Getter
    @Setter
    private List<Issue> issueList;

    @Getter
    @Setter
    private Long total;

    public IssueList(List<Issue> issueList) {
        this.issueList = issueList;
    }

    public IssueList() {
        issueList = new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.join("\n", issueList.stream()
                .map(Issue::toString)
                .collect(Collectors.toList()));
    }
}
