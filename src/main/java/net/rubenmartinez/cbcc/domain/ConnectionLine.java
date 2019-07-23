package net.rubenmartinez.cbcc.domain;

import lombok.Data;

@Data
public class ConnectionLine {

    private long timestamp;
    private String sourceHost;
    private String targetHost;
}
