module net.rubenmartinez.cbcc.reactive {
    exports net.rubenmartinez.cbcc.reactive.file;
    exports net.rubenmartinez.cbcc.reactive.file.exception;

    requires reactor.core;
    requires slf4j.api;
    requires org.apache.commons.io;

    // Required for JUnit-testing
    opens net.rubenmartinez.cbcc.reactive.file.lines;
}