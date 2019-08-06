package net.rubenmartinez.cbcc.params;

import lombok.Data;
import net.rubenmartinez.cbcc.exception.UserInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Command line options
 */
@Data
@Configuration
public class Options {

    private static final long DEFAULT_TIMESTAMP_ORDER_TOLERANCE_MILLIS = 1000 * 60 * 5;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;

    @Value("${initTimestamp:#{null}}")
    private Long initTimestamp;

    @Value("${endTimestamp:#{null}}")
    private Long endTimestamp;

    @Value("${initDateTime:#{null}}")
    private String initDateTime;

    @Value("${endDateTime:#{null}}")
    private String endDateTime;

    @Value("${sourceHost:#{null}}")
    private Optional<String> sourceHost;

    @Value("${targetHost:#{null}}")
    private Optional<String> targetHost;

    @Value("${uniqueHosts:false}")
    private boolean uniqueHosts;

    @Value("${statsWindow:PT1H}")
    private String statsWindow;

    @Value("${presearchTimestamp:false}")
    private boolean presearchTimestamp;

    @Value("${splits:0}")
    private int splits;

    @Value("${timestampOrderToleranceMillis:"+DEFAULT_TIMESTAMP_ORDER_TOLERANCE_MILLIS+"}")
    private long timestampOrderToleranceMillis;


    public Duration getStatsWindowDuration() {
        try {
            return Duration.parse(getStatsWindow());
        } catch (Exception e) {
            throw new UserInputException("Please use a ISO-8601 duration format (eg. \"PT1H\" for 1 hour)");

        }
    }

    public Long getInitTimestamp() {
        return initTimestamp != null ? initTimestamp : getTimestampFromDateTime(initDateTime);
    }

    public Long getEndTimestamp() {
        return endTimestamp != null ? endTimestamp : getTimestampFromDateTime(endDateTime);
    }

    private static final Long getTimestampFromDateTime(String dateTime) {
        if (dateTime == null) {
            return null;
        }

        try {
            return ZonedDateTime.parse(dateTime, dateTimeFormatter).toInstant().getEpochSecond();
        } catch (Exception e) {
            throw new UserInputException("Please use ISO-8601 format for datetime options, eg. 2011-12-03T10:15:30");
        }
    }
}
