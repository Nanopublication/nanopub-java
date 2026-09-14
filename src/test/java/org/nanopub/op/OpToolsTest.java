package org.nanopub.op;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.utils.TestUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.nanopub.utils.TestUtils.anyIri;
import static org.nanopub.utils.TestUtils.vf;

/**
 * Drives each command-line tool in this package through its {@code execute}
 * method, which returns the exit code instead of ending the JVM.
 * <p>
 * The tools share a shape — read something, write something out — so they are
 * covered here together rather than in seventeen near-identical test classes.
 */
class OpToolsTest {

    /**
     * What a tool expects to be pointed at.
     */
    private enum Input {
        /**
         * Plain nanopubs, which is what most of the tools read.
         */
        NANOPUBS,
        /**
         * Nanopubs with a trusty URI, which Tar needs to name its archive
         * entries.
         */
        TRUSTY_NANOPUBS,
        /**
         * An ordinary RDF document, which Build turns into nanopubs.
         */
        PLAIN_RDF,
        /**
         * The line-based cache that Reuse writes with -c, and that IndexReuse
         * reads.
         */
        NANOPUB_CACHE,
        /**
         * Nothing: the tool takes no input file.
         */
        NONE
    }

    /**
     * One tool, with the input it expects and the arguments that make it do its
     * job. The argument function is handed the input file and the directory to
     * write into.
     */
    private record Tool(String name,
            Input input,
            BiFunction<File, File, String[]> arguments,
            Function<String[], Integer> execute) {

        @Override
        public String toString() {
            return name;
        }
    }

    private static File out(File dir, String name) {
        return new File(dir, name);
    }

    static Stream<Tool> tools() {
        return Stream.of(
                new Tool("Aggregate", Input.NANOPUBS, (in, dir) -> new String[]{
            "-a", out(dir, "assertion.txt").getPath(), in.getPath()}, Aggregate::execute),
                new Tool("Build", Input.PLAIN_RDF, (in, dir) -> new String[]{
            "-o", out(dir, "built.trig").getPath(), in.getPath()}, Build::execute),
                new Tool("Count", Input.NANOPUBS, (in, dir) -> new String[]{
            "-r", out(dir, "counts.txt").getPath(), in.getPath()}, Count::execute),
                new Tool("Create", Input.NONE, (in, dir) -> new String[]{
            "-o", out(dir, "created.trig").getPath()}, Create::execute),
                new Tool("Decontextualize", Input.NANOPUBS, (in, dir) -> new String[]{
            "-o", out(dir, "plain.trig").getPath(), in.getPath()}, Decontextualize::execute),
                new Tool("ExportJson", Input.NANOPUBS, (in, dir) -> new String[]{
            "-o", out(dir, "export.json").getPath(), in.getPath()}, ExportJson::execute),
                new Tool("Extract", Input.NANOPUBS, (in, dir) -> new String[]{
            "-a", "-o", out(dir, "assertions.trig").getPath(), in.getPath()}, Extract::execute),
                new Tool("Filter", Input.NANOPUBS, (in, dir) -> new String[]{
            "-o", out(dir, "filtered.trig").getPath(), in.getPath()}, Filter::execute),
                new Tool("Fingerprint", Input.NANOPUBS, (in, dir) -> new String[]{
            "-o", out(dir, "fingerprints.txt").getPath(), in.getPath()}, Fingerprint::execute),
                new Tool("Gml", Input.NANOPUBS, (in, dir) -> new String[]{
            "-o", out(dir, "graph.gml").getPath(), in.getPath()}, Gml::execute),
                new Tool("Namespaces", Input.NANOPUBS, (in, dir) -> new String[]{
            "-a", out(dir, "namespaces.txt").getPath(), in.getPath()}, Namespaces::execute),
                new Tool("Tar", Input.TRUSTY_NANOPUBS, (in, dir) -> new String[]{
            "-o", out(dir, "nanopubs.tar").getPath(), in.getPath()}, Tar::execute),
                new Tool("Topic", Input.NANOPUBS, (in, dir) -> new String[]{
            "-o", out(dir, "topics.txt").getPath(), in.getPath()}, Topic::execute),
                new Tool("Union", Input.NANOPUBS, (in, dir) -> new String[]{
            "-o", out(dir, "union.trig").getPath(), in.getPath()}, Union::execute),
                // without -x, Reuse and IndexReuse create an initial dataset rather than reusing one
                new Tool("Reuse", Input.NANOPUBS, (in, dir) -> new String[]{
            "-o", out(dir, "reused.trig").getPath(),
            "-c", out(dir, "cache.txt").getPath(), in.getPath()}, Reuse::execute),
                new Tool("IndexReuse", Input.NANOPUB_CACHE, (in, dir) -> new String[]{
            "-o", out(dir, "index.trig").getPath(), in.getPath()}, IndexReuse::execute)
        );
    }

    private Map<Input, File> inputs;

