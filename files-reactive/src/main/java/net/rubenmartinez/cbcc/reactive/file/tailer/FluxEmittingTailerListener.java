package net.rubenmartinez.cbcc.reactive.file.tailer;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;

/**
 * Internal module class. It is an apache commons {@link org.apache.commons.io.input.TailerListener},
 * so it gets informed of every new lines written to specific file
 *
 * For every new line this TailListener "publish" or emits that new line in a Flux, so the line can be read (consumed) and processed reactively.
 */
public class FluxEmittingTailerListener extends TailerListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluxEmittingTailerListener.class);

    FluxSink<String> emitter;
    Tailer tailer;

    public FluxEmittingTailerListener(FluxSink<String> emitter) {
        this.emitter = emitter;
        ensureClosingOnTermination();
    }

    private void ensureClosingOnTermination() {
        this.emitter.onDispose(this::close);
        this.emitter.onCancel(this::close);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FluxEmittingTailerListener.this.close();
            }
        });
    }

    private void close() {
        if (tailer != null) {
            tailer.stop(); // Apache commons tailer will close the file too after stopping
            LOGGER.debug("close: tailer stopped");
        }
        else {
            LOGGER.debug("Trying to stop tailer, but it was not initiated. Ignoring");
        }
    }

    @Override
    public void init(Tailer tailer) {
        this.tailer = tailer;
        LOGGER.debug("init: {}", tailer);
    }

    @Override
    public void fileNotFound() {
        emitter.error(new TailerListenerException("File not found: " + (tailer != null ? tailer.getFile(): "[No tailer]")));
    }

    @Override
    public void fileRotated() {
        LOGGER.warn("File rotated while tailing: {}", tailer.getFile());
    }

    @Override
    public void handle(String line) {
        emitter.next(line);
    }

    @Override
    public void handle(Exception e) {
        emitter.error(new TailerListenerException("Exception while tailing file: " + (tailer != null ? tailer.getFile(): "[No tailer]"), e));
    }
}
