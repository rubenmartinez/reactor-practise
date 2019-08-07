package net.rubenmartinez.cbcc;

import net.rubenmartinez.cbcc.domain.ConnectionLogLine;
import net.rubenmartinez.cbcc.domain.ConnectionLogStats;
import net.rubenmartinez.cbcc.exception.UserInputException;
import net.rubenmartinez.cbcc.params.Options;
import net.rubenmartinez.cbcc.service.CommandLineUtilsService;
import net.rubenmartinez.cbcc.service.ConnectionLogParserService;
import net.rubenmartinez.cbcc.service.ConnectionLogStatsFormatterService;
import net.rubenmartinez.cbcc.service.ConnectionLogWatcherService;
import net.rubenmartinez.cbcc.service.TimestampPositionFinderService;
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

/**
 * Using just @ComponentScan instead of @SpringBoot as AutoConfiguration is not really worth for this CommandLineRunner.
 * There are not many Spring Beans to autoconfigure and this way we save some initialization time.
 *
 * SpringBoot is used anyway, for command line parameters and as the maven parent project of this project it is convenient
 * for the embedded maven shade plugin (jar creation) and dependency versions compatibilities. It is also useful for testing.
 */
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

    @Inject private TimestampPositionFinderService positionFinderService;

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

    public void run(String... args) {
        var parameters = commandLineUtils.parseCommandLineParameters(args);

        switch (parameters.getMode()) {
            case FOLLOW: runFollowLog(parameters.getLogFile(), options); break;
            case PARSE: runParseLog(parameters.getLogFile(), options); break;
        }
    }

    public void runFollowLog(Path logFile, Options options) {
        Flux<ConnectionLogStats> statisticsFlux = connectionLogWatcherParser.collectStats(logFile, options.getSourceHost(), options.getTargetHost(), options.getStatsWindowDuration());

        output(String.format("\nOutput stats each %s seconds, watching file [%s] (from now on)\n", options.getStatsWindowDuration().toSeconds(), logFile));
        // This blocks forever but note that the watched file is closed automatically by the files-reactive library on a termination
        // signal using a Shutdown Hook created directly inside the library
        statisticsFlux
                .map(logStatsFormatter::format)
                .doOnNext(Main::output)
                .blockLast();
    }

    public void runParseLog(Path logFile, Options options) {
        checkParseLogParameters(logFile, options);

        Flux<ConnectionLogLine> connectionsFlux;

        long fromPosition = 0;
        if (options.isPresearchTimestamp()) {
            fromPosition = positionFinderService.findNearTimestamp(getAdjustedStartTimestamp(options.getInitTimestamp()), logFile);
            output("Starting at position: " + fromPosition);
        }

        if (options.getSplits() == 0) {
            connectionsFlux = connectionLogFileParser.getConnectionsToHost(logFile, fromPosition, options.getTargetHost().get(), options.getInitTimestamp(), options.getEndTimestamp());
        }
        else {
            output("Warn: --splits it is a experimental feature, it could misbehave in some situations");
            connectionsFlux = connectionLogParallelFileParser.getConnectionsToHost(logFile, fromPosition, options.getTargetHost().get(), options.getInitTimestamp(), options.getEndTimestamp());
        }

        if (options.isUniqueHosts()) {
            Set<String> uniqueHosts = connectionsFlux.collect(() -> new HashSet<String>(), (set, connection) -> set.add(connection.getSourceHost())).block();

            uniqueHosts.forEach(Main::output);
        }
        else {
            connectionsFlux
                .subscribe(connection -> output(connection.getSourceHost() + " at " + connection.getTimestamp())
            );
        }
    }

    private void checkParseLogParameters(Path logFile, Options options) {
        if (options.getInitTimestamp() == null || options.getEndTimestamp() == null || options.getTargetHost().isEmpty()) {
            throw new UserInputException("initTimestamp, endTimestamp and targetHost are all required in this mode");
        }
    }


    private long getAdjustedStartTimestamp(long startTimestamp) {
        return startTimestamp + options.getTimestampOrderToleranceMillis();
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