    @BeforeEach
    void writeInputs(@TempDir File tempDir) throws Exception {
        inputs = new EnumMap<>(Input.class);
        inputs.put(Input.NANOPUBS, write(tempDir, "nanopubs.trig",
                () -> nanopub("https://example.org/np1#", "first").writeToString(RDFFormat.TRIG)
                + nanopub("https://example.org/np2#", "second").writeToString(RDFFormat.TRIG)));
        inputs.put(Input.TRUSTY_NANOPUBS, write(tempDir, "trusty.trig",
                () -> trustyNanopub("first").writeToString(RDFFormat.TRIG)
                + trustyNanopub("second").writeToString(RDFFormat.TRIG)));
        inputs.put(Input.PLAIN_RDF, write(tempDir, "plain.ttl", () -> """
                @prefix ex: <https://example.org/> .
                ex:s1 ex:p "one" .
                ex:s1 ex:q "two" .
                ex:s2 ex:p "three" .
                """));
        // IndexReuse consumes the cache that Reuse produces, so the tools are chained here
        // the same way they are on the command line
        File cache = new File(tempDir, "cache.txt");
        assertEquals(0, quietly(() -> Reuse.execute(new String[]{
            "-c", cache.getPath(),
            "-o", new File(tempDir, "reused.trig").getPath(),
            inputs.get(Input.NANOPUBS).getPath()})),
                "the cache fixture could not be built");
        inputs.put(Input.NANOPUB_CACHE, cache);

        // the tools that take no input are still handed a file, which they ignore
        inputs.put(Input.NONE, inputs.get(Input.NANOPUBS));
    }

    private interface ContentSupplier {

        String get() throws Exception;
    }

    private static File write(File dir, String name, ContentSupplier content) throws Exception {
        File file = new File(dir, name);
        Files.writeString(file.toPath(), content.get(), StandardCharsets.UTF_8);
        return file;
    }

    private static Nanopub nanopub(String uri, String label) throws Exception {
        return creatorFor(uri, label).finalizeNanopub();
    }

    private static Nanopub trustyNanopub(String label) throws Exception {
        return creatorFor("http://purl.org/nanopub/temp/" + label + "/", label).finalizeTrustyNanopub();
    }

    private static NanopubCreator creatorFor(String uri, String label) throws Exception {
        NanopubCreator creator = TestUtils.getNanopubCreator(uri);
        creator.addAssertionStatement(anyIri, RDFS.LABEL, vf.createLiteral(label));
        creator.addProvenanceStatement(anyIri, anyIri);
        creator.addPubinfoStatement(anyIri, anyIri);
        return creator;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tools")
    void succeedsOnAWellFormedInput(Tool tool, @TempDir File outputDir) {
        String[] args = tool.arguments().apply(inputs.get(tool.input()), outputDir);

        assertEquals(0, quietly(() -> tool.execute().apply(args)),
                tool + " should have completed successfully");
        assertTrue(outputDir.listFiles() != null && outputDir.listFiles().length > 0,
                tool + " should have written something");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tools")
    void failsOnAnUnknownOption(Tool tool, @TempDir File outputDir) {
        String[] args = tool.arguments().apply(inputs.get(tool.input()), outputDir);
        String[] withBadOption = new String[args.length + 1];
        withBadOption[0] = "--no-such-option";
        System.arraycopy(args, 0, withBadOption, 1, args.length);

        assertEquals(1, quietly(() -> tool.execute().apply(withBadOption)),
                tool + " should have rejected the unknown option");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tools")
    void failsOnAnUnreadableInput(Tool tool, @TempDir File outputDir) {
        assumeTakesAnInput(tool);
        File missing = new File(outputDir, "does-not-exist.trig");

        assertEquals(1, quietly(() -> tool.execute().apply(tool.arguments().apply(missing, outputDir))),
                tool + " should have reported the missing input");
    }

    private static void assumeTakesAnInput(Tool tool) {
        org.junit.jupiter.api.Assumptions.assumeTrue(tool.input() != Input.NONE,
                tool + " takes no input file");
    }

    @org.junit.jupiter.api.Test
    void importRejectsAnUnknownType(@TempDir File outputDir) {
        // only "cedar" is supported, and the type is required
        String[] args = {"-t", "no-such-type", "-o", out(outputDir, "imported.trig").getPath(),
            inputs.get(Input.NANOPUBS).getPath()};

        assertEquals(1, quietly(() -> Import.execute(args)));
    }

    @org.junit.jupiter.api.Test
    void importNeedsExactlyOneInputFile(@TempDir File outputDir) {
        File input = inputs.get(Input.NANOPUBS);
        String[] args = {"-t", "cedar", input.getPath(), input.getPath()};

        assertEquals(1, quietly(() -> Import.execute(args)));
    }

    /**
     * Runs the tool with its output swallowed, so that a passing test run stays
     * readable.
     */
    private static int quietly(Supplier<Integer> action) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
            return action.get();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

}
