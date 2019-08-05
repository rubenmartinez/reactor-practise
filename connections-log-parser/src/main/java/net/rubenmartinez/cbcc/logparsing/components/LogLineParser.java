package net.rubenmartinez.cbcc.logparsing.components;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.exception.LogParserException;
import org.springframework.stereotype.Component;

@Component
public interface LogLineParser {

    ConnectionLogLine parseLine(String line) throws LogParserException;
}
