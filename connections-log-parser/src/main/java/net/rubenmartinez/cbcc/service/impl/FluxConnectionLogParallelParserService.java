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
public class FluxConnectionLogParallelParserService extends BaseConnectionLogParserService implements ConnectionLogParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxConnectionLogParallelParserService.class);

    @Inject private Options options;
    @Inject private LogLineParser lineParser;

    public Flux<ConnectionLogLine> getConnectionsToHost(Path logFile, long fromPosition, String host, long initTimestamp, long endTimestamp) throws LogFileIOException {
        LOGGER.debug("getConnectionsToHost({}, {}, {}, {}, {})", logFile, fromPosition, host, initTimestamp, endTimestamp);

        long adjustedEndTimestamp = getAdjustedEndTimestamp(endTimestamp);
        LOGGER.debug("getConnectionsToHost: adjustedEndTimestamp= {}", adjustedEndTimestamp);

        Flux<String>[] allPartsFluxArray = FileFlux.splitFileLines(logFile, options.getSplits());

        // Just playing with some schedulers configuration, but I haven't found a good one yet
        Scheduler scheduler = Schedulers.newParallel("FluxConnectionLogParallelParserService");

        return Flux.fromArray(allPartsFluxArray)
            .flatMap(filePartFlux -> filePartFlux
                    .subscribeOn(scheduler)
                    .map(lineParser::parseLine)
                    .onErrorContinue((exception, line) -> LOGGER.warn("Ignoring line: {} (error: {})", line, exception.getMessage()))
                    .takeWhile(connection -> connection.getTimestamp() <= adjustedEndTimestamp)
                    .filter(connection -> filterConnection(connection, host, initTimestamp, endTimestamp)))
            .doOnComplete(() -> scheduler.dispose());
    }

}
