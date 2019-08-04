package net.rubenmartinez.cbcc.logparser.service.impl;

import net.rubenmartinez.cbcc.logparser.service.impl.domain.LogLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;


class ConnectionLogStatsContainerTest {

    private ConnectionLogStatsContainer statsContainer;

    @BeforeEach
    void before() {
        var startTime = LocalDateTime.now();
        var duration = Duration.of(1, ChronoUnit.HOURS);
        var sourceHost = Optional.of("configuredSourceHost");
        var targetHost = Optional.of("configuredTargetHost");

        statsContainer = new ConnectionLogStatsContainer(startTime, duration, sourceHost, targetHost);
    }

    @Test
    void testSourceHostListWithMostConnections() {
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source2", "target1"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source1", "target2"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source1", "target3"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source1", "target4"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source1", "target5"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source2", "target6"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source2", "target7"));

        var connectionLogStats = statsContainer.getConnectionLogStats();

        assertThat(connectionLogStats.getSourceHostListWithMostConnections(), contains("source1"));
    }

    @Test
    void testSourceHostListWithMostConnectionsTwoTops() {
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "sourceTop1", "target1"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "sourceTop2", "target2"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "sourceTop1", "target3"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "sourceTop2", "target4"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source1", "target5"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source2", "target6"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source3", "target7"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source4", "target7"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source5", "target7"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source6", "target7"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source7", "target7"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "sourceTop1", "target1"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "sourceTop2", "target2"));

        var connectionLogStats = statsContainer.getConnectionLogStats();

        assertThat(connectionLogStats.getSourceHostListWithMostConnections(), containsInAnyOrder("sourceTop1", "sourceTop2"));
    }

    @Test
    void testSourceHostListWithMostConnectionsAllOneConnection() {
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source1", "target1"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source2", "target2"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source3", "target3"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source4", "target4"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source5", "target5"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source6", "target6"));
        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source7", "target7"));

        var connectionLogStats = statsContainer.getConnectionLogStats();

        assertThat(connectionLogStats.getSourceHostListWithMostConnections(), containsInAnyOrder("source1", "source2", "source3", "source4", "source5", "source6", "source7"));
    }

    @Test
    void testOptionals() {
        var startTime = LocalDateTime.now();
        var duration = Duration.of(1, ChronoUnit.HOURS);

        var statsContainer = new ConnectionLogStatsContainer(startTime, duration, Optional.empty(), Optional.empty());

        statsContainer.accept(new LogLine(System.currentTimeMillis(), "source1", "target1"));

        var connectionLogStats = statsContainer.getConnectionLogStats();

        assertEquals(startTime, connectionLogStats.getStartTime());
        assertEquals(duration, connectionLogStats.getDuration());
        assertEquals(Optional.empty(), connectionLogStats.getSourceHost());
        assertEquals(Optional.empty(), connectionLogStats.getTargetHost());
        assertEquals(Optional.empty(), connectionLogStats.getConnectedToTargetHost());
        assertEquals(Optional.empty(), connectionLogStats.getConnectedFromSourceHost());
    }
}