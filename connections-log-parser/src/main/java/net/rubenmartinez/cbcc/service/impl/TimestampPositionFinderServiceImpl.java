package net.rubenmartinez.cbcc.service.impl;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.exception.LogParserException;
import net.rubenmartinez.cbcc.logparsing.components.LogLineParser;
import net.rubenmartinez.cbcc.service.TimestampPositionFinderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Predicate;

@Service
public class TimestampPositionFinderServiceImpl implements TimestampPositionFinderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimestampPositionFinderServiceImpl.class);

    private static final Charset CHARSET = StandardCharsets.US_ASCII; // TODO Configurable

    private static final int MINIMUM_FRAGMENT_SIZE = 8096;
    private static final int BUFFER_FIND_NEWLINE_SIZE = 8096;

    private static final int MAXIMUM_LOOPS_BEFORE_ERROR = 1000;

    @Inject LogLineParser logLineParser;

    /**
     * Tries to find a line (actually the byte position that starts at a line) closer to a given timestamp in a log file than the beginning of the file
     *
     * Note the position returned could be at a line termination, so the client must be able to ignore empty lines at the returned position
     *
     * Note also that even if the timestamp exist in the file this method might not find it and report a lower position. This is just for efficiency to
     * avoid scaning the full file. The position returned will be near the given timestamp but it is not ensured to be the nearest.
     *
     * What it is ensured:
     * - Position returned won't be at the middle of a line
     * - Position returned will point to a timestamp that will be equals or less the given timestamp (and it should be quite close to the given timestamp)
     *
     * @return A lower bound position hint for the given timestamp
     */
    @Override
    public long findNearTimestamp(long timestamp, Path logFile) {

        try (var fileChannel = FileChannel.open(logFile, StandardOpenOption.READ)) {

            long fileLength = fileChannel.size();

            long lowerBoundPosition = 0;
            long upperBoundPosition = fileLength;
            long currentPosition;

            int loops = 0;

            while (((upperBoundPosition - lowerBoundPosition) > MINIMUM_FRAGMENT_SIZE) ) {

                // Just a binary search
                currentPosition = (upperBoundPosition - lowerBoundPosition) / 2 + lowerBoundPosition;
                fileChannel.position(currentPosition);

                var line = getNextLineNonEmpty(fileChannel);
                LOGGER.trace("[{}, {}]. Line at position [{}]: {}", lowerBoundPosition, upperBoundPosition, currentPosition, line);
                if (line == null) {
                    break;
                }

                ConnectionLogLine connectionLog = logLineParser.parseLine(line);
                LOGGER.trace("Timestamp comparision: {}", connectionLog.getTimestamp()- timestamp);

                if (connectionLog.getTimestamp() >= timestamp) {
                    upperBoundPosition = currentPosition;
                }
                else {
                    lowerBoundPosition = currentPosition;
                }

                if (loops++ > MAXIMUM_LOOPS_BEFORE_ERROR) { // Hey, this is an experimental feature after all
                    throw new LogParserException(String.format("Aborting experimental feature. Possibly infinite loop while finding nearest timestamp to [%s] in file: %s", timestamp, logFile));
                }
            }

            return nextLineBoundaryPosition(fileChannel, lowerBoundPosition);

        } catch (IOException e) {
            throw new LogParserException(String.format("Error while finding nearest timestamp to [%s] in file: %s", timestamp, logFile), e);
        }
    }

    private static String getNextLineNonEmpty(FileChannel fileChannel) throws IOException {
        var bufferedReader = new BufferedReader(Channels.newReader(fileChannel, CHARSET));
        bufferedReader.readLine(); // Ignoring a (likely) non-complete line;

        String nextLine;
        do {
            nextLine = bufferedReader.readLine();
        } while (nextLine != null && nextLine.isEmpty());

        return nextLine;
    }

    private static long nextLineBoundaryPosition(FileChannel fileChannel, long position) throws IOException {
        if (position == 0) {
            return 0;
        }

        fileChannel.position(position);

        var byteBuffer = ByteBuffer.allocate(BUFFER_FIND_NEWLINE_SIZE);
        fileChannel.read(byteBuffer);
        byteBuffer.flip();

        // Find next new line
        consumeCharactersWhile(byteBuffer, b -> b!='\n' && b!='\r');
        // Consume new line to get next line's position
        consumeCharactersWhile(byteBuffer, b -> b=='\n' || b=='\r');

        return position + byteBuffer.position() - 1;

    }

    private static void consumeCharactersWhile(ByteBuffer byteBuffer, Predicate<Byte> predicate) {
        byte b;
        do {
            if (!byteBuffer.hasRemaining()) {
                throw new LogParserException("Error finding timestamp, reached end of buffer, this might indicate a bug or that there are lines greater than " + BUFFER_FIND_NEWLINE_SIZE + " bytes");
            }

            b = byteBuffer.get();
            if (b == -1) {
                throw new LogParserException("Error finding timestamp, reached end of file");
            }
        } while (predicate.test(b));
    }


}
