package net.rubenmartinez.cbcc.reactive.file.lines;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class PositionLimitedBufferedLineReaderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionLimitedBufferedLineReaderTest.class);

    private static final Charset CHARSET = StandardCharsets.US_ASCII;

    private File tempFile;
    private FileChannel tempFileChannel;

    @BeforeEach
    void createTempFile() throws IOException {
        tempFile = File.createTempFile("FileLinesHelperTest", "txt");
        tempFileChannel = FileChannel.open(tempFile.toPath(), StandardOpenOption.READ);
    }

    @AfterEach
    void deleteTempFile() throws IOException {
        tempFileChannel.close();
        tempFile.delete();
    }

    private void setTestFileContentTo(String s) throws IOException {
        Files.writeString(tempFile.toPath(), s, StandardOpenOption.WRITE);
    }

    private static Stream<String> provideLineTerminations() {
        return Stream.of("\n", "\r", "\r\n");
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testTwoLinesFirstLineLongSplitsInTwo(String lineTermination) throws IOException {
        String FIRST_LINE = "1234567890";
        String SECOND_LINE = "2234567890";
        String THIRD_LINE = "3234567890";
        String content = FIRST_LINE + lineTermination + SECOND_LINE + lineTermination + THIRD_LINE;

        setTestFileContentTo(content);

        int maxCharsToRead = FIRST_LINE.length() + lineTermination.length();

        var positionLimitedReader = new PositionLimitedBufferedLineReader(Channels.newReader(tempFileChannel, CHARSET), maxCharsToRead);
        LOGGER.info("First line: {}", positionLimitedReader.readLine());
        LOGGER.info("Second line: {}", positionLimitedReader.readLine());
        LOGGER.info("Third line: {}", positionLimitedReader.readLine());

    }
}


