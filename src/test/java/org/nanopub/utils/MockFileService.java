package org.nanopub.utils;

import net.trustyuri.TrustyUriUtils;
import org.eclipse.rdf4j.model.Resource;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.fdo.FdoUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class MockFileService {

    private final Path TEST_SUITE = Path.of(Objects.requireNonNull(this.getClass().getResource("/testsuite")).getPath());
    private final Path FDOs = Path.of(Objects.requireNonNull(this.getClass().getResource("/fdo")).getPath());

    private static final Map<String, String> validAndSignedNanopubs = new HashMap<>();
    private static final Map<String, String> fdoNanopubs = new HashMap<>();

    protected MockFileService() {
        try (Stream<Path> paths = Files.walk(Path.of(TEST_SUITE + "/valid/signed"))) {
            paths.filter(Files::isRegularFile).forEach(this::processFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read mock files from: " + TEST_SUITE, e);
        }
        try (Stream<Path> paths = Files.walk(FDOs)) {
            paths.filter(Files::isRegularFile).forEach(this::processFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read mock files from: " + "/fdo", e);
        }
    }

    private void processFile(Path filePath) {
        try {
            Nanopub nanopub = new NanopubImpl(filePath.toFile());
            if (FdoUtils.isFdoNanopub(nanopub)) {
                Resource uriHandle = nanopub.getAssertion().stream().findFirst().get().getSubject();
                fdoNanopubs.put(FdoUtils.extractHandle(uriHandle), filePath.toString());
            }
            validAndSignedNanopubs.put(TrustyUriUtils.getArtifactCode(String.valueOf(nanopub.getUri())), filePath.toString());
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + filePath, e);
        } catch (MalformedNanopubException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getValidAndSignedNanopubFromId(String nanopubId) {
        return validAndSignedNanopubs.get(nanopubId);
    }

    public static String getFdoNanopubFromHandle(String handle) {
        return fdoNanopubs.get(handle);
    }

}