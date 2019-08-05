package net.rubenmartinez.cbcc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.rubenmartinez.cbcc.domain.ConnectionLogStats;
import net.rubenmartinez.cbcc.exception.LogParserException;
import net.rubenmartinez.cbcc.service.ConnectionLogStatsFormatterService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class JsonConnectionLogStatsFormatterService implements ConnectionLogStatsFormatterService {

    private static final String SEPARATOR = "--------------------------------------------------\n";

    @Inject
    private ObjectMapper mapper;

    @Override
    public String format(ConnectionLogStats connectionLogStats) {
        try {
            return SEPARATOR + mapper.writeValueAsString(connectionLogStats) + SEPARATOR;
        } catch (Exception e) {
            throw new LogParserException("Exception while convertion log stats to json: " + connectionLogStats, e);
        }
    }
}
