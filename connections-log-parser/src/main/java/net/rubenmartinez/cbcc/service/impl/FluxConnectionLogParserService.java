package net.rubenmartinez.cbcc.service.impl;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.domain.ConnectionLogStats;
import net.rubenmartinez.cbcc.exception.LogFileIOException;
import net.rubenmartinez.cbcc.logparser.components.LogLineParser;
import net.rubenmartinez.cbcc.logwatcher.components.ConnectionLogStatsContainer;
import net.rubenmartinez.cbcc.params.Options;
import net.rubenmartinez.cbcc.reactive.file.FileFlux;
import net.rubenmartinez.cbcc.service.ConnectionLogParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FluxConnectionLogParserService implements ConnectionLogParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxConnectionLogParserService.class);

    @Inject
    private Options options;

    @Inject
    private LogLineParser lineParser;

    // XXX
    public Set<String> uniqueConnectionsTo(Path logFile, String host, long initTimestamp, long endTimestamp) throws LogFileIOException {
        long adjustedInitTimestamp = initTimestamp - options.getTimestampOrderToleranceMillis();
        long adjustedEndTimestamp = endTimestamp + options.getTimestampOrderToleranceMillis();

        LOGGER.debug("Adjusted timestamps: {} -> {}", adjustedInitTimestamp, adjustedEndTimestamp);

        try {
            return Files.lines(logFile)
                    .peek(line -> LOGGER.trace("Processing line: {}", line))
                    .filter(line -> !line.isEmpty())
                    .map(lineParser::parseLine)
                    .takeWhile(connection -> connection.getTimestamp() <= adjustedEndTimestamp)
                    .filter(connection -> connection.getTimestamp() >= adjustedInitTimestamp && connection.getTargetHost().equals(host))
                    .map(connection -> connection.getSourceHost())
                    .collect(Collectors.toUnmodifiableSet());



        } catch (IOException e) {
            throw new LogFileIOException("Error while reading logfile: " + logFile, e);
        }
    }

    public Flux<ConnectionLogLine> getConnectionsToHost(Path logFile, String host, long initTimestamp, long endTimestamp) throws LogFileIOException {
        long adjustedInitTimestamp = initTimestamp - options.getTimestampOrderToleranceMillis();
        long adjustedEndTimestamp = endTimestamp + options.getTimestampOrderToleranceMillis();

        LOGGER.debug("Adjusted timestamps: {} -> {}", adjustedInitTimestamp, adjustedEndTimestamp);

        try {
            return FileFlux.lines(logFile)
                    .map(lineParser::parseLine)
                    .takeWhile(connection -> connection.getTimestamp() <= adjustedEndTimestamp)
                    .filter(connection -> connection.getTimestamp() >= adjustedInitTimestamp && connection.getTargetHost().equals(host));
        } catch (IOException e) {
            throw new LogFileIOException("Error while reading logfile into a Flux. logFile: " + logFile, e);
        }
    }

    public Flux<ConnectionLogStats> collectStats(Path logFile, Optional<String> sourceHost, Optional<String> targetHost, Duration windowDuration) {
        return FileFlux.follow(logFile, true)
                .map(lineParser::parseLine)
                .window(windowDuration)
                .flatMapSequential(windowFlux -> createMonoOfStatsFromFluxOfLogLines(windowFlux, sourceHost, targetHost, windowDuration))
                .map(ConnectionLogStatsContainer::getConnectionLogStats);
    }

    private Mono<ConnectionLogStatsContainer> createMonoOfStatsFromFluxOfLogLines(Flux<ConnectionLogLine> logLinesFlux, Optional<String> sourceHost, Optional<String> targetHost, Duration windowDuration) {
        var newStatsContainer = new ConnectionLogStatsContainer(LocalDateTime.now(), windowDuration, sourceHost, targetHost);
        return logLinesFlux.collect(() -> newStatsContainer, (theStatsContainer, logLine) -> theStatsContainer.accept(logLine));
    }
}
