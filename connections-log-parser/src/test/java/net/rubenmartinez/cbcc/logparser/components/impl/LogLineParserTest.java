package net.rubenmartinez.cbcc.logparser.components.impl;

import net.rubenmartinez.cbcc.Main;
import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.logparsing.components.LogLineParser;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(Main.class)
class LogLineParserTest {

    @Inject
    LogLineParser logLineParser;

    @Test
    public void emptyLine_thenError() {
        assertThrows(IllegalArgumentException.class, () -> logLineParser.parseLine(""));
    }

    @Test
    public void incorrectNumberOfItems_thenError() {
        assertThrows(IllegalArgumentException.class, () -> logLineParser.parseLine("1item"));
        assertThrows(IllegalArgumentException.class, () -> logLineParser.parseLine("2 items"));
        assertThrows(IllegalArgumentException.class, () -> logLineParser.parseLine("more than 3 items"));
    }

    @Test
    public void noTimestamp_thenError() {
        assertThrows(IllegalArgumentException.class, () -> logLineParser.parseLine("first second third"));
    }

    @Test
    public void correctLine() {
        long timestamp = System.currentTimeMillis();
        String source = "sourceHost";
        String target = "targetHost";

        String lineString = timestamp + " " + source + " " + target;

        ConnectionLogLine parsed = logLineParser.parseLine(lineString);

        assertEquals(timestamp, parsed.getTimestamp());
        assertEquals(source, parsed.getSourceHost());
        assertEquals(target, parsed.getTargetHost());
    }

}