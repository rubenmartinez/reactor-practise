package net.rubenmartinez.cbcc.domain;

import lombok.Value;

@Value
public class ConnectionLogLine {

    private final long timestamp;
    private final String sourceHost;
    private final String targetHost;
}