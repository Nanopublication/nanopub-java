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
		double rd = random.nextDouble();
		if (rd < errorProbability / 2) {
			return random.nextInt(256);
		} else if (rd < errorProbability) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ex) {}
			throw new SimulatedIOException("Simulated IO Problem");
		}
		return wrappedInputStream.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		double rd = random.nextDouble();
		int r = wrappedInputStream.read(b);
		if (rd < errorProbability / 2) {
			b[random.nextInt(b.length)] = (byte) random.nextInt(256);
		} else if (rd < errorProbability) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ex) {}
			throw new SimulatedIOException("Simulated IO Problem");
		}
		return r;
	}

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
			} catch (InterruptedException ex) {}
			throw new SimulatedIOException("Simulated IO Problem");
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

	
	public class SimulatedIOException extends IOException {

		public SimulatedIOException(String text) {
			super(text);
		}

	}


}
