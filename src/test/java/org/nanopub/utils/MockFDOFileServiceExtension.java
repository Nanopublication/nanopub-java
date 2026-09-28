package org.nanopub.utils;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class MockFDOFileServiceExtension implements BeforeAllCallback, AfterAllCallback {

    private static MockFDOFileService service;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (service == null) {
            service = new MockFDOFileService();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Optional cleanup if needed
    }

}
