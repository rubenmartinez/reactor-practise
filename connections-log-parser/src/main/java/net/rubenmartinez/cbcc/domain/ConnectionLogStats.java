package net.rubenmartinez.cbcc.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Data
@JsonInclude(Include.NON_EMPTY)
public class ConnectionLogStats {

    private LocalDateTime startTime;
    private Duration duration;

    private Optional<String> targetHost;
    private Optional<List<String>> connectedToTargetHost;

    private Optional<String> sourceHost;
    private Optional<List<String>> connectedFromSourceHost;

    private List<String> sourceHostListWithMostConnections;
}
