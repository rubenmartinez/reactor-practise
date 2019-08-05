package net.rubenmartinez.cbcc.service;

import net.rubenmartinez.cbcc.domain.ConnectionLogStats;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public interface ConnectionLogWatcherService {

    Flux<ConnectionLogStats> collectStats(Path logFile, Optional<String> sourceHost, Optional<String> targetHost, Duration windowDuration);


}
