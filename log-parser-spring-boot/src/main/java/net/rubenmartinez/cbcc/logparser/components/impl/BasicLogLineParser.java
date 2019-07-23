package net.rubenmartinez.cbcc.logparser.components.impl;

import net.rubenmartinez.cbcc.exception.LogFileParsingException;
import net.rubenmartinez.cbcc.logparser.components.LogLineParser;
import net.rubenmartinez.cbcc.logparser.service.impl.domain.LogLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BasicLogLineParser implements LogLineParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicLogLineParser.class);

    private static final String HOSTS_CONNECTION_LINE_SEPARATOR = " ";

    @Override
    public LogLine parseLine(String line) {
        String[] items = line.split(HOSTS_CONNECTION_LINE_SEPARATOR);

        if (items.length != 3) {
            throw new LogFileParsingException(String.format("Error while parsing host connections file, format must be strictly: <unix_timestamp>'%s'<sourceHost>'%s'<targetHost>, but the following line couldn't be parsed: %s", HOSTS_CONNECTION_LINE_SEPARATOR, HOSTS_CONNECTION_LINE_SEPARATOR, line));
        }

        long timestamp;
        try {
            timestamp = Long.valueOf(items[0]);
        } catch (NumberFormatException e) {
            throw new LogFileParsingException("Error while parsing host connections file line, first item must be a unix timestamp. Line: " + line, e);
        }

        var hostsConnectionLine = new LogLine(timestamp, items[1], items[2]);
        LOGGER.trace("Generated new HostsConnectionLine: {}", hostsConnectionLine);
        return hostsConnectionLine;
    }
}
