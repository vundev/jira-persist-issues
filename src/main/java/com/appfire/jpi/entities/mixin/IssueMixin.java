package com.appfire.jpi.entities.mixin;

import com.appfire.jpi.utils.CDataTextSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;

public abstract class IssueMixin {
    @JsonSerialize(using = CDataTextSerializer.class)
    @JacksonXmlCData
    private String description;

    @JsonSerialize(using = CDataTextSerializer.class)
    @JacksonXmlCData
    private String summary;
}
