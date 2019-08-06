package net.rubenmartinez.cbcc.service.impl;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.domain.ConnectionLogStats;
import net.rubenmartinez.cbcc.logparsing.components.LogLineParser;
import net.rubenmartinez.cbcc.logparsing.components.impl.ConnectionLogStatsContainer;
import net.rubenmartinez.cbcc.reactive.file.FileFlux;
import net.rubenmartinez.cbcc.service.ConnectionLogWatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FluxConnectionLogWatcherService implements ConnectionLogWatcherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxConnectionLogWatcherService.class);

    @Inject
    private LogLineParser lineParser;

    @Override
    public Flux<ConnectionLogStats> collectStats(Path logFile, Optional<String> sourceHost, Optional<String> targetHost, Duration windowDuration) {
        LOGGER.debug("collectStats(logFile={}, sourceHost={}, targetHost={}, windowDuration{})", logFile, sourceHost, targetHost, windowDuration);

        return FileFlux.follow(logFile, true)
                .map(lineParser::parseLine)
                .onErrorContinue((exception, line) -> LOGGER.warn("Ignoring line: {} (error: {})", line, exception.getMessage()))
                .window(windowDuration)
                .flatMapSequential(windowFlux -> collectStatsForWindow(windowFlux, sourceHost, targetHost, windowDuration))
                .map(ConnectionLogStatsContainer::getConnectionLogStats);
    }

    private Mono<ConnectionLogStatsContainer> collectStatsForWindow(Flux<ConnectionLogLine> logLinesFlux, Optional<String> sourceHost, Optional<String> targetHost, Duration windowDuration) {
        var newStatsContainer = new ConnectionLogStatsContainer(LocalDateTime.now(), windowDuration, sourceHost, targetHost);
        return logLinesFlux.collect(() -> newStatsContainer, (theStatsContainer, logLine) -> theStatsContainer.accept(logLine));
    }
}
