package net.rubenmartinez.cbcc.params;

import lombok.Data;
import net.rubenmartinez.cbcc.exception.UserInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Command line options
 */
@Data
@Configuration
public class Options {

    private static final long DEFAULT_TIMESTAMP_ORDER_TOLERANCE_MILLIS = 1000 * 60 * 5;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    @Value("${logFile:Instructions/input-file-100010000.txt}") // TODO XXX Remove default
    private String logFile;

    @Value("${initTimestamp}")
    private Long initTimestamp;

    @Value("${endTimestamp}")
    private Long endTimestamp;

    @Value("${initDateTime}")
    private String initDateTime;

    @Value("${endDateTime}")
    private String endDateTime;

    @Value("${sourceHost}")
    private String sourceHost;

    @Value("${targetHost}")
    private String targetHost;

    @Value("${uniqueHosts:false}")
    private boolean uniqueHosts;

    @Value("${splits:0}")
    private int splits;

    @Value("${stream:1}") // XXX
    private int stream;

    @Value("${debug:false}")
    private boolean debug;

    @Value("${timestampOrderToleranceMillis:"+DEFAULT_TIMESTAMP_ORDER_TOLERANCE_MILLIS+"}")
    private long timestampOrderToleranceMillis;


    public long getInitTimestamp() {
        return initTimestamp != null ? initTimestamp : getTimestampFromDateTime(initDateTime);
    }


    public long getEndTimestamp() {
        return endTimestamp != null ? endTimestamp : getTimestampFromDateTime(endDateTime);
    }

    private static final long getTimestampFromDateTime(String dateTime) {
        if (dateTime == null) {
            throw new UserInputException("Please use ISO-8601 format for datetime options, eg. 2011-12-03T10:15:30");
        }

        return ZonedDateTime.parse(dateTime, dateTimeFormatter).toInstant().getEpochSecond();
    }
}
