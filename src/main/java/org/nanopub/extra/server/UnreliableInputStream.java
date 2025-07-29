package org.nanopub.extra.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * An InputStream that simulates unreliable behavior, for testing purposes.
 */
public class UnreliableInputStream extends InputStream {

    private static final double errorProbability = 0.01;
    private static Random random = new Random();

    private InputStream wrappedInputStream;

    /**
     * Constructs an UnreliableInputStream that wraps the given InputStream.
     *
     * @param wrappedInputStream the InputStream to wrap
     */
    public UnreliableInputStream(InputStream wrappedInputStream) {
        this.wrappedInputStream = wrappedInputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        double rd = random.nextDouble();
        if (rd < errorProbability / 2) {
            return random.nextInt(256);
        } else if (rd < errorProbability) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
            }
            throw new SimulatedIOException("Simulated IO Problem");
        }
        return wrappedInputStream.read();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b) throws IOException {
        double rd = random.nextDouble();
        int r = wrappedInputStream.read(b);
        if (rd < errorProbability / 2) {
            b[random.nextInt(b.length)] = (byte) random.nextInt(256);
        } else if (rd < errorProbability) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
            }
            throw new SimulatedIOException("Simulated IO Problem");
        }
        return r;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        double rd = random.nextDouble();
        int r = wrappedInputStream.read(b, off, len);
        if (rd < errorProbability / 2) {
            if (r > 0) {
                b[off + random.nextInt(r)] = (byte) random.nextInt(256);
            }
        } else if (rd < errorProbability) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
            }
            throw new SimulatedIOException("Simulated IO Problem");
        }
        return r;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        return wrappedInputStream.available();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        wrappedInputStream.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void mark(int readlimit) {
        wrappedInputStream.mark(readlimit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markSupported() {
        return wrappedInputStream.markSupported();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void reset() throws IOException {
        wrappedInputStream.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long skip(long n) throws IOException {
        return wrappedInputStream.skip(n);
    }


    /**
     * An IOException that simulates an IO problem for testing purposes.
     */
    public class SimulatedIOException extends IOException {

        /**
         * Constructs a SimulatedIOException with the specified detail message.
         *
         * @param text the detail message
         */
        public SimulatedIOException(String text) {
            super(text);
        }

    }


}
