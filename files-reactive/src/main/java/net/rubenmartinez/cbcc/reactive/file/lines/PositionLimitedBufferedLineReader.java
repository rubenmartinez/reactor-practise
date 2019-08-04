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
 * This is a very drastic solution, copying JDK11's {@link java.io.BufferedReader} here, but I haven't found any class that made this
 * I could always write my own BufferedReader with this functionality, but JDK's one is already working and very tested, so why not.
 */

public class PositionLimitedBufferedLineReader extends Reader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionLimitedBufferedLineReader.class);

    private Reader in;

    private long totalCharReadCount;
    private long maxCharsToRead;

    private char cb[];
    private int nChars, nextChar;

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
        if (bufferSize <= 0)
            throw new IllegalArgumentException("Buffer size <= 0");
        this.in = in;
        cb = new char[bufferSize];
        nextChar = nChars = 0;


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
        if (in == null)
            throw new IOException("Stream closed");
    }

    /**
     * Fills the input buffer, taking the mark into account if it is valid.
     */
    private void fill() throws IOException {
        int n;
        do {
            n = in.read(cb, 0, cb.length);
        } while (n == 0);
        if (n > 0) {
            nChars = n;
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

        synchronized (lock) {
            ensureOpen();
            boolean omitLF = ignoreLF || skipLF;

            LOGGER.debug("readLine(): nextChar: {}, nChars: {}, totalCharReadCount: {}, maxCharsToRead: {}", nextChar, nChars, totalCharReadCount, maxCharsToRead);

            bufferLoop:
            for (;;) {

                if (nextChar >= nChars) {
                    fill();
                    LOGGER.debug("Filled buffer: omitLF: {}, skipLF: {}, nextChar: {}, nChars: {}, totalCharReadCount: {}, maxCharsToRead: {}", omitLF, skipLF, nextChar, nChars, totalCharReadCount, maxCharsToRead);
                }
                if (nextChar >= nChars || totalCharReadCount >= maxCharsToRead) { /* EOF or limit reached */
                    LOGGER.debug("EOF or limit reached: omitLF: {}, skipLF: {}, nextChar: {}, nChars: {}, totalCharReadCount: {}, maxCharsToRead: {}", omitLF, skipLF, nextChar, nChars, totalCharReadCount, maxCharsToRead);
                    if (s != null && s.length() > 0)
                        return s.toString();
                    else
                        return null;
                }
                boolean eol = false;
                char c = 0;
                int i;

                /* Skip a leftover '\n', if necessary */
                if (omitLF && (cb[nextChar] == '\n')) {
                    LOGGER.debug("Skipped leftover '\n': omitLF: {}, skipLF: {}, nextChar: {}, nChars: {}, totalCharReadCount: {}, maxCharsToRead: {}", omitLF, skipLF, nextChar, nChars, totalCharReadCount, maxCharsToRead);
                    totalCharReadCount++;
                    nextChar++;
                }
                skipLF = false;
                omitLF = false;

                charLoop:
                for (i = nextChar; i < nChars; i++) {
                    c = cb[i];
                    totalCharReadCount++;
                    if (totalCharReadCount >= maxCharsToRead) {
                        break charLoop;
                    }
                    if ((c == '\n') || (c == '\r')) {
                        eol = true;
                        break charLoop;
                    }
                    LOGGER.debug("In charLoop: omitLF: {}, skipLF: {}, nextChar: {}, nChars: {}, totalCharReadCount: {}, maxCharsToRead: {}", omitLF, skipLF, nextChar, nChars, totalCharReadCount, maxCharsToRead);
                }
                startChar = nextChar;
                nextChar = i;
                LOGGER.debug("After charLoop: omitLF: {}, skipLF: {}, nextChar: {}, nChars: {}, totalCharReadCount: {}, maxCharsToRead: {}", omitLF, skipLF, nextChar, nChars, totalCharReadCount, maxCharsToRead);

                if (eol) {
                    LOGGER.debug("EOL: omitLF: {}, skipLF: {}, nextChar: {}, nChars: {}, totalCharReadCount: {}, maxCharsToRead: {}, startChar: {}", omitLF, skipLF, nextChar, nChars, totalCharReadCount, maxCharsToRead, startChar);
                    String str;
                    if (s == null) {
                        str = new String(cb, startChar, i - startChar);
                    } else {
                        s.append(cb, startChar, i - startChar);
                        str = s.toString();
                    }
                    totalCharReadCount++;
                    nextChar++;
                    if (c == '\r') {
                        skipLF = true;
                    }
                    return str;
                }

                if (s == null)
                    s = new StringBuffer(defaultExpectedLineLength);
                s.append(cb, startChar, i - startChar);
            }
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
     * Tells whether this stream is ready to be read.  A buffered character
     * stream is ready if the buffer is not empty, or if the underlying
     * character stream is ready.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public boolean ready() throws IOException {
        synchronized (lock) {
            ensureOpen();

            /*
             * If newline needs to be skipped and the next char to be read
             * is a newline character, then just skip it right away.
             */
            if (skipLF) {
                /* Note that in.ready() will return true if and only if the next
                 * read on the stream will not block.
                 */
                if (nextChar >= nChars && in.ready()) {
                    fill();
                }
                if (nextChar < nChars) {
                    if (cb[nextChar] == '\n')
                        nextChar++;
                    skipLF = false;
                }
            }
            return (nextChar < nChars) || in.ready();
        }
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
            if (in == null)
                return;
            try {
                in.close();
            } finally {
                in = null;
                cb = null;
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
     * {@code BufferedReader}, it is wrapped in an {@link
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
