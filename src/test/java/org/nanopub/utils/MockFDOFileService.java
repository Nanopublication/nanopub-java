package org.nanopub.utils;

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

public class MockFDOFileService {

    private static final Map<String, String> fdoNanopubs = new HashMap<>();

    protected MockFDOFileService() {
        Path FDO_PATH = Path.of(Objects.requireNonNull(this.getClass().getResource("/fdo")).getPath());
        try (Stream<Path> paths = Files.walk(FDO_PATH)) {
            paths.filter(Files::isRegularFile).forEach(this::processFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read mock files from: " + FDO_PATH, e);
        }
    }

    private void processFile(Path filePath) {
        try {
            Nanopub nanopub = new NanopubImpl(filePath.toFile());
            if (FdoUtils.isFdoNanopub(nanopub)) {
                Resource uriHandle = nanopub.getAssertion()
                        .stream().findFirst()
                        .orElseThrow()
                        .getSubject();
                fdoNanopubs.put(FdoUtils.extractHandle(uriHandle), filePath.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + filePath, e);
        } catch (MalformedNanopubException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the file path of the FDO nanopub corresponding to the given handle.
     *
     * @param handle the handle of the FDO nanopub
     * @return the file path of the FDO nanopub, or null if not found
     */
    public static String getFdoNanopubFromHandle(String handle) {
        return fdoNanopubs.get(handle);
    }

}