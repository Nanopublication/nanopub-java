package org.nanopub;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.nanopub.vocabulary.KPXL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the RoCrateImporter and RoCrateParser against a local json-ld ro-crate-metadata file.
 */
public class RoCrateTest {

    final String roCrateUrl = "https://w3id.org/ro-id/7ad44bec-6784-437f-b5f3-2199b43a5303/";
    final String roCrateMetadataPath = Objects.requireNonNull(this.getClass().getResource("/")).getPath() + "7ad44bec-6784-437f-b5f3-2199b43a5303.jsonld";
    static final Logger log = LoggerFactory.getLogger(RoCrateTest.class);

    @Test
    void testParseRoCrateMetadata() throws Exception {
        Nanopub np = new RoCrateParser().parseRoCreate(roCrateUrl, new FileInputStream(roCrateMetadataPath));
        assertEquals(312, np.getTripleCount());
        List<Statement> typePred = np.getPubinfo().stream().filter(st -> st.getPredicate().equals(RDF.TYPE)).toList();
        assertEquals(1, typePred.size());
        assertEquals(KPXL.RO_CRATE_NANOPUB, typePred.getFirst().getObject());
    }

    @Test
    void testCommandLineWithExplicitLocalFile () throws Exception {
        RoCrateImporter ro = CliRunner.initJc(new RoCrateImporter(), new String[] {
                "-l",
                "-f", roCrateMetadataPath,
                roCrateUrl
        });
        ro.run();
    }

    @Test
    void testLogging() {

        // Source - https://stackoverflow.com/a/46052596
// Posted by user5845413
// Retrieved 2025-11-11, License - CC BY-SA 3.0

//        System.setProperty(org.slf4j.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "Info");
//        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
//        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");

        log.debug("xzyABC212 abc");
        if (log.isDebugEnabled()) {
            log.error("xzyABC212 debug-error");
            System.out.println("Sysout: xzyABC212 debug-error");
        }
        log.trace("xzyABC212 trace");
        log.error("xzyABC212 error");
        log.info("xzyABC212 info");
        System.out.println("Sysout xzyABC212 : end");
    }

    @Test
    void testCommandLineWithMockedMetadataDownload() {
        String mockedUrl = roCrateUrl + "ro-crate-metadata.json";
        IRI res = RoCrateParser.constructRoCrateUrl(mockedUrl, null);
        try (MockedStatic<RoCrateParser> staticMock = Mockito.mockStatic(RoCrateParser.class)) {
            staticMock.when(() -> RoCrateParser.downloadRoCreateMetadataFile(mockedUrl))
                    .thenReturn(new FileInputStream(roCrateMetadataPath));
            staticMock.when(() -> RoCrateParser.constructRoCrateUrl(Mockito.any(), Mockito.any()))
                    .thenReturn(res);
            RoCrateImporter ro = CliRunner.initJc(new RoCrateImporter(), new String[]{
                    "-l",
                    mockedUrl
            });
            ro.run();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertTrue(true);
    }

    @Test
    void testConstructRoCrateUrl() {
        // id #536707bd-bca4-42a1-b1ec-e9e3eab75073

        // https://rawcdn.githack.com/biocompute-objects/bco-ro-example-chipseq/76cb84c8d6a17a3fd7ae3102f68de3f780458601/data/
        // ro-crate-metadata.jsonld

        // https://zenodo.org/records/3541888/files/
        // ro-crate-metadata.jsonld

        // http://mod.paradisec.org.au/repository/72/b3/dc/14/01/c8/ff/06/aa/cb/a0/99/0a/12/8f/c1/13/cf/9a/d5/27/5f/49/4b/05/c1/14/21/77/35/65/61/bd/7f/4c/0e/88/00/ba/de/2c/bb/be/d7/5f/6d/9d/01/98/94/73/5a/d7/e4/07/62/68/4d/24/3a/44/2d/65/8a/v1/content/

        String suffix = "crate/download/";
        String baseDownloadUrl = "https://api.rohub.org/api/ros/";
        String id = "55a1b422-f279-4765-9ba7-d27268059844/";
        String fullUrl = baseDownloadUrl + id + suffix;
        IRI res = RoCrateParser.constructRoCrateUrl(fullUrl, null);
        assertEquals("https://w3id.org/ro-id/" + id, res.stringValue());
    }

    static final String BASE_ROHUB_URL = "https://w3id.org/ro-id/";

    @Test
    void testConstructRoHubApiUrl() {
        String roHubId = "302b4ebf-db38-49d5-8ab4-4561181f4e94";
        String downloadUrl = "https://api.rohub.org/api/ros/" + roHubId + "/crate/download/";
        IRI res = RoCrateParser.constructRoCrateUrl(downloadUrl, null);
        assertEquals(BASE_ROHUB_URL + roHubId + "/", res.stringValue());
    }

    @Test
    void testConstructSimpleRoCrateUrl() {
        String url = "https://zenodo.org/records/3541888/files/";
        String metadataUrl = "ro-crate-metadata.jsonld";
        IRI res = RoCrateParser.constructRoCrateUrl(url + metadataUrl, null);
        assertEquals(url, res.stringValue());
    }

    @Test
    void testConstructSimpleRoCrateUrlWithMetadataJustOneSlash() {
        String url = "https://zenodo.org/records/3541888/files/";
        String completeUrl = url + "/";
        IRI res = RoCrateParser.constructRoCrateUrl(url, null);
        assertEquals(url, res.stringValue());
    }

    @Test
    void testConstructSimpleRoCrateUrlWithMetadataSpecialCaseDoubleSlash() {
        // TODO discuss standard-conformity of this, ...//
        String url = "https://zenodo.org/records/3541888/files//";
        IRI res = RoCrateParser.constructRoCrateUrl(url, null);
        assertEquals(url, res.stringValue());
    }

    @Test
    void testConstructSimpleRoCrateUrlWithDotReferenceInPath() {
        String url = "https://abc.ziz/testrecord/./";
        IRI res = RoCrateParser.constructRoCrateUrl(url, null);
        assertEquals(url, res.stringValue());
    }

    @Test
    void testConstructNonIdRoCrateUrl() {
        String urlWithoutIdNorMetadata = "https://raw.githubusercontent.com/FAIR2Adapt/saarland-flooding/refs/heads/main/notebooks/get_typename_from_WFS.ipynb";
        String expectedUrlNoMetadata__ = "https://raw.githubusercontent.com/FAIR2Adapt/saarland-flooding/refs/heads/main/notebooks/";
        IRI res = RoCrateParser.constructRoCrateUrl(urlWithoutIdNorMetadata, null);
        assertEquals(expectedUrlNoMetadata__, res.stringValue());
    }

}
