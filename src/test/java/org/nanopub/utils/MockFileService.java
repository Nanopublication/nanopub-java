package org.nanopub.utils;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class MockFileService {

    private final Path TEST_SUITE = Path.of(Objects.requireNonNull(this.getClass().getResource("/testsuite")).getPath());
    private static final Map<String, String> validAndSignedNanopubs = new HashMap<>();

    public MockFileService() {
        try (Stream<Path> paths = Files.walk(Path.of(TEST_SUITE + "/valid/signed"))) {
            paths.filter(Files::isRegularFile)
                    .forEach(this::processFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read mock files from: " + TEST_SUITE, e);
        }
    }

    private void processFile(Path filePath) {
        try {
            Nanopub nanopub = new NanopubImpl(filePath.toFile());
            validAndSignedNanopubs.put(nanopub.getUri().getLocalName(), filePath.toString());

        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + filePath, e);
        } catch (MalformedNanopubException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getValidAndSignedNanopubFromId(String nanopubId) {
        return validAndSignedNanopubs.get(nanopubId);
    }

}