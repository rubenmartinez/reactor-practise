package net.rubenmartinez.cbcc.reactive.file;

import net.rubenmartinez.cbcc.reactive.file.tailer.FluxEmittingTailerListener;
import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
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

    private static final Charset CHARSET = StandardCharsets.US_ASCII;
    private static final long POLLING_DELAY_MILLIS = 1000; // TODO Configurable

    public enum Mode {
        READ_FROM_BEGINNING,
        READ_FROM_BEGINNING_AND_THEN_FOLLOW_UPDATES,
        FOLLOW_UPDATES_FROM_END
    }

    /**
     * Returns a {@link Flux} of lines of the given {@code path}.
     *
     * Disposing the Flux will stop the "follow" process (if any) and close the opened path.
     *
     * @param path {@link Path} of the file to read
     * @param mode <b>{@code READ_FROM_BEGINNING}</b>: Traditional read, just returns all the line of a file.
     *             <b>{@code READ_FROM_BEGINNING_AND_THEN_FOLLOW_UPDATES}</b>: Returns all the line of a file then keeps reading similar to unix utility 'tail -f'
     *             <b>{@code FOLLOW_UPDATES_FROM_END}</b>: Similar 'tail -f -n0', that is, just waits for new updates
     * @return
     * @throws IOException
     */
    public static Flux<String> lines(Path path, Mode mode) throws IOException {
        FileFlux fileFlux = new FileFlux();

        switch (mode) {
            case READ_FROM_BEGINNING:
                return fileFlux.readLines(path);
            case READ_FROM_BEGINNING_AND_THEN_FOLLOW_UPDATES:
                return fileFlux.follow(path, false);
            case FOLLOW_UPDATES_FROM_END:
                return fileFlux.follow(path, true);
            default:
                throw new IllegalStateException("Unexpected mode: " + mode);
        }
    }

    /**
     * Private constructor as this is an utility class, similar to {@link Files}
     */
    private FileFlux() {
    }

    /**
     * Using Apache Commons {@link Tailer} to do the work of polling the file at intervals to check if it has been updated
     * Tailer allows just to write a Listener ({@link org.apache.commons.io.input.TailerListener} implemented by {@link FluxEmittingTailerListener}
     *
     * That listener basically emits a new element (log line) in the Flux when it receives new lines from Tailer.
     */
    private Flux<String> follow(Path path, boolean fromEnd) {

        // Using Apache Commons Tailer (org.apache.commons.io.input.Tailer)
        // That API

        return Flux.push(emitter -> {
            var tailerListener = new FluxEmittingTailerListener(emitter);
            Tailer tailer = new Tailer(path.toFile(), tailerListener, POLLING_DELAY_MILLIS, fromEnd); // XXX Configure 1K
            tailer.run();
        });
    }

    private Flux<String> readLines(Path path) throws IOException {
       BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.US_ASCII);
        return Flux.create(emitter -> {
            emitter.onDispose(() -> uncheckedExceptionClose(bufferedReader));
            emitter.onCancel(() -> uncheckedExceptionClose(bufferedReader));

            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    emitter.next(line);
                }
            } catch (IOException e) {
                emitter.error(e);
            } finally {
                uncheckedExceptionClose(bufferedReader);
            }

            emitter.complete();
        });
    }

    private static void uncheckedExceptionClose(Reader reader) {
        LOGGER.debug("Closing reader {}", reader);
        try {
            reader.close();
        } catch (IOException e) {
            LOGGER.debug("Ignored exception while closing reader " + reader, e);
        }
    }

    /**
     * Get the line at a percentage position of the given path. eg if {@code percentagePosition} is 0.5
     * this method will return a line approximately at half of the file.
     *
     * This method can be used as a rough binary search to find something in a file that whose lines are ordered.
     *
     * <b>Not Tested</b> (not used in the end)
     */
    public static String getLineAtPosition(Path path, float percentagePosition) throws IOException {
        var fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        long position = (long) (fileChannel.size() * percentagePosition);
        fileChannel.position(position);

        var bufferedReader = new BufferedReader(Channels.newReader(fileChannel, CHARSET));

        bufferedReader.readLine(); // Consuming potential non-complete line;
        return bufferedReader.readLine(); // XXX TODO manage exception if position was at the end of the file
    }

}
