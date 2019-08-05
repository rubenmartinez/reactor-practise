package net.rubenmartinez.cbcc.service;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.exception.LogFileIOException;
import net.rubenmartinez.cbcc.exception.LogFileParsingException;
import reactor.core.publisher.Flux;

import java.nio.file.Path;

public interface ConnectionLogParserService {

    Flux<ConnectionLogLine> getConnectionsToHost(Path logFile, String host, long initTimestamp, long endTimestamp) throws LogFileIOException, LogFileParsingException;
}
