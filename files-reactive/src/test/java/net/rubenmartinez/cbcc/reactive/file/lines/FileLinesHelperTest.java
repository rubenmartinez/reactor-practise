package net.rubenmartinez.cbcc.reactive.file.lines;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class FileLinesHelperTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileLinesHelperTest.class);

    private static final String CHARSET = StandardCharsets.US_ASCII.name();

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

    private static Stream<Arguments> provideLineTerminationsAndSplits() {
        var lineTerminations = List.of("\n", "\r", "\r\n");
        var splits = List.of(1, 2, 3, 10, 100, 1000);

        // Cartesian product to get all combinations between lineTerminations and splits
        return lineTerminations.stream().flatMap(lineTermination -> splits.stream().map(split -> Arguments.of(lineTermination, split)));
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testTwoLinesFirstLineLongSplitsInTwo(String lineTermination) throws IOException {
        String FIRST_LINE = "First line is quite long, so when split in 2, it will likely be really split in 2 lines";
        String SECOND_LINE = "Second line";
        String content = FIRST_LINE + lineTermination + SECOND_LINE;

        setTestFileContentTo(content);

        int splits = 2;

        FilePositionRange[] filePositionRanges = FileLinesHelper.getSplitPositionsAtLineBoundaries(tempFile.toPath(), splits);
        assertThat(filePositionRanges.length,  equalTo(2));
        assertThat(filePositionRanges[0].getFromPosition(), equalTo(0L));
        assertThat(filePositionRanges[0].getToPosition(), equalTo((long) (FIRST_LINE.length() + lineTermination.length())));
    }


    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testTwoShortLinesSplitsInTwoBecauseRequestedManySplits(String lineTermination) throws IOException {
        String FIRST_LINE = "Short";
        String SECOND_LINE = "Second line";
        String content = FIRST_LINE + lineTermination + SECOND_LINE;

        setTestFileContentTo(content);

        int splits = 100;

        FilePositionRange[] filePositionRanges = FileLinesHelper.getSplitPositionsAtLineBoundaries(tempFile.toPath(), splits);
        assertThat(filePositionRanges.length,  equalTo(2));
        assertThat(filePositionRanges[0].getFromPosition(),  equalTo(0L));
        assertThat(filePositionRanges[0].getToPosition(), equalTo((long) (FIRST_LINE.length() + lineTermination.length())));
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminations")
    void testThreeLinesSplitsInTwo(String lineTermination) throws IOException {
        String FIRST_LINE = "First line";
        String SECOND_LINE = "Second line";
        String THIRD_LINE = "Third line";

        String content = FIRST_LINE + lineTermination + SECOND_LINE + lineTermination + THIRD_LINE;

        setTestFileContentTo(content);

        FilePositionRange[] filePositionRanges = FileLinesHelper.getSplitPositionsAtLineBoundaries(tempFile.toPath(), 2);
        assertThatPositionRangesCoverEntireFile(filePositionRanges, content.length());

        assertThat(filePositionRanges.length,  equalTo(2));
        assertThat(filePositionRanges[1].getFromPosition(), equalTo((long) (FIRST_LINE.length() + lineTermination.length() + SECOND_LINE.length() + lineTermination.length())));
    }


    @ParameterizedTest
    @MethodSource("provideLineTerminationsAndSplits")
    void testOneLineVariableSplits(String lineTermination, int splits) throws IOException {
        String content = "This is the only line";

        setTestFileContentTo(content);

        FilePositionRange[] filePositionRanges = FileLinesHelper.getSplitPositionsAtLineBoundaries(tempFile.toPath(), splits);
        assertThat(filePositionRanges.length,  equalTo(1));
        assertThatPositionRangesCoverEntireFile(filePositionRanges, content.length());
    }


    @ParameterizedTest
    @MethodSource("provideLineTerminationsAndSplits")
    void testTwoLinesVariableSplits(String lineTermination, int splits) throws IOException {
        String content =
                "First line" + lineTermination +
                "Second line";

        setTestFileContentTo(content);

        FilePositionRange[] filePositionRanges = FileLinesHelper.getSplitPositionsAtLineBoundaries(tempFile.toPath(), splits);
        assertThat(filePositionRanges.length,  greaterThanOrEqualTo(1));
        assertThat(filePositionRanges.length,  lessThanOrEqualTo(2));
        assertThatPositionRangesCoverEntireFile(filePositionRanges, content.length());
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminationsAndSplits")
    void testThreeLinesVariableSplits(String lineTermination, int splits) throws IOException {
        String content =
                "First line" + lineTermination +
                "Second line" + lineTermination +
                "Third line";

        setTestFileContentTo(content);

        FilePositionRange[] filePositionRanges = FileLinesHelper.getSplitPositionsAtLineBoundaries(tempFile.toPath(), splits);
        assertThat(filePositionRanges.length,  greaterThanOrEqualTo(1));
        assertThat(filePositionRanges.length,  lessThanOrEqualTo(3));
        assertThatPositionRangesCoverEntireFile(filePositionRanges, content.length());
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminationsAndSplits")
    void testManyLinesVariableLengthVariableSplits(String lineTermination, int splits) throws IOException {

        StringBuilder fileContentBuilder = new StringBuilder();


        fileContentBuilder.append(lineTermination); // Ensure at least some line of zero characters for the tests
        new Random().ints(3000, 0, 1000).forEach(
                randomCharactersInLine -> addLineOfRandomCharactersToBuilder(fileContentBuilder, randomCharactersInLine, lineTermination));
        fileContentBuilder.append(lineTermination); // Ensure at least some line of zero characters for the tests in the middle of the file
        new Random().ints(3000, 0, 1000).forEach(
                randomCharactersInLine -> addLineOfRandomCharactersToBuilder(fileContentBuilder, randomCharactersInLine, lineTermination));
        fileContentBuilder.append(lineTermination); // Ensure at least some line of zero characters for the tests

        String content = fileContentBuilder.toString();

        setTestFileContentTo(content);

        FilePositionRange[] filePositionRanges = FileLinesHelper.getSplitPositionsAtLineBoundaries(tempFile.toPath(), splits);
        assertThatPositionRangesCoverEntireFile(filePositionRanges, content.length());

        LOGGER.info("File length: [{}], requested Splits: [{}], final splits: [{}]", content.length(), splits, filePositionRanges.length);
    }

    @ParameterizedTest
    @MethodSource("provideLineTerminationsAndSplits")
    void testManyLinesNotSoVariableLengthVariableSplits(String lineTermination, int splits) throws IOException {

        StringBuilder fileContentBuilder = new StringBuilder();

        new Random().ints(10000, 17, 34).forEach(
                randomCharactersInLine -> addLineOfRandomCharactersToBuilder(fileContentBuilder, randomCharactersInLine, lineTermination));

        String content = fileContentBuilder.toString();

        setTestFileContentTo(content);

        FilePositionRange[] filePositionRanges = FileLinesHelper.getSplitPositionsAtLineBoundaries(tempFile.toPath(), splits);
        assertThatPositionRangesCoverEntireFile(filePositionRanges, content.length());

        LOGGER.info("File length: [{}], requested Splits: [{}], final splits: [{}]", content.length(), splits, filePositionRanges.length);
    }

    private static void assertThatPositionRangesCoverEntireFile(FilePositionRange[] positionRanges, long fileLength) {
        assertThat(positionRanges[0].getFromPosition(), equalTo(0L));


        int positionRangesLength = positionRanges.length;

        for (int i=0; i<positionRangesLength-1; i++) {
            assertThat(positionRanges[i].getToPosition(), lessThan(fileLength));
            assertThat(positionRanges[i].getToPosition(), equalTo(positionRanges[i+1].getFromPosition()));
        }

        assertThat(positionRanges[positionRangesLength-1].getToPosition(), greaterThanOrEqualTo(fileLength));
    }

    private static void addLineOfRandomCharactersToBuilder(StringBuilder builder, int numberOfCharacters, String lineTermination) {
        new Random().ints(numberOfCharacters, 'a', 'z').forEach(builder::append);
        builder.append(lineTermination);
    }
}
