package net.rubenmartinez.cbcc.logparser.components;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.exception.LogFileParsingException;
import org.springframework.stereotype.Component;

@Component
public interface LogLineParser {

    ConnectionLogLine parseLine(String line) throws LogFileParsingException;
}
