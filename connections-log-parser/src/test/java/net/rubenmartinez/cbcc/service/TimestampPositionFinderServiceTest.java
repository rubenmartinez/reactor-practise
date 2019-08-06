package net.rubenmartinez.cbcc.service;

import net.rubenmartinez.cbcc.Main;
import net.rubenmartinez.cbcc.logparsing.components.LogLineParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

@SpringJUnitConfig(Main.class)
class TimestampPositionFinderServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimestampPositionFinderServiceTest.class);

    private static final Charset CHARSET = StandardCharsets.US_ASCII;

    @Inject
    TimestampPositionFinderService timestampPositionFinder;

    @Inject
    LogLineParser lineParser;

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

    @Test
    public void testOneLine() throws IOException {
        String content = "10 a b";

        setTestFileContentTo(content);

        long position;

        position = timestampPositionFinder.findNearTimestamp(0, tempFile.toPath());
        assertThat(position, equalTo(0L));

        position = timestampPositionFinder.findNearTimestamp(5, tempFile.toPath());
        assertThat(position, equalTo(0L));

        position = timestampPositionFinder.findNearTimestamp(10, tempFile.toPath());
        assertThat(position, equalTo(0L));
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    public void testTwoLines(String lineTermination) throws IOException {
        String content = "10 a b" + lineTermination + "20 b c";

        setTestFileContentTo(content);
        long position;

        position = timestampPositionFinder.findNearTimestamp(0, tempFile.toPath());
        assertThat(position, equalTo(0L));

        position = timestampPositionFinder.findNearTimestamp(5, tempFile.toPath());
        assertThat(position, equalTo(0L));

        position = timestampPositionFinder.findNearTimestamp(10, tempFile.toPath());
        assertThat(position, equalTo(0L));

        position = timestampPositionFinder.findNearTimestamp(11, tempFile.toPath());
        assertThat(position, lessThanOrEqualTo((long) content.length() + lineTermination.length() + 1));

        position = timestampPositionFinder.findNearTimestamp(15, tempFile.toPath());
        assertThat(position, lessThanOrEqualTo((long) content.length() + lineTermination.length() + 1));

        position = timestampPositionFinder.findNearTimestamp(20, tempFile.toPath());
        assertThat(position, lessThanOrEqualTo((long) content.length() + lineTermination.length() + 1));

    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    public void testManyLines(String lineTermination) throws IOException {

        int NUM_LINES = 10_000_000;

        LOGGER.info("Generating test file...");
        StringBuilder content = new StringBuilder(NUM_LINES * 3 * 20);
        for (int i=0; i<NUM_LINES; i++) {
            content.append(i).append(" firstSource target").append(lineTermination)
                   .append(i).append(" secondSource target").append(lineTermination)
                   .append(i).append(" thirdSource target").append(lineTermination);
        }
        setTestFileContentTo(content.toString());


        long lastPosition = 0;
        //for(long i=1000; i<NUM_LINES; i+=new Random().nextInt(10)) { // Random steps between 0-500 otherwise this test would take ages
        for(long i=1000; i<NUM_LINES; i++) {
            long position = timestampPositionFinder.findNearTimestamp(i, tempFile.toPath());
            assertThat(position, greaterThanOrEqualTo(lastPosition));

            tempFileChannel.position(position);
            String lineAtPosition = getNextLineNonEmpty(tempFileChannel);
            LOGGER.info("Searching for ts [{}] returned line at position [{}]: {}", i, position, lineAtPosition);

            var connectionLogLine = lineParser.parseLine(lineAtPosition);
            assertThat(connectionLogLine.getTimestamp(), lessThanOrEqualTo(i));

            if (connectionLogLine.getTimestamp() == i) {
                assertThat(connectionLogLine.getSourceHost(), equalTo("firstSource"));
            }
        }
    }


    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    public void testManyLinesWithALotOfRepeatedTimestampsAtTheEnd(String lineTermination) throws IOException {

        int repeatedTimestamp = 102;

        LOGGER.info("Generating test file...");
        StringBuilder content = new StringBuilder(300000 * 20);
        for (int i=0; i<100; i++) {
            content.append(i).append(" source target").append(lineTermination);
        }
        for (int i=0; i<300000; i++) {
            content.append(repeatedTimestamp).append(" repeated repeated").append(lineTermination);
        }
        setTestFileContentTo(content.toString());

        long position = timestampPositionFinder.findNearTimestamp(repeatedTimestamp, tempFile.toPath());
        tempFileChannel.position(position);
        String lineAtPosition = getNextLineNonEmpty(tempFileChannel);
        LOGGER.info("Searching for ts [{}] returned line at position [{}]: {}", repeatedTimestamp, position, lineAtPosition);

        var connectionLogLine = lineParser.parseLine(lineAtPosition);
        assertThat(connectionLogLine.getTimestamp(), lessThan((long)repeatedTimestamp));
    }


    private static String getNextLineNonEmpty(FileChannel fileChannel) throws IOException {
        var bufferedReader = new BufferedReader(Channels.newReader(fileChannel, CHARSET));
        bufferedReader.readLine(); // Ignoring a (likely) non-complete line;

        String nextLine;
        do {
            nextLine = bufferedReader.readLine();
        } while (nextLine != null && nextLine.isEmpty());

        return nextLine;
    }
}