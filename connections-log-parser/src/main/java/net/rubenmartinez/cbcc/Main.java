package net.rubenmartinez.cbcc;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.domain.ConnectionLogStats;
import net.rubenmartinez.cbcc.exception.UserInputException;
import net.rubenmartinez.cbcc.params.Options;
import net.rubenmartinez.cbcc.service.CommandLineUtilsService;
import net.rubenmartinez.cbcc.service.ConnectionLogParserService;
import net.rubenmartinez.cbcc.service.ConnectionLogStatsFormatterService;
import net.rubenmartinez.cbcc.service.ConnectionLogWatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@ComponentScan
public class Main implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final int EXCEPTION_EXIT_CODE = -2;

    @Inject private ConnectionLogWatcherService connectionLogWatcherParser;

    @Inject private CommandLineUtilsService commandLineUtils;

    @Inject private ConnectionLogStatsFormatterService logStatsFormatter;

    @Named("sequential")
    @Inject private ConnectionLogParserService connectionLogFileParser;

    @Named("parallel")
    @Inject private ConnectionLogParserService connectionLogParallelFileParser;

    @Inject private Options options;


    public static void main(String[] args) {
        try {
            SpringApplication app = new SpringApplication(Main.class);
            app.setBannerMode(Banner.Mode.OFF);
            app.run(args);
        } catch (Exception e) {
            showExceptionAndExit(e);
        }
    }

    @Override
    public void run(String... args) {
        var parameters = commandLineUtils.parseCommandLineParameters(args);

        switch (parameters.getMode()) {
            case FOLLOW: runFollowLog(parameters.getLogFile(), options); break;
            case PARSE: runParseLog(parameters.getLogFile(), options); break;
        }
    }

    public void runFollowLog(Path logFile, Options options) {
        Flux<ConnectionLogStats> statsFlux = connectionLogWatcherParser.collectStats(logFile, options.getSourceHost(), options.getTargetHost(), options.getStatsWindowDuration());

        output(String.format("\nOutput stats each %s seconds, watching file [%s] (from now on)\n", options.getStatsWindowDuration().toSeconds(), logFile));
        // This blocks forever but note that the watched file is closed automatically by the files-reactive library on a termination
        // signal using a Shutdown Hook
        statsFlux
                .map(logStatsFormatter::format)
                .doOnNext(Main::output)
                .blockLast();
    }


    public void runParseLog(Path logFile, Options options) {
        checkParseLogParameters(logFile, options);

        Flux<ConnectionLogLine> connectionsFlux;

        if (options.getSplits() == 0) {
            connectionsFlux = connectionLogFileParser.getConnectionsToHost(logFile, options.getTargetHost().get(), options.getInitTimestamp(), options.getEndTimestamp());
        }
        else {
            connectionsFlux = connectionLogParallelFileParser.getConnectionsToHost(logFile, options.getTargetHost().get(), options.getInitTimestamp(), options.getEndTimestamp());
        }

        // XXX
        long startTime = System.nanoTime();

        if (options.isUniqueHosts()) {
            Set<String> uniqueHosts = connectionsFlux.collect(() -> new HashSet<String>(), (set, connection) -> set.add(connection.getSourceHost())).block();

            uniqueHosts.forEach(Main::output);
        }
        else {
            connectionsFlux
                .doOnComplete(() -> { long elapsed = System.nanoTime() - startTime; output("------------------\n************** Elapsed: " + elapsed / 1000000);})
                .subscribe(
                    connection -> output(connection.getSourceHost() + " at " + connection.getTimestamp())
            );
        }
    }

    private void checkParseLogParameters(Path logFile, Options options) {
        if (options.getInitTimestamp() == null || options.getEndTimestamp() == null || options.getTargetHost().isEmpty()) {
            throw new UserInputException("initTimestamp, endTimestamp and targetHost are all required in this mode");
        }
    }

    private static final void output(String s) {
        System.out.println(s);
    }

    private static void showExceptionAndExit(Throwable e) {
        System.err.println("---- Error occurred ----\n");
        do {
            System.err.println(e.getMessage());
            e = e.getCause();
        } while (e != null);
        System.exit(EXCEPTION_EXIT_CODE);
    }
}
