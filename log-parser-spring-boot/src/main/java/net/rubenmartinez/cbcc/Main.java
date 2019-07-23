package net.rubenmartinez.cbcc;

import lombok.Getter;
import lombok.ToString;
import net.rubenmartinez.cbcc.logparser.service.ConnectionLogParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.Disposable;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@ComponentScan(lazyInit = true)
public class Main implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Inject
    private Parameters parameters;

    @Inject
    private ConnectionLogParserService connectionLogFileParser;

    private Disposable currentFluxDisposable;
    private Stream currentStream;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Main.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @ToString
    @Getter
    private class TestConsumer implements BiConsumer<TestConsumer, Integer> {
        private int sum = 0;
        private int counter = 0;

        @Override
        public void accept(TestConsumer other, Integer integer) {
            System.out.println("accept: " + integer);
            sum += integer;
            counter++;
        }
    }

    @Override
    public void run(String... args) throws Exception {

        if (parameters.getStream() == 1) {
            System.out.println("Start stream version"); // XXX
            var linesStream = connectionLogFileParser.streamConnectionsTo(Path.of(parameters.getLogFile()), parameters.getTargetHost(), parameters.getInitDateTime(), parameters.getEndDateTime());
            linesStream.forEach(System.out::println);
        }
        else {
            System.out.println("Start flux version"); // XXX
            var linesFlux = connectionLogFileParser.connectionsTo(Path.of(parameters.getLogFile()), parameters.getTargetHost(), parameters.getInitDateTime(), parameters.getEndDateTime());

            currentFluxDisposable = linesFlux.subscribe(System.out::println);
        }
    }
}
