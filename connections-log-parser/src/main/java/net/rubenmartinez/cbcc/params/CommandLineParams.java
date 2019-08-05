package net.rubenmartinez.cbcc.params;

import lombok.Data;

import java.nio.file.Path;

/**
 * Command line mandatory parameters
 */
@Data
public class CommandLineParams {
    private Path logFile;
    private WorkingMode mode;
}
