package org.nanopub.extra.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * For testing purposes.
 */
public class UnreliableInputStream extends InputStream {

	private static final double errorProbability = 0.01;
	private static Random random = new Random();

	private InputStream wrappedInputStream;

	public UnreliableInputStream(InputStream wrappedInputStream) {
		this.wrappedInputStream = wrappedInputStream;
	}

	@Override
	public int read() throws IOException {
		if (random.nextDouble() < errorProbability) {
			return random.nextInt(256);
		}
		return wrappedInputStream.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		int r = wrappedInputStream.read(b);
		if (random.nextDouble() < errorProbability) {
			b[random.nextInt(b.length)] = (byte) random.nextInt(256);
		}
		return r;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int r = wrappedInputStream.read(b, off, len);
		if (random.nextDouble() < errorProbability && r > 0) {
			b[off + random.nextInt(r)] = (byte) random.nextInt(256);
		}
		return r;
	}

	@Override
	public int available() throws IOException {
		return wrappedInputStream.available();
	}

	@Override
	public void close() throws IOException {
		wrappedInputStream.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		wrappedInputStream.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return wrappedInputStream.markSupported();
	}

	@Override
	public synchronized void reset() throws IOException {
		wrappedInputStream.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return wrappedInputStream.skip(n);
	}

}
