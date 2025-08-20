package org.nanopub.utils;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class MockFileServiceExtension implements BeforeAllCallback, AfterAllCallback {

    private static MockFileService service;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (service == null) {
            service = new MockFileService();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Optional cleanup if needed
    }

}
