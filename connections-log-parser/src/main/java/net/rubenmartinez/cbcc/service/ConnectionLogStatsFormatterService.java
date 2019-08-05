package net.rubenmartinez.cbcc.service;

import net.rubenmartinez.cbcc.domain.ConnectionLogStats;

public interface ConnectionLogStatsFormatterService {

    String format(ConnectionLogStats connectionLogStats);
}
