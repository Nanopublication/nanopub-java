package org.nanopub.jelly;

import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.types.Binary;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.utils.TestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NanopubStreamTest {

    /**
     * A cursor over the given documents, which is all {@link NanopubStream} asks of Mongo.
     */
    private static MongoCursor<Document> cursorOver(List<Document> documents) {
        Iterator<Document> iterator = documents.iterator();
        @SuppressWarnings("unchecked")
        MongoCursor<Document> cursor = mock(MongoCursor.class);
        when(cursor.hasNext()).thenAnswer(invocation -> iterator.hasNext());
        when(cursor.next()).thenAnswer(invocation -> iterator.next());
        // Iterator.forEachRemaining is a default method, which a mock would otherwise turn into a
        // no-op — and that is exactly what the stream uses to drain the cursor.
        doCallRealMethod().when(cursor).forEachRemaining(any());
        return cursor;
    }

    private static Document documentFor(Nanopub nanopub) {
        return new Document("jelly", new Binary(JellyUtils.writeNanopubForDB(nanopub)));
    }

    private static Document documentFor(Nanopub nanopub, long counter) {
        return documentFor(nanopub).append("counter", counter);
    }

    private static Nanopub nanopub(String uri) throws Exception {
        return TestUtils.createNanopub(uri);
    }

    /**
     * Writes the stream out and reads it back, which is how the registry ships nanopubs around.
     */
    private static List<MaybeNanopub> roundTrip(NanopubStream stream) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        stream.writeToByteStream(out);

        return NanopubStream.fromByteStream(new ByteArrayInputStream(out.toByteArray()))
                .getAsNanopubs()
                .toList();
    }

    @Test
    void streamsNanopubsFromAMongoCursor() throws Exception {
        Nanopub first = nanopub("https://example.org/np1#");
        Nanopub second = nanopub("https://example.org/np2#");

        List<MaybeNanopub> result = roundTrip(NanopubStream.fromMongoCursor(
                cursorOver(List.of(documentFor(first), documentFor(second)))));

        assertEquals(2, result.size());
        assertTrue(result.get(0).isSuccess());
        assertEquals(first.getUri(), result.get(0).getNanopub().getUri());
        assertEquals(second.getUri(), result.get(1).getNanopub().getUri());
        // no counter metadata was written, so none comes back
        assertEquals(-1, result.get(0).getCounter());
    }

    @Test
    void streamsNanopubsWithTheirCounter() throws Exception {
        Nanopub first = nanopub("https://example.org/np1#");
        Nanopub second = nanopub("https://example.org/np2#");

        List<MaybeNanopub> result = roundTrip(NanopubStream.fromMongoCursorWithCounter(
                cursorOver(List.of(documentFor(first, 7), documentFor(second, 8)))));

        assertEquals(2, result.size());
        assertEquals(7, result.get(0).getCounter());
        assertEquals(8, result.get(1).getCounter());
        assertEquals(first.getUri(), result.get(0).getNanopub().getUri());
    }

    @Test
    void streamsNothingFromAnEmptyCursor() {
        assertTrue(roundTrip(NanopubStream.fromMongoCursor(cursorOver(List.of()))).isEmpty());
    }

    @Test
    void reportsUnparseableJellyContentFromTheDatabase() {
        Document broken = new Document("jelly", new Binary(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));
        NanopubStream stream = NanopubStream.fromMongoCursor(cursorOver(List.of(broken)));

        assertThrows(RuntimeException.class, () -> stream.writeToByteStream(new ByteArrayOutputStream()));
    }

    @Test
    void reportsUnparseableJellyContentWhenReadingWithCounters() {
        Document broken = new Document("jelly", new Binary(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}))
                .append("counter", 1L);
        NanopubStream stream = NanopubStream.fromMongoCursorWithCounter(cursorOver(List.of(broken)));

        assertThrows(RuntimeException.class, () -> stream.writeToByteStream(new ByteArrayOutputStream()));
    }

    @Test
    void reportsAFailureWhileReadingTheByteStream() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("connection reset");
            }
        };

        NanopubStream stream = NanopubStream.fromByteStream(failing);

        assertThrows(RuntimeException.class, () -> stream.getAsNanopubs().toList());
    }

    @Test
    void reportsAFailureWhileWritingTheByteStream() throws Exception {
        NanopubStream stream = NanopubStream.fromMongoCursor(
                cursorOver(List.of(documentFor(nanopub("https://example.org/np1#")))));
        OutputStream failing = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("disk full");
            }
        };

        assertThrows(RuntimeException.class, () -> stream.writeToByteStream(failing));
    }

    @Test
    void readingAnEmptyByteStreamYieldsNothing() {
        NanopubStream stream = NanopubStream.fromByteStream(new ByteArrayInputStream(new byte[0]));

        assertTrue(stream.getAsNanopubs().toList().isEmpty());
    }

    @Test
    void reportsFramesThatDoNotHoldAWholeNanopub() {
        // valid Jelly, but the statements in it do not add up to a nanopub
        JellyWriterRDFHandler handler = new JellyWriterRDFHandler(JellyUtils.jellyOptionsForDB);
        handler.startRDF();
        handler.handleStatement(TestUtils.vf.createStatement(TestUtils.anyIri, TestUtils.anyIri,
                TestUtils.anyIri, TestUtils.anyIri));
        handler.endRDF();
        Document document = new Document("jelly", new Binary(handler.getFrame().toByteArray()));

        List<MaybeNanopub> result = roundTrip(NanopubStream.fromMongoCursor(cursorOver(List.of(document))));

        assertEquals(1, result.size());
        assertTrue(result.getFirst().isFailure());
        assertInstanceOf(MalformedNanopubException.class, result.getFirst().getException());
        assertNull(result.getFirst().getNanopub());
    }

}
