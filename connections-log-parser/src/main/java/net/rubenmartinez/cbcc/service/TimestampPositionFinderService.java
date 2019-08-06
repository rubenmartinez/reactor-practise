package net.rubenmartinez.cbcc.service;

import java.nio.file.Path;

public interface TimestampPositionFinderService {


    long findNearTimestamp(long timestamp, Path logFile);
}
