package net.rubenmartinez.cbcc;

import com.beust.jcommander.Parameter;
import lombok.Data;

@Data
public class CommandLineArgs {

    @Parameter(names = { "--init-datetime", "-i" }, description = "Start timestamp. The format must be ISO8601, eg.  XXX")
    private String initDatetime;

    @Parameter(names = { "--end-datetime", "-e" }, description = "End timestamp. The format must be ISO8601, eg.  XXX")
    private String endDatetime;

}
