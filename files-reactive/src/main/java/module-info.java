module net.rubenmartinez.cbcc.reactive {
    exports net.rubenmartinez.cbcc.reactive.file;
    exports net.rubenmartinez.cbcc.reactive.file.exception;

    requires org.apache.commons.io;
    requires reactor.core;
    requires slf4j.api;
}