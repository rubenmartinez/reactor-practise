package net.rubenmartinez.cbcc.util;

import java.io.InputStream;
import java.util.Scanner;

public class Resources {

    public static InputStream getResourceAsStream(String resource) {
         return Resources.class.getResourceAsStream(resource);
    }

    public static String getResourceAsString(String resource) {
        try (Scanner s = new Scanner(getResourceAsStream(resource))) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private Resources() {
    }
}
