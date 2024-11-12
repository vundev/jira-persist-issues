package com.appfire.jpi.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueComment {
    @Getter
    @Setter
    private String author;

    @Getter
    @Setter
    private String text;
}
