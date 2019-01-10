package org.nanopub.extra.security;

public class MalformedCryptoElementException extends Exception {

	private static final long serialVersionUID = 3555738230396015129L;

	public MalformedCryptoElementException(String message) {
		super(message);
	}

	public MalformedCryptoElementException(Throwable cause) {
		super(cause);
	}

}
