package net.rubenmartinez.cbcc.reactive.file.lines;

import lombok.Data;
import lombok.NonNull;

@Data
public class FilePositionRange {

    @NonNull
    private long fromPosition;

    @NonNull
    private long toPosition;
}
