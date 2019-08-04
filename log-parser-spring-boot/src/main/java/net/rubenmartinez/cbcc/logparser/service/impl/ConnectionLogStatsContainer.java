package net.rubenmartinez.cbcc.logparser.service.impl;

import net.rubenmartinez.cbcc.domain.ConnectionLogStats;
import net.rubenmartinez.cbcc.logparser.service.impl.domain.LogLine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * package-level class to collect stats about log lines during a period of time
 *
 * Note that startTime and duration fields are just informative, they are not involved in any logic
 *
 */
class ConnectionLogStatsContainer implements Consumer<LogLine> {
    private ConnectionLogStats connectionLogStats;

    private LocalDateTime startTime;
    private Duration duration;
    private Optional<String> sourceHost;
    private Optional<String> targetHost;

    private long maxNumberOfConnectionsFromASourceHost;
    private Map<String, Integer> numberOfConnectionsPerHost;

    ConnectionLogStatsContainer(LocalDateTime startTime, Duration duration, Optional<String> sourceHost, Optional<String> targetHost) {
        this.startTime = startTime;
        this.duration = duration;
        this.sourceHost = sourceHost;
        this.targetHost = targetHost;

        this.maxNumberOfConnectionsFromASourceHost = 0;
        this.numberOfConnectionsPerHost = new HashMap<>();

        initializeConnectionLogStats(startTime, duration, sourceHost, targetHost);

    }

    private void initializeConnectionLogStats(LocalDateTime startTime, Duration duration, Optional<String> sourceHost, Optional<String> targetHost) {
        connectionLogStats = new ConnectionLogStats();
        connectionLogStats.setStartTime(startTime);
        connectionLogStats.setDuration(duration);
        connectionLogStats.setSourceHost(sourceHost);
        if (sourceHost.isPresent()) {
            connectionLogStats.setConnectedFromSourceHost(Optional.of(new ArrayList<>()));
        }
        else {
            connectionLogStats.setConnectedFromSourceHost(Optional.empty());
        }

        connectionLogStats.setTargetHost(targetHost);
        if (targetHost.isPresent()) {
            connectionLogStats.setConnectedToTargetHost(Optional.of(new ArrayList<>()));
        }
        else {
            connectionLogStats.setConnectedToTargetHost(Optional.empty());
        }

        connectionLogStats.setSourceHostListWithMostConnections(new ArrayList<>());
    }

    @Override
    public void accept(LogLine logLine) {
        incrementNumberOfConnectionsPerHost(logLine.getSourceHost());
        addConnectionIfSourceHostMatches(logLine.getSourceHost(), logLine.getTargetHost());
        addConnectionIfTargetHostMatches(logLine.getSourceHost(), logLine.getTargetHost());
    }

    private void incrementNumberOfConnectionsPerHost(String logLineSourceHost) {
        Integer currentNumberOfConnections = numberOfConnectionsPerHost.getOrDefault(logLineSourceHost, 0);
        Integer newNumberOfConnections = currentNumberOfConnections + 1;
        numberOfConnectionsPerHost.put(logLineSourceHost, newNumberOfConnections);

        updateTopConnectionsSource(logLineSourceHost, newNumberOfConnections);
    }

    private void updateTopConnectionsSource(String logLineSourceHost, Integer newNumberOfConnections) {
        if (newNumberOfConnections == maxNumberOfConnectionsFromASourceHost) {
            connectionLogStats.getSourceHostListWithMostConnections().add(logLineSourceHost);
        }
        else if (newNumberOfConnections > maxNumberOfConnectionsFromASourceHost) {
            maxNumberOfConnectionsFromASourceHost = newNumberOfConnections;
            connectionLogStats.getSourceHostListWithMostConnections().clear();
            connectionLogStats.getSourceHostListWithMostConnections().add(logLineSourceHost);
        }
    }

    private void addConnectionIfSourceHostMatches(String logLineSourceHost, String logLineTargetHost) {
        if (this.sourceHost.isPresent() && this.sourceHost.get().equals(logLineSourceHost)) {
            connectionLogStats.getConnectedFromSourceHost().get().add(logLineTargetHost);
        }
    }

    private void addConnectionIfTargetHostMatches(String logLineSourceHost, String logLineTargetHost) {
        if (this.targetHost.isPresent() && this.targetHost.get().equals(logLineTargetHost)) {
            connectionLogStats.getConnectedToTargetHost().get().add(logLineSourceHost);
        }
    }

    public ConnectionLogStats getConnectionLogStats() {
        return connectionLogStats;
    }
}
