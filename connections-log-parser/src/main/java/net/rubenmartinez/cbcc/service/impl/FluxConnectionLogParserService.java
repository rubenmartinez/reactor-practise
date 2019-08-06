package net.rubenmartinez.cbcc.service.impl;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.exception.LogFileIOException;
import net.rubenmartinez.cbcc.logparsing.components.LogLineParser;
import net.rubenmartinez.cbcc.reactive.file.FileFlux;
import net.rubenmartinez.cbcc.service.ConnectionLogParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import java.nio.file.Path;

@Service("sequential")
public class FluxConnectionLogParserService extends BaseConnectionLogParserService implements ConnectionLogParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxConnectionLogParserService.class);

    @Inject
    private LogLineParser lineParser;

    public Flux<ConnectionLogLine> getConnectionsToHost(Path logFile, long fromPosition, String host, long initTimestamp, long endTimestamp) throws LogFileIOException {
        LOGGER.debug("getConnectionsToHost({}, {}, {}, {}, {})", logFile, fromPosition, host, initTimestamp, endTimestamp);

        long adjustedEndTimestamp = getAdjustedEndTimestamp(endTimestamp);
        LOGGER.debug("getConnectionsToHost: adjustedEndTimestamp= {}", adjustedEndTimestamp);

        return FileFlux.lines(logFile)
                .map(lineParser::parseLine)
                .onErrorContinue((exception, line) -> LOGGER.warn("Ignoring line: {} (error: {})", line, exception.getMessage()))
                .takeWhile(connection -> connection.getTimestamp() <= adjustedEndTimestamp)
                .filter(connection -> filterConnection(connection, host, initTimestamp, endTimestamp));
    }

}
