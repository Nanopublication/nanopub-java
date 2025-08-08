package org.nanopub.utils;

import net.trustyuri.TrustyUriUtils;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mockStatic;

public class MockTrustyUriUtils implements AutoCloseable {

    private final MockedStatic<TrustyUriUtils> mockedStatic;

    public MockTrustyUriUtils() {
        this.mockedStatic = mockStatic(TrustyUriUtils.class);
    }

    public void isPotentialTrustyUri(String uri, boolean result) {
        this.mockedStatic.when(() -> TrustyUriUtils.isPotentialTrustyUri(uri)).thenReturn(result);
    }

    @Override
    public void close() {
        if (this.mockedStatic != null) {
            this.mockedStatic.close();
        }
    }

}
