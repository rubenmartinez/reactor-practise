package net.rubenmartinez.cbcc;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class Parameters {

    @Value("${logFile:Instructions/input-file-100010000.txt}") // TODO XXX Remove default
    private String logFile;

    @Value("${initDateTime:0}") // TODO XXX Remove default
    private long initDateTime;

    @Value("${endDateTime:1565727042686}") // TODO XXX Remove default
    private long endDateTime;

    @Value("${targetHost:Markena}")
    private String targetHost;

    @Value("${stream:1}") // XXX
    private int stream;

}
