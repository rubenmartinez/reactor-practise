package net.rubenmartinez.cbcc.params;

import net.rubenmartinez.cbcc.exception.UserInputException;

import java.util.Arrays;

public enum WorkingMode {
    PARSE, FOLLOW;

    public static WorkingMode fromString(String s) {
        for (WorkingMode mode: WorkingMode.values()) {
            if (mode.name().equalsIgnoreCase((s))) {
                return mode;
            }
        }

        throw new UserInputException("Invalid working mode: [" + s + "]. Please use one of: " + Arrays.asList(WorkingMode.values()));
    }
}
