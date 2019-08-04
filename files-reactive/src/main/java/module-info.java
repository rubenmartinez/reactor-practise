module net.rubenmartinez.cbcc.reactive {
    exports net.rubenmartinez.cbcc.reactive.file;
    exports net.rubenmartinez.cbcc.reactive.file.exception;

    requires org.apache.commons.io;
    requires reactor.core;
    requires slf4j.api;

    // Required for JUnit-testing
    opens net.rubenmartinez.cbcc.reactive.file.lines;
}