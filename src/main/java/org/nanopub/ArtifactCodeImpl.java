package org.nanopub;

import net.trustyuri.ModuleDirectory;
import net.trustyuri.TrustyUriModule;
import net.trustyuri.TrustyUriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * An implementation of the ArtifactCode interface.
 */
public class ArtifactCodeImpl implements ArtifactCode {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactCodeImpl.class);

    private final String code;
    private final TrustyUriModule module;

    /**
     * Constructs an ArtifactCodeImpl with the given code.
     *
     * @param code the artifact code
     * @throws IllegalArgumentException if the code is not a valid artifact code
     */
    public ArtifactCodeImpl(String code) {
        if (TrustyUriUtils.isPotentialArtifactCode(code)) {
            this.code = code;
            this.module = ModuleDirectory.getModule(TrustyUriUtils.getModuleId(code));
        } else {
            logger.error("Invalid artifact code: {}", code);
            throw new IllegalArgumentException("Invalid artifact code: " + code);
        }
    }

    /**
     * Returns the artifact code as a string.
     *
     * @return the artifact code
     */
    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public TrustyUriModule getModule() {
        return this.module;
    }

    @Override
    public String toString() {
        return "ArtifactCode{" + "code='" + code + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArtifactCodeImpl that = (ArtifactCodeImpl) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

}
