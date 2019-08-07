package net.rubenmartinez.cbcc.reactive.file.lines;

import net.rubenmartinez.cbcc.reactive.file.exception.FileFluxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Internal class, non-exposed in module
 *
 */
public class FileLinesHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileLinesHelper.class);

    private static final String RANDOM_ACCESS_FILE_READONLY_MODE = "r";

    /**
     * Just an utility class with private constructor such as {@link java.nio.file.Files}, moreover this is an internal package (non-exposed in module)
     */
    private FileLinesHelper() {
    }

    public static FilePositionRange[] getSplitPositionsAtLineBoundaries(Path path, int splits) {
        return getSplitPositionsAtLineBoundaries(path, splits, 0);
    }

    public static FilePositionRange[] getSplitPositionsAtLineBoundaries(Path path, int splits, long startPosition) {
        return getSplitPositionsAtLineBoundaries(path, splits, startPosition, Long.MAX_VALUE);
    }

    /**
     *
     * As the positions returned must strictly match line boundaries, that means that the length of the returned array must necessarily match the {@code splits} argument.
     * eg. when splits=4, but the file only has a line then the returned array will contain only one element, with a position range from zero to the total length of the file
     *
     * @param path
     * @param splits
     * @return
     */
    public static FilePositionRange[] getSplitPositionsAtLineBoundaries(Path path, int splits, long startPosition, long endPosition) {
        LOGGER.trace("getSplitPositionsAtLineBoundaries({}, {})", path, splits);
        if (splits < 1) {
            throw new IllegalArgumentException("splits must be greater than zero");
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), RANDOM_ACCESS_FILE_READONLY_MODE)) {

            var filePositionRangeList = new ArrayList<FilePositionRange>(splits); // Initial size estimation, but final size could be shrunk (see internal javadoc above)

            if (endPosition == Long.MAX_VALUE) {
                endPosition = randomAccessFile.length();
            }

            LOGGER.trace("getSplitPositionsAtLineBoundaries: endPosition={}", endPosition);
            long initialSplitPosition = endPosition / splits;
            LOGGER.trace("getSplitPositionsAtLineBoundaries: initialSplitPosition={}", initialSplitPosition);

            long fromPosition = startPosition;
            long toPosition = initialSplitPosition;
            for (int i=0; i<splits; i++) {
                long adjustedToPosition = positionToBeginningOfNextLine(randomAccessFile, toPosition);

                filePositionRangeList.add(new FilePositionRange(fromPosition, adjustedToPosition));

                LOGGER.trace("getSplitPositionsAtLineBoundaries: iteration={}, fromPosition={}, toPosition={}, adjustedToPosition={}", i, fromPosition, toPosition, adjustedToPosition);
                if (adjustedToPosition >= endPosition) {
                    break;
                }

                fromPosition = adjustedToPosition;
                toPosition = Math.max(toPosition + initialSplitPosition, fromPosition);
            }
            ensureLastRangeCoversEndPosition(filePositionRangeList, endPosition); // This could happen if there are empty lines at the end of the file for example

            return filePositionRangeList.toArray(new FilePositionRange[filePositionRangeList.size()]);

        } catch (IOException e) {
            throw new FileFluxException(String.format("IOException while getSplitPositionsAtLineBoundaries(%s, %s)", path, splits), e);
        }
    }

    private static void ensureLastRangeCoversEndPosition(ArrayList<FilePositionRange> filePositionRanges, long endPosition) {
        filePositionRanges.get(filePositionRanges.size()-1).setToPosition(endPosition);
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