package net.rubenmartinez.cbcc.service.impl;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.params.Options;
import net.rubenmartinez.cbcc.service.ConnectionLogParserService;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import java.nio.file.Path;

public abstract class BaseConnectionLogParserService implements ConnectionLogParserService {

    @Inject private Options options;

    protected long getAdjustedEndTimestamp(long endTimestamp) {
        return endTimestamp + options.getTimestampOrderToleranceMillis();
    }

    protected boolean filterConnection(ConnectionLogLine connection, String host, long initTimestamp, long endTimestamp) {
        return connection.getTimestamp() >= initTimestamp &&
                connection.getTimestamp() <= endTimestamp &&
                connection.getTargetHost().equals(host);
    }

    public abstract Flux<ConnectionLogLine> getConnectionsToHost(Path logFile, long fromPosition, String host, long initTimestamp, long endTimestamp);
}
