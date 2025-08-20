package org.nanopub.extra.setting;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.extra.server.GetNanopub;
import org.nanopub.utils.MockTrustyUriUtils;
import org.nanopub.utils.TestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class IntroExtractorTest {

    @Test
    void defaultConstructor() {
        IntroNanopub.IntroExtractor extractor = new IntroNanopub.IntroExtractor("userId");
        assertNotNull(extractor);
        assertNull(extractor.getName());
        assertNull(extractor.getIntroNanopub());
    }

    @Test
    void handleStatementSetsIntroNanopubWhenTrustyUriFound() throws RDFHandlerException, MalformedNanopubException {
        String trustyUri = "https://knowledgepixels.com/trustyUri";
        String userId = "https://knowledgepixels.com/userId";
        Statement statement = SimpleValueFactory.getInstance().createStatement(
                SimpleValueFactory.getInstance().createIRI(userId),
                FOAF.PAGE,
                SimpleValueFactory.getInstance().createIRI(trustyUri)
        );
        try (MockTrustyUriUtils mockedTrustyUriUtils = new MockTrustyUriUtils();
             MockedStatic<GetNanopub> mockedGetNanopub = mockStatic(GetNanopub.class)) {
            mockedTrustyUriUtils.isPotentialTrustyUri(anyString(), true);

            Nanopub nanopub = TestUtils.createNanopub();
            mockedGetNanopub.when(() -> GetNanopub.get("https://knowledgepixels.com/trustyUri")).thenReturn(nanopub);

            IntroNanopub.IntroExtractor extractor = new IntroNanopub.IntroExtractor(userId);
            extractor.handleStatement(statement);

            assertEquals(nanopub, extractor.getIntroNanopub());
            assertNull(extractor.getName());
        }
    }

    @Test
    void handleStatementSetsIntroNanopubWhenPredicateIsLabel() throws RDFHandlerException {
        String userId = "https://knowledgepixels.com/userId";
        String label = "https://knowledgepixels.com/labelUri";
        Statement statement = SimpleValueFactory.getInstance().createStatement(
                SimpleValueFactory.getInstance().createIRI(userId),
                RDFS.LABEL,
                SimpleValueFactory.getInstance().createIRI(label)
        );

        IntroNanopub.IntroExtractor extractor = new IntroNanopub.IntroExtractor(userId);
        extractor.handleStatement(statement);

        assertEquals(label, extractor.getName());
        assertNull(extractor.getIntroNanopub());
    }

}