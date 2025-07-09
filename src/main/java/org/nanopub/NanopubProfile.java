package org.nanopub;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to read the nanopub profile file, which is a YAML file.
 */
public class NanopubProfile {
    // For writing profile file see:
    // https://stackoverflow.com/questions/24949505/how-do-i-write-to-a-yaml-file-using-snakeyaml

    public final static String IMPLICIT_PROFILE_FILE_NAME = System.getProperty("user.home") + "/.nanopub/profile.yaml";

    private Map<String, Object> map;

    /**
     * Constructor that reads the profile file from the default location.
     *
     * @param profileFileName the name of the profile file to read.
     */
    public NanopubProfile(String profileFileName) {
        File profileFile = new File(profileFileName);
        if (profileFile.exists()) {
            Yaml yaml = new Yaml();
            try (InputStream inputStream = new FileInputStream(profileFile)) {
                map = yaml.load(inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            map = new HashMap<>();
        }
    }

    /**
     * Returns the value of the private key in the profile.yaml file.
     *
     * @return the value of private_key in the profile.yaml, iff the file is there and the value is defined.
     * null otherwise.
     */
    public String getPrivateKeyPath() {
        return (String) map.get("private_key");
    }

    /**
     * Returns the value of the orcid_id in the profile.yaml file.
     *
     * @return the value of orcid_id in the profile.yaml, iff the file is there and the value is defined.
     * null otherwise.
     */
    public String getOrcidId() {
        return (String) map.get("orcid_id");
    }

}