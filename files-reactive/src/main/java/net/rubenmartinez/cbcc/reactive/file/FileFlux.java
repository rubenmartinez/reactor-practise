package net.rubenmartinez.cbcc.reactive.file;

import net.rubenmartinez.cbcc.reactive.file.exception.FileFluxException;
import net.rubenmartinez.cbcc.reactive.file.lines.FileLinesHelper;
import net.rubenmartinez.cbcc.reactive.file.lines.PositionLimitedBufferedLineReader;
import net.rubenmartinez.cbcc.reactive.file.tailer.FluxEmittingTailerListener;
import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

/**
 * Utility class to read files in a reactive way.
 *
 * At the moment, it provides a method to get a {@link Flux} of the lines of a given file.
 *
 * In case you don't know already, the {@link Flux} is basically a {@code Publisher} that {@code Subscribers} can subscribe to in order
 * to read and process the lines of a file.
 *
 * You can think of Java 8 {@link Stream} of lines. Even if the fundamentals are quite different,
 * the API is somewhat similar in the sense that you have an stream of lines that "clients" can consume and process.
 *
 * Note that, currently, the only encoding supported is {@link StandardCharsets#US_ASCII}
 *
 * @see <a href="https://www.baeldung.com/reactor-core">Intro to reactor core</a>
 * @see <a href="https://spring.io/blog/2016/04/19/understanding-reactive-types">Understanding reactive types</a>
 */
