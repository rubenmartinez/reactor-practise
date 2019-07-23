package net.rubenmartinez.cbcc.logparser.service.impl.domain;

import lombok.Value;

@Value
public class LogLine {

    private final long timestamp;
    private final String sourceHost;
    private final String targetHost;
}