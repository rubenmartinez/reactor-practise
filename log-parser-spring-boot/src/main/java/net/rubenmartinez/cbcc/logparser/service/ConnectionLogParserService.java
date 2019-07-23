package net.rubenmartinez.cbcc.logparser.service;

import net.rubenmartinez.cbcc.exception.LogFileIOException;
import net.rubenmartinez.cbcc.exception.LogFileParsingException;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface ConnectionLogParserService {

    Stream<String> streamConnectionsTo(Path logFile, String host, long initTimestamp, long endTimestamp) throws LogFileIOException, LogFileParsingException;

    Flux<String> connectionsTo(Path logFile, String host, long initTimestamp, long endTimestamp) throws LogFileIOException, LogFileParsingException;
}
