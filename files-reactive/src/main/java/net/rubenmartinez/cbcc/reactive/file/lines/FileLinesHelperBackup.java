package net.rubenmartinez.cbcc.reactive.file.lines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

/**
 * Internal class, non-exposed in module
 *
 */
public class FileLinesHelperBackup {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileLinesHelperBackup.class);

    private static final int DEFAULT_AVERAGE_LINE_SIZE_HINT = 80;

    /**
     * Just an utility class with private constructor like {@link java.nio.file.Files}, moreover this is an internal package (non-exposed in module)
     */
    private FileLinesHelperBackup() {
    }

    /**
     */
    public static String getEntireLineAtCurrentPosition(FileChannel fileChannel, String charset) throws IOException {
        return getEntireLineAtCurrentPosition(fileChannel, charset, DEFAULT_AVERAGE_LINE_SIZE_HINT);
    }

    /**
     */
    public static String getEntireLineAtCurrentPosition(FileChannel fileChannel, String charset, int averageLineSizeHint) throws IOException {

        int lookBehindBytes = averageLineSizeHint;

        String lineAtCurrentPosition;
        while ( (lineAtCurrentPosition = getLineAtCurrentPositionUsingMemoryBuffer(fileChannel, lookBehindBytes, charset)) == null) {
            lookBehindBytes *= 2;
            LOGGER.debug("getEntireLineAtCurrentPosition: doubling lookBehindBytes to: {}", lookBehindBytes);
        }

        return lineAtCurrentPosition;
    }

    /**
     */
    private static String getLineAtCurrentPositionUsingMemoryBuffer(FileChannel fileChannel, int lookBehindBytes, String charset) throws IOException, EOFException {
        long originalPosition = fileChannel.position();
        // We could read byte by byte backwards till we find '\n', '\r' or '\r\n', but it would be inefficient without buffering
        long startingPosition = Math.max(0, originalPosition - lookBehindBytes);
        LOGGER.debug("getLineAtCurrentPositionUsingMemoryBuffer: originalPosition={}, startingPosition={}", originalPosition, startingPosition);


        fileChannel.position(startingPosition);
        var bufferedReader = new BufferedReader(Channels.newReader(fileChannel, charset));

        String previousLineCandidate = bufferedReader.readLine();
        LOGGER.debug("getLineAtCurrentPositionUsingMemoryBuffer: previousLineCandidate={}", previousLineCandidate);

        if (previousLineCandidate == null) {
            throw new EOFException(String.format("Unexpected EOF at getLineAtCurrentPositionUsingMemoryBuffer(%s, %s) - previousLineCandidate", fileChannel, lookBehindBytes));
        }

        if (fileChannel.position() > originalPosition) {
            if (startingPosition == 0) {
                return previousLineCandidate;
            }
            else {
                LOGGER.debug("Looking behind [{}] bytes from original position [{}] and reading a line, the new position already passed the original position, so it is not certain if the line was read completely, try with a bigger 'lookBehindBytes', (potentially partial)line read was: {}", lookBehindBytes, originalPosition, previousLineCandidate);
                return null;
            }
        }

        String lineCandidate;
        long currentPosition;
        do {
            lineCandidate = bufferedReader.readLine();
            currentPosition = fileChannel.position();
            LOGGER.debug("getLineAtCurrentPositionUsingMemoryBuffer: current position={}; lineCandidate read in iteration={}", currentPosition, lineCandidate);
        } while (false);

        if (lineCandidate == null) {
            throw new EOFException(String.format("Unexpected EOF at getLineAtCurrentPositionUsingMemoryBuffer(%s, %s) - lineCandidate", fileChannel, lookBehindBytes));
        }

        return lineCandidate;
    }
}