package net.rubenmartinez.cbcc.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class Config {

    @Value("${timestampOrderTolerance:0}")
    private long timestampOrderTolerance;
}
