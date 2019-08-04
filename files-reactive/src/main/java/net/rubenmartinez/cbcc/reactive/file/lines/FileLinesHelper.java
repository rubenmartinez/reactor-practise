package net.rubenmartinez.cbcc.reactive.file.lines;

import net.rubenmartinez.cbcc.reactive.file.exception.FileFluxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Internal class, non-exposed in module
 *
 */
public class FileLinesHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileLinesHelper.class);

    private static final int DEFAULT_AVERAGE_LINE_SIZE_HINT = 80;
    private static final String RANDOM_ACCESS_FILE_READONLY_MODE = "r";

    /**
     * Just an utility class with private constructor like {@link java.nio.file.Files}, moreover this is an internal package (non-exposed in module)
     */
    private FileLinesHelper() {
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

    public static FilePositionRange[] getSplitPositionsAtLineBoundaries(Path path, int splits) {
        if (splits < 1) {
            throw new IllegalArgumentException("splits must be greater than zero");
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), RANDOM_ACCESS_FILE_READONLY_MODE)) {

            var filePositionRanges = new FilePositionRange[splits];

            long endPosition = randomAccessFile.length();
            long initialSplitPosition = endPosition / splits;

            long fromPosition = 0;
            long toPosition = initialSplitPosition;
            for (int i=0; i<splits; i++) {

                long adjustedToPosition = positionToBeginningOfNextLine(randomAccessFile, toPosition);
                filePositionRanges[i] = new FilePositionRange(fromPosition, adjustedToPosition);

                fromPosition = adjustedToPosition;
                toPosition += initialSplitPosition;
            }

            // Ensure the final range ends at the end of the file
            filePositionRanges[splits-1].setToPosition(endPosition);

            return filePositionRanges;

        } catch (IOException e) {
            throw new FileFluxException(String.format("IOException while getSplitPositionsAtLineBoundaries(%s, %s)", path, splits), e);
        }
    }

    private static long positionToBeginningOfNextLine(RandomAccessFile randomAccessFile, long position) throws IOException {
        randomAccessFile.seek(position);
        randomAccessFile.readLine();
        return randomAccessFile.getFilePointer();

        // TODO Consider use buffering with af fileChannel instead of a RandomAccessFile, something like;
        // var bufferedReader = new BufferedReader(Channels.newReader(fileChannel, charset));
        // String currentLine = bufferedReader.readLine();
        // int newPosition = originalPosition + currentLine.length();
        // ... And now consume possible extra newline character,
    }
}