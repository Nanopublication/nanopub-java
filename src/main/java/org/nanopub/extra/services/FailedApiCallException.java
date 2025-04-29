package org.nanopub.extra.services;

public class FailedApiCallException extends Exception {

	private static final long serialVersionUID = 1L;

	public FailedApiCallException(Exception cause) {
		super(cause);
	}

}
