package org.nanopub.extra.services;

public class FailedApiCallException extends Exception {

	public FailedApiCallException(Exception cause) {
		super(cause);
	}

}
