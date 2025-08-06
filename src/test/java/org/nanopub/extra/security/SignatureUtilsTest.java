package org.nanopub.extra.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignatureUtilsTest {

    private final String pathSuffix = ".nanopub/";
    private final String fileNamePrefix = "id";

//    @Test
//    void getFullFilePathFromHomePath() {
//        String pathPrefix = "~/";
//        String relativeFilePathAndFileNamePrefix = pathPrefix + pathSuffix + fileNamePrefix;
//        String fullFilePath = SignatureUtils.getFullFilePath(relativeFilePathAndFileNamePrefix);
//        assertEquals((System.getProperty("user.home") + "/" + pathSuffix), fullFilePath);
//    }

    @Test
    void getFullFilePathWithFullPath() {
        String pathPrefix = "/home/user/";
        String fullFilePath = pathPrefix + pathSuffix + fileNamePrefix;
        String result = SignatureUtils.getFullFilePath(fullFilePath);
        assertEquals(fullFilePath, result);
    }

}