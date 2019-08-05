package net.rubenmartinez.cbcc.logparsing.components.impl;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.domain.ConnectionLogStats;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * package-level class to collect stats about log lines during a period of time
 *
 * Note that startTime and duration fields are just informative, they are not involved in any logic
 *
 */
public class ConnectionLogStatsContainer implements Consumer<ConnectionLogLine> {
    private ConnectionLogStats connectionLogStats;

    private LocalDateTime startTime;
    private Duration duration;
    private Optional<String> sourceHost;
    private Optional<String> targetHost;

    private Map<String, Integer> numberOfConnectionsPerHost;

    public ConnectionLogStatsContainer(LocalDateTime startTime, Duration duration, Optional<String> sourceHost, Optional<String> targetHost) {
        this.startTime = startTime;
        this.duration = duration;
        this.sourceHost = sourceHost;
        this.targetHost = targetHost;

        this.numberOfConnectionsPerHost = new HashMap<>();

        initializeConnectionLogStats(startTime, duration, sourceHost, targetHost);

    }

    private void initializeConnectionLogStats(LocalDateTime startTime, Duration duration, Optional<String> sourceHost, Optional<String> targetHost) {
        connectionLogStats = new ConnectionLogStats();
        connectionLogStats.setStartTime(startTime);
        connectionLogStats.setDuration(duration);
        connectionLogStats.setSourceHost(sourceHost);
        if (sourceHost.isPresent()) {
            connectionLogStats.setConnectedFromSourceHostInWindow(Optional.of(new HashSet<>()));
        }
        else {
            connectionLogStats.setConnectedFromSourceHostInWindow(Optional.empty());
        }

        connectionLogStats.setTargetHost(targetHost);
        if (targetHost.isPresent()) {
            connectionLogStats.setConnectedToTargetHostInWindow(Optional.of(new HashSet<>()));
        }
        else {
            connectionLogStats.setConnectedToTargetHostInWindow(Optional.empty());
        }

        connectionLogStats.getSourceHostsWithMostConnectionsInWindow().setList(new HashSet<>());
        connectionLogStats.getSourceHostsWithMostConnectionsInWindow().setNumberOfConnections(0);
    }

    @Override
    public void accept(ConnectionLogLine logLine) {
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
        var sourceHostsWithMostConnections = connectionLogStats.getSourceHostsWithMostConnectionsInWindow();

        if (newNumberOfConnections == sourceHostsWithMostConnections.getNumberOfConnections()) {
            sourceHostsWithMostConnections.getList().add(logLineSourceHost);
        }
        else if (newNumberOfConnections > sourceHostsWithMostConnections.getNumberOfConnections()) {
            sourceHostsWithMostConnections.setNumberOfConnections(newNumberOfConnections);
            sourceHostsWithMostConnections.getList().clear();
            sourceHostsWithMostConnections.getList().add(logLineSourceHost);
        }
    }

    private void addConnectionIfSourceHostMatches(String logLineSourceHost, String logLineTargetHost) {
        if (this.sourceHost.isPresent() && this.sourceHost.get().equals(logLineSourceHost)) {
            connectionLogStats.getConnectedFromSourceHostInWindow().get().add(logLineTargetHost);
        }
    }

    private void addConnectionIfTargetHostMatches(String logLineSourceHost, String logLineTargetHost) {
        if (this.targetHost.isPresent() && this.targetHost.get().equals(logLineTargetHost)) {
            connectionLogStats.getConnectedToTargetHostInWindow().get().add(logLineSourceHost);
        }
    }

    public ConnectionLogStats getConnectionLogStats() {
        return connectionLogStats;
    }
}
