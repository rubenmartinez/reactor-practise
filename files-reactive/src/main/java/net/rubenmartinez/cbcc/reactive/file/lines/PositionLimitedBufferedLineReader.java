/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package net.rubenmartinez.cbcc.reactive.file.lines;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link Reader} that allows reading lines limiting the maximum number of characters read.
 *
 * This is a very drastic solution, modifying JDK11's {@link java.io.BufferedReader} here, but I haven't found any class that made this
 * I could always write my own BufferedReader with this functionality, but JDK's one is already working and very tested, so why not.
 *
 * Also this version doesn't use locks for efficiency, so it shouldn't be used by different threads at the same time.
 *
 * It has also other changes in favour of XXX legibility though (simplifications for logic not needed, variable names..) although it is still not quite clean
 */

public class PositionLimitedBufferedLineReader extends Reader {

    private Reader inReader;

    private long totalCharReadCount;
    private long maxCharsToRead;

    private char charBuffer[];
    private int charsInBuffer, nextChar;

    private static final int INVALIDATED = -2;

    /** If the next character is a line feed, skip it */
    private boolean skipLF = false;

    private static int defaultCharBufferSize = 8192;
    private static int defaultExpectedLineLength = 80;

    /**
     * Creates a buffering character-input stream that uses an input buffer of
     * the specified size, limiting reading to as much as {@code maxCharsToRead} bytes
     *
     * @param  in   A Reader
     * @param  maxCharsToRead   Maximum bytes to read
     * @param  bufferSize   Input-buffer size
     *
     * @exception  IllegalArgumentException  If {@code sz <= 0}
     */
    public PositionLimitedBufferedLineReader(Reader in, int bufferSize, long maxCharsToRead) {
        super(in);
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }

        this.inReader = in;
        charBuffer = new char[bufferSize];
        nextChar = charsInBuffer = 0;

