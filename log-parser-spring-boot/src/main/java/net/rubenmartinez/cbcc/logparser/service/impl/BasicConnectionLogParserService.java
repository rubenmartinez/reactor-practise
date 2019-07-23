package net.rubenmartinez.cbcc.logparser.service.impl;

import net.rubenmartinez.cbcc.config.Config;
import net.rubenmartinez.cbcc.domain.ConnectionLogStats;
import net.rubenmartinez.cbcc.exception.LogFileIOException;
import net.rubenmartinez.cbcc.logparser.components.LogLineParser;
import net.rubenmartinez.cbcc.logparser.service.ConnectionLogParserService;
import net.rubenmartinez.cbcc.logparser.service.impl.domain.LogLine;
import net.rubenmartinez.cbcc.reactive.file.FileFlux;
import net.rubenmartinez.cbcc.reactive.file.FileFlux.Mode;
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
import java.util.stream.Stream;

@Service
public class BasicConnectionLogParserService implements ConnectionLogParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicConnectionLogParserService.class);

    @Inject
    private Config config;

    @Inject
    private LogLineParser lineParser;

    // XXX
    public Set<String> uniqueConnectionsTo(Path logFile, String host, long initTimestamp, long endTimestamp) throws LogFileIOException {
        long adjustedInitTimestamp = initTimestamp - config.getTimestampOrderTolerance();
        long adjustedEndTimestamp = endTimestamp + config.getTimestampOrderTolerance();

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

    public Stream<String> streamConnectionsTo(Path logFile, String host, long initTimestamp, long endTimestamp) throws LogFileIOException {
        long adjustedInitTimestamp = initTimestamp - config.getTimestampOrderTolerance();
        long adjustedEndTimestamp = endTimestamp + config.getTimestampOrderTolerance();

        LOGGER.debug("Adjusted timestamps: {} -> {}", adjustedInitTimestamp, adjustedEndTimestamp);

        try {
            return Files.lines(logFile)
                    .map(lineParser::parseLine)
                    .takeWhile(connection -> connection.getTimestamp() <= adjustedEndTimestamp)
                    .filter(connection -> connection.getTimestamp() >= adjustedInitTimestamp && connection.getTargetHost().equals(host))
                    .map(connection -> connection.getSourceHost());
        } catch (IOException e) {
            throw new LogFileIOException("Error while reading logfile: " + logFile, e);
        }
    }

    public Flux<String> connectionsTo(Path logFile, String host, long initTimestamp, long endTimestamp) throws LogFileIOException {
        long adjustedInitTimestamp = initTimestamp - config.getTimestampOrderTolerance();
        long adjustedEndTimestamp = endTimestamp + config.getTimestampOrderTolerance();

        LOGGER.debug("Adjusted timestamps: {} -> {}", adjustedInitTimestamp, adjustedEndTimestamp);

        try {
            return FileFlux.lines(logFile, Mode.READ_FROM_BEGINNING)
                    .map(lineParser::parseLine)
                    .takeWhile(connection -> connection.getTimestamp() <= adjustedEndTimestamp)
                    .filter(connection -> connection.getTimestamp() >= adjustedInitTimestamp && connection.getTargetHost().equals(host))
                    .map(connection -> connection.getSourceHost());
        } catch (IOException e) {
            throw new LogFileIOException("Error while reading logfile into a Flux. logFile: " + logFile, e);
        }
    }

    public Flux<ConnectionLogStats> collectStats(Path logFile, Optional<String> sourceHost, Optional<String> targetHost, Duration windowDuration) throws LogFileIOException {
        try {
            return FileFlux.lines(logFile, Mode.FOLLOW_UPDATES_FROM_END)
                    .map(lineParser::parseLine)
                    .window(windowDuration)
                    .flatMapSequential(windowFlux -> createMonoOfStatsFromFluxOfLogLines(windowFlux, sourceHost, targetHost, windowDuration))
                    .map(ConnectionLogStatsContainer::getConnectionLogStats);
        } catch (IOException e) {
            throw new LogFileIOException("Error while reading (following) logfile into a Flux. logFile: " + logFile, e);
        }
    }


    private Mono<ConnectionLogStatsContainer> createMonoOfStatsFromFluxOfLogLines(Flux<LogLine> logLinesFlux, Optional<String> sourceHost, Optional<String> targetHost, Duration windowDuration) {
        var newStatsContainer = new ConnectionLogStatsContainer(LocalDateTime.now(), windowDuration, sourceHost, targetHost);
        return logLinesFlux.collect(() -> newStatsContainer, (theStatsContainer, logLine) -> theStatsContainer.accept(logLine));
    }
}
