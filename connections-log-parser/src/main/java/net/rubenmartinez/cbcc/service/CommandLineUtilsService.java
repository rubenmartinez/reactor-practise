package net.rubenmartinez.cbcc.service;

import net.rubenmartinez.cbcc.params.CommandLineParams;

public interface CommandLineUtilsService {

    CommandLineParams parseCommandLineParameters(String... args);
}
