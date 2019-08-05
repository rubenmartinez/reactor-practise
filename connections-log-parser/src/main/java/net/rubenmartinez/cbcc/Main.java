package net.rubenmartinez.cbcc;

import net.rubenmartinez.cbcc.params.Options;
import net.rubenmartinez.cbcc.service.CommandLineUtilsService;
import net.rubenmartinez.cbcc.service.ConnectionLogParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;

import javax.inject.Inject;
import java.nio.file.Path;

@ComponentScan
public class Main implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final int EXCEPTION_EXIT_CODE = -2;

    @Inject
    private ConnectionLogParserService connectionLogFileParser;

    @Inject
    private CommandLineUtilsService commandLineUtils;

    @Inject
    private Options options;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Main.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        try {

            var parameters = commandLineUtils.parseCommandLineParameters(args);

            switch (parameters.getMode()) {
                case PARSE: runParseLog(parameters.getLogFile(), options); break;
                case FOLLOW: runFollowLow(parameters.getLogFile(), options); break;
            }

        } catch (Exception e) {
            showExceptionAndExit(e);
        }

    }

    public void runParseLog(Path logFile, Options options) {
        var connectionsFlux = connectionLogFileParser.getConnectionsToHost(logFile, options.getTargetHost(), options.getInitTimestamp(), options.getEndTimestamp());



    }

    public void runFollowLow(Path logFile, Options options) {

    }

    private void showExceptionAndExit(Exception e) {
        System.err.println("---- Error occurred ----\n");

        if (options.isDebug()) {
            System.err.println(e);
            e.printStackTrace(System.err);
        }
        else {
            System.err.println(e.getMessage());
            System.err.println("\nUse option '--debug=true' to show full exception, note '=true' is mandatory in this case)");
        }

        System.exit(EXCEPTION_EXIT_CODE);
    }
}