        this.totalCharReadCount = 0;
        this.maxCharsToRead = maxCharsToRead;
    }

    /**
     * Creates a buffering character-input stream that uses a default-sized
     * input buffer, limiting reading to as much as {@code maxCharsToRead} bytes
     *
     * @param  in   A Reader
     * @param  maxCharsToRead   Maximum bytes to read
     */
    public PositionLimitedBufferedLineReader(Reader in, long maxCharsToRead) {
        this(in, defaultCharBufferSize, maxCharsToRead);
    }

    /** Checks to make sure that the stream has not been closed */
    private void ensureOpen() throws IOException {
        if (inReader == null)
            throw new IOException("Stream closed");
    }

    /**
     * Fills the input buffer, taking the mark into account if it is valid.
     */
    private void fill() throws IOException {
        int n;
        do {
            n = inReader.read(charBuffer, 0, charBuffer.length);
        } while (n == 0);
        if (n > 0) {
            charsInBuffer = n;
            nextChar = 0;
        }
    }

    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), a carriage return
     * followed immediately by a line feed, or by reaching the end-of-file
     * (EOF).
     *
     * @param      ignoreLF  If true, the next '\n' will be skipped
     *
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached without reading any characters
     *
     * @see        java.io.LineNumberReader#readLine()
     *
     * @exception  IOException  If an I/O error occurs
     */
    String readLine(boolean ignoreLF) throws IOException {
        StringBuffer s = null;
        int startChar;

        ensureOpen();
        boolean omitLF = ignoreLF || skipLF;

        bufferLoop:
        for (;;) {

            if (nextChar >= charsInBuffer) {
                fill();
            }
            if (nextChar >= charsInBuffer || totalCharReadCount > maxCharsToRead) { /* EOF or limit reached */
                if (s != null && s.length() > 0)
                    return s.toString();
                else
                    return null;
            }
            boolean eol = false;
            char c = 0;
            int i;

            /* Skip a leftover '\n', if necessary */
            if (omitLF && (charBuffer[nextChar] == '\n')) {
                totalCharReadCount++;
                nextChar++;
            }
            skipLF = false;
            omitLF = false;

            charLoop:
            for (i = nextChar; i < charsInBuffer; i++) {
                c = charBuffer[i];
                totalCharReadCount++;
                //LOGGER.debug("In charLoop (c={}): omitLF: {}, skipLF: {}, nextChar: {}, charsInBuffer: {}, totalCharReadCount: {}, maxCharsToRead: {}", c, omitLF, skipLF, nextChar, charsInBuffer, totalCharReadCount, maxCharsToRead);
                if (totalCharReadCount > maxCharsToRead) {
                    break charLoop;
                }
                if ((c == '\n') || (c == '\r')) {
                    eol = true;
                    break charLoop;
                }
            }
            startChar = nextChar;
            nextChar = i;

            if (eol) {
                String str;
                if (s == null) {
                    str = new String(charBuffer, startChar, i - startChar);
                } else {
                    s.append(charBuffer, startChar, i - startChar);
                    str = s.toString();
                }
                nextChar++;
                if (c == '\r') {
                    skipLF = true;
                }
                return str;
            }

            if (s == null)
                s = new StringBuffer(defaultExpectedLineLength);
            s.append(charBuffer, startChar, i - startChar);
        }
    }

    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), a carriage return
     * followed immediately by a line feed, or by reaching the end-of-file
     * (EOF).
     *
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached without reading any characters
     *
     * @exception  IOException  If an I/O error occurs
     *
     * @see java.nio.file.Files#readAllLines
     */
    public String readLine() throws IOException {
        return readLine(false);
    }

    /**
     * Not suported
     *
     * @exception  UnsupportedOperationException  always
     */
    public long skip(long n) throws IOException {
        throw new UnsupportedOperationException();
    }


    /**
     * Not suported
     *
     * @exception  UnsupportedOperationException  always
     */
    public int read() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not suported
     *
     * @exception  UnsupportedOperationException  always
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not suported
     *
     * @exception  UnsupportedOperationException  always
     */
    public boolean ready() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not suported
     *
     * @exception  UnsupportedOperationException  always
     */
    public boolean markSupported() {
        throw new UnsupportedOperationException();
    }

    /**
     * Not suported
     *
     * @exception  UnsupportedOperationException  always
     */
    public void mark(int readAheadLimit) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not suported
     *
     * @exception  UnsupportedOperationException  always
     */
    public void reset() throws IOException {
        throw new UnsupportedOperationException();
    }

    public void close() throws IOException {
        synchronized (lock) {
            if (inReader == null)
                return;
            try {
                inReader.close();
            } finally {
                inReader = null;
                charBuffer = null;
            }
        }
    }

    /**
     * Returns a {@code Stream}, the elements of which are lines read from
     * this {@code BufferedReader}.  The {@link Stream} is lazily populated,
     * i.e., read only occurs during the
     * <a href="../util/stream/package-summary.html#StreamOps">terminal
     * stream operation</a>.
     *
     * <p> The reader must not be operated on during the execution of the
     * terminal stream operation. Otherwise, the result of the terminal stream
     * operation is undefined.
     *
     * <p> After execution of the terminal stream operation there are no
     * guarantees that the reader will be at a specific position from which to
     * read the next character or line.
     *
     * <p> If an {@link IOException} is thrown when accessing the underlying
     * {@code BufferedReader}, it is wrapped inReader an {@link
     * UncheckedIOException} which will be thrown from the {@code Stream}
     * method that caused the read to take place. This method will return a
     * Stream if invoked on a BufferedReader that is closed. Any operation on
     * that stream that requires reading from the BufferedReader after it is
     * closed, will cause an UncheckedIOException to be thrown.
     *
     * @return a {@code Stream<String>} providing the lines of text
     *         described by this {@code BufferedReader}
     *
     * @since 1.8
     */
    public Stream<String> lines() {
        Iterator<String> iter = new Iterator<>() {
            String nextLine = null;

            @Override
            public boolean hasNext() {
                if (nextLine != null) {
                    return true;
                } else {
                    try {
                        nextLine = readLine();
                        return (nextLine != null);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }

            @Override
            public String next() {
                if (nextLine != null || hasNext()) {
                    String line = nextLine;
                    nextLine = null;
                    return line;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iter, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }
}
