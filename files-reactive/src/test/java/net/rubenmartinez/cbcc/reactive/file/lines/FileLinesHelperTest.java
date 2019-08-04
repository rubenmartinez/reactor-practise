package net.rubenmartinez.cbcc.reactive.file.lines;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileLinesHelperTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileLinesHelperTest.class);

    private static final String CHARSET = StandardCharsets.US_ASCII.name();

    private static File tempFile;
    private static FileChannel tempFileChannel;

    private static final String[] POSSIBLE_LINE_TERMINATIONS_CSV = {"test"};

    @BeforeAll
    private static void createTempFile() throws IOException {
        tempFile = File.createTempFile("FileLinesHelperTest", "txt");
        tempFileChannel = FileChannel.open(tempFile.toPath(), StandardOpenOption.READ);
    }

    @AfterAll
    private static void deleteTempFile() throws IOException {
        tempFileChannel.close();
        tempFile.delete();
    }

    private static void setFileContentTo(String s) throws IOException {
        Files.writeString(tempFile.toPath(), s, StandardOpenOption.WRITE);
    }

    private static Stream<Arguments> provideLineTerminationsAndAverageLineSizeHint() {
        var lineTerminations = List.of("\n", "\r", "\r\n");
        var averageLineSizesHint = List.of(3, 14, 100, 1000);

        // Cartesian product to test all combinations
        return lineTerminations.stream().flatMap(lineTermination -> averageLineSizesHint.stream().map(lineSize -> Arguments.of(lineTermination, lineSize)));
    }

    @Test
    void testDelete() throws IOException {
        var fileChannel = tempFileChannel;

        String FIRST_LINE = "First line";
        String SECOND_LINE = "Second line opasduifhaspdufhpqwef hpasdoifh apsdfioha spdfoihas pdfiahspd ofahspdf hapsodfh asSecond line opasduifhaspdufhpqwef hpasdoifh apsdfioha spdfoihas pdfiahspd ofahspdf hapsodfh asSecond line opasduifhaspdufhpqwef hpasdoifh apsdfioha spdfoihas pdfiahspd ofahspdf hapsodfh asSecond line opasduifhaspdufhpqwef hpasdoifh apsdfioha spdfoihas pdfiahspd ofahspdf hapsodfh asSecond line opasduifhaspdufhpqwef hpasdoifh apsdfioha spdfoihas pdfiahspd ofahspdf hapsodfh asSecond line opasduifhaspdufhpqwef hpasdoifh apsdfioha spdfoihas pdfiahspd ofahspdf hapsodfh as";

        String content = FIRST_LINE + "\r\n" + SECOND_LINE;

        LOGGER.info("content.length(): {}", content.length());
        LOGGER.info("FIRST_LINE.length(): {}", FIRST_LINE.length());

        setFileContentTo(FIRST_LINE + "\r\n" + SECOND_LINE);

        long originalPosition = fileChannel.position();
        LOGGER.info("originalPosition: {}", originalPosition);
        var byteBuffer = ByteBuffer.allocate(4);
        fileChannel.read(byteBuffer);
        long newPosition = fileChannel.position();
        LOGGER.info("newPosition: {}", fileChannel.position());
        var bufferedReader = new BufferedReader(Channels.newReader(fileChannel, CHARSET));
        String previousLineCandidate = bufferedReader.readLine();
        LOGGER.info("line: {}", previousLineCandidate);

        LOGGER.info("previousLineCandidate.length(): {}", previousLineCandidate.length());

        fileChannel.position(newPosition + previousLineCandidate.length() + 2);
        LOGGER.info("newPosition3: {}", fileChannel.position());

        var bufferedReader2 = new BufferedReader(Channels.newReader(fileChannel, CHARSET));
        String line = bufferedReader2.readLine();
        LOGGER.info("line: {}", line);

        LOGGER.info("newPosition3: {}", fileChannel.position());

    }

    @ParameterizedTest
    @MethodSource("provideLineTerminationsAndAverageLineSizeHint")
    void testTwoLines(String lineTermination, int averageLineSizesHint) throws IOException {
        String FIRST_LINE = "First line";
        String SECOND_LINE = "Second line";

        setFileContentTo(FIRST_LINE + lineTermination + SECOND_LINE);

        tempFileChannel.position(0);
        assertEquals(FIRST_LINE, FileLinesHelper.getEntireLineAtCurrentPosition(tempFileChannel, CHARSET, averageLineSizesHint));

        tempFileChannel.position(1);
        assertEquals(FIRST_LINE, FileLinesHelper.getEntireLineAtCurrentPosition(tempFileChannel, CHARSET, averageLineSizesHint));

        tempFileChannel.position(FIRST_LINE.length()-1);
        assertEquals(FIRST_LINE, FileLinesHelper.getEntireLineAtCurrentPosition(tempFileChannel, CHARSET, averageLineSizesHint));

        tempFileChannel.position(FIRST_LINE.length());
        assertEquals(FIRST_LINE, FileLinesHelper.getEntireLineAtCurrentPosition(tempFileChannel, CHARSET, averageLineSizesHint));

        tempFileChannel.position(FIRST_LINE.length()+1);
        assertEquals(FIRST_LINE, FileLinesHelper.getEntireLineAtCurrentPosition(tempFileChannel, CHARSET, averageLineSizesHint));

        tempFileChannel.position(FIRST_LINE.length()+5);
        assertEquals(SECOND_LINE, FileLinesHelper.getEntireLineAtCurrentPosition(tempFileChannel, CHARSET, averageLineSizesHint));

        tempFileChannel.position(FIRST_LINE.length()+4);
        assertEquals(SECOND_LINE, FileLinesHelper.getEntireLineAtCurrentPosition(tempFileChannel, CHARSET, averageLineSizesHint));

    }
}
