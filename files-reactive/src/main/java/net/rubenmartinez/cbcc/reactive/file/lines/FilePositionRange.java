package net.rubenmartinez.cbcc.reactive.file.lines;


public class FilePositionRange {
    // Tried to use lombok, but it doesn't work very well yet with Jigsaw modules https://github.com/rzwitserloot/lombok/issues/1723
    // Apparently it is solved in edge release (without version number) so I'd rather not to use it for the moment just for one class in this library

    private long fromPosition;
    private long toPosition;

    public FilePositionRange(long fromPosition, long toPosition) {
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
    }

    public long getFromPosition() {
        return fromPosition;
    }

    public void setFromPosition(long fromPosition) {
        this.fromPosition = fromPosition;
    }

    public long getToPosition() {
        return toPosition;
    }

    public void setToPosition(long toPosition) {
        this.toPosition = toPosition;
    }
}