public final class FileFlux {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileFlux.class);

    private static final Charset CHARSET = StandardCharsets.US_ASCII; // TODO Configurable
    private static final long POLLING_DELAY_MILLIS = 1000; // TODO Configurable

    /**
     * Just an utility class with private constructor like {@link java.nio.file.Files}, moreover this is an internal package (non-exposed in module)
     */
    private FileFlux() {
    }

    /**
     *
     * @param path
     * @return
     * @throws FileFluxException
     */
    public static Flux<String> lines(Path path) throws FileFluxException {
        return lines(path, 0L, Long.MAX_VALUE);
    }

    /**
     *
     * @param path
     * @param fromPosition
     * @return
     * @throws FileFluxException
     */
    public static Flux<String> lines(Path path, long fromPosition) throws FileFluxException {
        return lines(path, fromPosition, Long.MAX_VALUE);
    }

    /**
     *
     * If {@code fromPosition} is greather than 0, then the first line returned will be always the following line to the line
     * at the given position. This is true even if {@code fromPosition} points already to a beginning of a line in the middle of a file.
     *
     * In other words, a line or partial-line will be always skipped unless {@code fromPosition} is exactly 0
     *
     * @param path
     * @param fromPosition
     * @param toPosition
     * @return
     */
    public static Flux<String> lines(Path path, long fromPosition, long toPosition) {
        BufferedReader bufferedReader;

        try {
            if (fromPosition == 0) {
                bufferedReader = Files.newBufferedReader(path, CHARSET);
            }
            else {
                FileChannel fileChannel;
                fileChannel = FileChannel.open(path, StandardOpenOption.READ);
                fileChannel.position(fromPosition);

                if (toPosition < Long.MAX_VALUE) {
                    bufferedReader = new BufferedReader(Channels.newReader(fileChannel, CHARSET));
                }
                else {
                    long maxCharsToRead = toPosition - fromPosition;
                    bufferedReader = new PositionLimitedBufferedLineReader(Channels.newReader(fileChannel, CHARSET), maxCharsToRead);
                }
            }
        } catch (IOException e) {
            throw new FileFluxException(String.format("Error opening file [%s] [from:%s; to:%s] ", path, fromPosition, toPosition), e);
        }

        return Flux.create(fluxSink -> emitFileLinesToFluxSink(bufferedReader, fluxSink)); // Note that Flux won't call method emitFileLinesToFluxSink *until* some consumer subscribes to it
    }

    private static void emitFileLinesToFluxSink(BufferedReader bufferedReader, FluxSink<String> fluxSink) {
        fluxSink.onDispose(() -> uncheckedExceptionClose(bufferedReader));

        try {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                fluxSink.next(line);
            }
        } catch (IOException e) {
            fluxSink.error(e);
        } finally {
            uncheckedExceptionClose(bufferedReader);
        }

        fluxSink.complete();
    }

    /**
     * XXX
     * Lines splitting file for parallel processing
     *
     * @param path
     * @param splits
     * @return array of {@link Flux}, each Flux represent a 'stream' of lines for each split of the file
     */
    public static Flux<String>[] splitFileLines(Path path, int splits) {
        var positionRanges = FileLinesHelper.getSplitPositionsAtLineBoundaries(path, splits);
        var splitFileLinesFluxArray = new Flux[splits];

        for (int i=0; i<positionRanges.length; i++) {
            splitFileLinesFluxArray[i] = lines(path, positionRanges[i].getFromPosition(), positionRanges[i].getToPosition());
        }

        return splitFileLinesFluxArray;
    }

    /**
     * Using Apache Commons {@link Tailer} to do the work of polling the file at intervals to check if it has been updated
     * Tailer allows just to write a Listener ({@link org.apache.commons.io.input.TailerListener} implemented by {@link FluxEmittingTailerListener}
     *
     * That listener basically emits a new element (log line) in the Flux when it receives new lines from Tailer.
     */
    public static Flux<String> follow(Path path, boolean fromEnd) {
        File file = path.toFile();

        createFileIfDoesntExist(file);

        return Flux.push(emitter -> {
            var tailerListener = new FluxEmittingTailerListener(emitter);
            Tailer tailer = new Tailer(file, tailerListener, POLLING_DELAY_MILLIS, fromEnd); // TODO Configurable
            tailer.run();
        });
    }

    private static void createFileIfDoesntExist(File file) {
        try {
            if (!file.exists()) {
                file.createNewFile();
                LOGGER.info("Created new file as it didn't exist: " + file);
            }
        } catch (Exception e) {
            throw new FileFluxException("File [" + file + "] didn't exist. But couldn't be created empty", e);
        }
    }

    /**
     * Get the line at a "fractional" position (between 0 and 1) of the given path. eg if {@code fractionalPosition} is 0.5
     * this method will return a line approximately at half of the file.
     *
     * The result is just wrapped into a Mono for consistency with the rest of the methods in this class.
     * So a call to {@link Mono#block}, {@link Mono#subscribe} or equivalent is needed to get the actual result. But you can use
     * the rich {@link Mono} API to execute the call in another thread or to interact with other {@code Monos}
     *
     * If the position given is placed at the middle of a line (as will probably happen most of the times), then the line returned will be
     * always the <em>next one</em>. If the position is at the last line, then the Mono will produce null when subscribed to
     *
     *
     * @param fractionalPosition it is actually a fraction position in the file, so must be between 0 and 1
     */
    public static Mono<String> getNextLineFromPosition(Path path, float fractionalPosition) {
        if (fractionalPosition < 0 || fractionalPosition >= 1) {
            throw new IllegalArgumentException("fractionalPosition must be greater or equals than 0 and less than 1");
        }

        return Mono.fromCallable(() -> {
            try {
                var fileChannel = FileChannel.open(path, StandardOpenOption.READ);
                long position = (long) (fileChannel.size() * fractionalPosition);
                fileChannel.position(position);

                var bufferedReader = new BufferedReader(Channels.newReader(fileChannel, CHARSET));
                bufferedReader.readLine(); // Consuming the expected non-complete line;
                return bufferedReader.readLine();
            } catch (IOException e) {
                throw new FileFluxException(String.format("Error while getting getLineAtPosition(%s, %s)", path, fractionalPosition), e);
            }
        });
    }


    private static void uncheckedExceptionClose(Closeable closeable) {
        LOGGER.debug("Closing {}", closeable);
        try {
            closeable.close();
        } catch (IOException e) {
            LOGGER.debug("Ignored exception while closing " + closeable, e);
        }
    }
}
