package net.rubenmartinez.cbcc.reactive.file.lines;


import net.rubenmartinez.test.util.TestLoggingExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(TestLoggingExtension.class)
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
    void testThreeLinesOnly2Bytes(String lineTermination) throws IOException {
        String FIRST_LINE = "1234567890";
        String SECOND_LINE = "2234567890";
        String THIRD_LINE = "3234567890";
        String content = FIRST_LINE + lineTermination + SECOND_LINE + lineTermination + THIRD_LINE;

        setTestFileContentTo(content);

        int maxCharsToRead = 2;

        var positionLimitedReader = new PositionLimitedBufferedLineReader(Channels.newReader(tempFileChannel, CHARSET), maxCharsToRead);

        assertThat(positionLimitedReader.readLine(), equalTo(FIRST_LINE.substring(0, 2)));
        assertThat(positionLimitedReader.readLine(), nullValue());
        assertThat(positionLimitedReader.readLine(), nullValue());
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testOneLinesOnly2Bytes(String lineTermination) throws IOException {
        String content = "1234567890";

        setTestFileContentTo(content);

        int maxCharsToRead = 2;

        var positionLimitedReader = new PositionLimitedBufferedLineReader(Channels.newReader(tempFileChannel, CHARSET), maxCharsToRead);

        assertThat(positionLimitedReader.readLine(), equalTo(content.substring(0, 2)));
        assertThat(positionLimitedReader.readLine(), nullValue());
        assertThat(positionLimitedReader.readLine(), nullValue());
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testOneLineReadAll(String lineTermination) throws IOException {
        String content = "1234567890";

        setTestFileContentTo(content);

        int maxCharsToRead = content.length() + 100;

        var positionLimitedReader = new PositionLimitedBufferedLineReader(Channels.newReader(tempFileChannel, CHARSET), maxCharsToRead);

        assertThat(positionLimitedReader.readLine(), equalTo(content));
        assertThat(positionLimitedReader.readLine(), nullValue());
        assertThat(positionLimitedReader.readLine(), nullValue());
    }


    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testThreeLinesOnlyFirstLinePlus2Bytes(String lineTermination) throws IOException {
        String FIRST_LINE = "1234567890";
        String SECOND_LINE = "2234567890";
        String THIRD_LINE = "3234567890";
        String content = FIRST_LINE + lineTermination + SECOND_LINE + lineTermination + THIRD_LINE;

        setTestFileContentTo(content);

        int maxCharsToRead = FIRST_LINE.length() + lineTermination.length() + 2;

        var positionLimitedReader = new PositionLimitedBufferedLineReader(Channels.newReader(tempFileChannel, CHARSET), maxCharsToRead);

        assertThat(positionLimitedReader.readLine(), equalTo(FIRST_LINE));
        assertThat(positionLimitedReader.readLine(), equalTo(SECOND_LINE.substring(0, 2)));
        assertThat(positionLimitedReader.readLine(), nullValue());
        assertThat(positionLimitedReader.readLine(), nullValue());
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testThreeLinesContentLengthRead(String lineTermination) throws IOException {
        String FIRST_LINE = "1234567890";
        String SECOND_LINE = "2234567890";
        String THIRD_LINE = "3234567890";
        String content = FIRST_LINE + lineTermination + SECOND_LINE + lineTermination + THIRD_LINE;

        setTestFileContentTo(content);

        int maxCharsToRead = content.length();

        var positionLimitedReader = new PositionLimitedBufferedLineReader(Channels.newReader(tempFileChannel, CHARSET), maxCharsToRead);

        assertThat(positionLimitedReader.readLine(), equalTo(FIRST_LINE));
        assertThat(positionLimitedReader.readLine(), equalTo(SECOND_LINE));
        assertThat(positionLimitedReader.readLine(), equalTo(THIRD_LINE));
        assertThat(positionLimitedReader.readLine(), nullValue());
        assertThat(positionLimitedReader.readLine(), nullValue());
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testThreeLinesMoreThanContentLengthRead(String lineTermination) throws IOException {
        String FIRST_LINE = "1234567890";
        String SECOND_LINE = "2234567890";
        String THIRD_LINE = "3234567890";
        String content = FIRST_LINE + lineTermination + SECOND_LINE + lineTermination + THIRD_LINE;

        setTestFileContentTo(content);

        int maxCharsToRead = content.length() + 200;

        var positionLimitedReader = new PositionLimitedBufferedLineReader(Channels.newReader(tempFileChannel, CHARSET), maxCharsToRead);
        assertThat(positionLimitedReader.readLine(), equalTo(FIRST_LINE));
        assertThat(positionLimitedReader.readLine(), equalTo(SECOND_LINE));
        assertThat(positionLimitedReader.readLine(), equalTo(THIRD_LINE));
        assertThat(positionLimitedReader.readLine(), nullValue());
        assertThat(positionLimitedReader.readLine(), nullValue());
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testManyLinesVariableLength(String lineTermination) throws IOException {
        StringBuilder fileContentBuilder = new StringBuilder();
        var linesList = new ArrayList<String>();

        fileContentBuilder.append(lineTermination); // Ensure at least some line of zero characters for the tests
        linesList.add("");

        new Random().ints(10, 1, 5)
                    .mapToObj(randomCharactersInLine -> getLineOfRandomCharacters(randomCharactersInLine, lineTermination))
                    .forEach(line -> { fileContentBuilder.append(line); fileContentBuilder.append(lineTermination); linesList.add(line); });

        fileContentBuilder.append(lineTermination); // Ensure at least some line of zero characters for the tests in the middle of the file
        linesList.add("");

        new Random().ints(3000, 0, 1000)
                .mapToObj(randomCharactersInLine -> getLineOfRandomCharacters(randomCharactersInLine, lineTermination))
                .forEach(line -> { fileContentBuilder.append(line); fileContentBuilder.append(lineTermination); linesList.add(line); });

        fileContentBuilder.append(lineTermination); // Ensure at least some line of zero characters for the tests
        linesList.add("");

        String content = fileContentBuilder.toString();

        setTestFileContentTo(content);

        int startPosition = 0;
        for (String line: linesList) {
            tempFileChannel.position(startPosition);
            int maxCharsToRead = line.length() + lineTermination.length();
            var positionLimitedReader = new PositionLimitedBufferedLineReader(Channels.newReader(tempFileChannel, CHARSET), maxCharsToRead);
            assertThat(positionLimitedReader.readLine(), equalTo(line));
            startPosition += maxCharsToRead;
        }
    }

    private static String getLineOfRandomCharacters(int numberOfCharacters, String lineTermination) {
        StringBuilder lineBuilder = new StringBuilder();
        new Random().ints(numberOfCharacters, 'a', 'z').forEach(lineBuilder::append);

        return lineBuilder.toString();
    }

}


