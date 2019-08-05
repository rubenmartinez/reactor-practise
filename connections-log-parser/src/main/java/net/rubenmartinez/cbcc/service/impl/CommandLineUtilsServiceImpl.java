package net.rubenmartinez.cbcc.service.impl;

import net.rubenmartinez.cbcc.params.CommandLineParams;
import net.rubenmartinez.cbcc.params.WorkingMode;
import net.rubenmartinez.cbcc.service.CommandLineUtilsService;
import net.rubenmartinez.cbcc.util.Resources;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class CommandLineUtilsServiceImpl implements CommandLineUtilsService {
    private static final String USAGE_CLASSPATH_FILE = "/usage.txt";
    private static final int RC_SHOW_USAGE = -1;

    @Override
    public CommandLineParams parseCommandLineParameters(String... args) {
        if (args.length < 2) {
            showUsageAndExit();
        }

        var parameters = new CommandLineParams();

        parameters.setMode( WorkingMode.fromString(args[0]) );
        parameters.setLogFile( Path.of(args[1]) );

        return parameters;
    }

    private static void showUsageAndExit() {
        try {
            System.out.println(Resources.getResourceAsString(USAGE_CLASSPATH_FILE));
        }
        catch (Exception e) {
            throw new IllegalStateException("Couldn't read classpath file [" + USAGE_CLASSPATH_FILE + "]");
        }

        System.exit(RC_SHOW_USAGE);
    }
}
