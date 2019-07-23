package net.rubenmartinez.cbcc.logparser.components;

import net.rubenmartinez.cbcc.logparser.service.impl.domain.LogLine;
import net.rubenmartinez.cbcc.exception.LogFileParsingException;
import org.springframework.stereotype.Component;

@Component
public interface LogLineParser {

    LogLine parseLine(String line) throws LogFileParsingException;
}
