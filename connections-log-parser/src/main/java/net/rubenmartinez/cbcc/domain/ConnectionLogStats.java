package net.rubenmartinez.cbcc.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Getter @Setter
@JsonInclude(Include.NON_EMPTY)
public class ConnectionLogStats {

    public ConnectionLogStats() {
        this.sourceHostsWithMostConnectionsInWindow = new SourceHostsWithMostConnections();
    }

    private LocalDateTime startTime;
    private Duration duration;

    private Optional<String> targetHost;
    private Optional<Set<String>> connectedToTargetHostInWindow;

    private Optional<String> sourceHost;
    private Optional<Set<String>> connectedFromSourceHostInWindow;

    private SourceHostsWithMostConnections sourceHostsWithMostConnectionsInWindow;

    @Getter @Setter
    public class SourceHostsWithMostConnections {
        private Set<String> list;
        private long numberOfConnections;
    }
}
