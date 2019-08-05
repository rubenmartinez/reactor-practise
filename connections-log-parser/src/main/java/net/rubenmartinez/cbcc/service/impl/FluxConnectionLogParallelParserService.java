package net.rubenmartinez.cbcc.service.impl;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.exception.LogFileIOException;
import net.rubenmartinez.cbcc.logparsing.components.LogLineParser;
import net.rubenmartinez.cbcc.params.Options;
import net.rubenmartinez.cbcc.reactive.file.FileFlux;
import net.rubenmartinez.cbcc.service.ConnectionLogParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import java.nio.file.Path;

@Service("parallel")
public class FluxConnectionLogParallelParserService implements ConnectionLogParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxConnectionLogParallelParserService.class);

    @Inject private Options options;
    @Inject private LogLineParser lineParser;

    public Flux<ConnectionLogLine> getConnectionsToHost(Path logFile, String host, long initTimestamp, long endTimestamp) throws LogFileIOException {
        LOGGER.debug("getConnectionsToHost({}, {}, {}, {})", logFile, host, initTimestamp, endTimestamp);

        long adjustedInitTimestamp = initTimestamp - options.getTimestampOrderToleranceMillis();
        long adjustedEndTimestamp = endTimestamp + options.getTimestampOrderToleranceMillis();

        LOGGER.debug("getConnectionsToHost: Adjusted timestamps: {} -> {}", adjustedInitTimestamp, adjustedEndTimestamp);

        Flux<String>[] allPartsFluxArray = FileFlux.splitFileLines(logFile, options.getSplits());

        // Just playing with some schedulers configuration, but probably I haven't found the best one yet
        Scheduler scheduler = Schedulers.newParallel("FluxConnectionLogParallelParserService");

        Scheduler scheduler2 = Schedulers.newParallel("FluxConnectionLogParallelParserService-otro");

        return Flux.fromArray(allPartsFluxArray)
            .flatMap(filePartFlux -> filePartFlux
                    .subscribeOn(scheduler)
                    .publishOn(scheduler2)
                    .filter(line -> !line.isEmpty())
                    .map(lineParser::parseLine)
                    .onErrorContinue((e, line) -> LOGGER.warn("Ignoring erroneous line: {} (error: {})", line, e.getMessage()))
                    .takeWhile(connection -> connection.getTimestamp() <= adjustedEndTimestamp)
                    .filter(connection -> connection.getTimestamp() >= adjustedInitTimestamp && connection.getTargetHost().equals(host)))
            .doOnComplete(() -> { scheduler.dispose(); scheduler2.dispose(); } );

    }

}
