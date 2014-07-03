package org.nanopub.trusty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.trustyuri.TrustyUriException;
import net.trustyuri.TrustyUriResource;
import net.trustyuri.rdf.RdfFileContent;
import net.trustyuri.rdf.RdfUtils;
import net.trustyuri.rdf.TransformRdf;

import org.nanopub.MalformedNanopubException;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubRdfHandler;
import org.nanopub.NanopubUtils;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class MakeTrustyNanopub {

	@com.beust.jcommander.Parameter(description = "input-nanopubs", required = true)
	private List<File> inputNanopubs = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-m", description = "Multi mode (for files containing multiple nanopubs)")
	private boolean multiMode = false;

	public static void main(String[] args) {
		MakeTrustyNanopub obj = new MakeTrustyNanopub();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		try {
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private void run() throws IOException, RDFParseException, RDFHandlerException,
			MalformedNanopubException, TrustyUriException {
		for (File inputFile : inputNanopubs) {
			if (multiMode) {
				String path = "";
				if (inputFile.getParent() != null) {
					path = inputFile.getParent() + "/";
				}
				OutputStream out = new FileOutputStream(path + "trusty." + inputFile.getName());
				TrustyUriResource r = new TrustyUriResource(inputFile);
				RDFFormat format = r.getFormat(RDFFormat.TRIG);
				transformMultiNanopub(format, inputFile, out);
			} else {
				TrustyUriResource r = new TrustyUriResource(inputFile);
				RdfFileContent content = RdfUtils.load(r);
				Nanopub nanopub = null;
				try {
					nanopub = new NanopubImpl(content.getStatements());
				} catch (MalformedNanopubException ex) {
					System.out.println("ERROR: Malformed nanopub: " + ex.getMessage());
					System.exit(1);
				}
				TransformRdf.transform(content, inputFile.getParent(), nanopub.getUri().toString());
			}
		}
	}

	public static Nanopub transform(Nanopub nanopub) throws TrustyUriException {
		Nanopub np;
		try {
			RdfFileContent r = new RdfFileContent(RDFFormat.TRIG);
			NanopubUtils.propagateToHandler(nanopub, r);
			NanopubRdfHandler h = new NanopubRdfHandler();
			TransformRdf.transform(r, h, nanopub.getUri().toString());
			np = h.getNanopub();
		} catch (RDFHandlerException ex) {
			throw new TrustyUriException(ex);
		} catch (MalformedNanopubException ex) {
			throw new TrustyUriException(ex);
		}
		return np;
	}

	public static void transformMultiNanopub(final RDFFormat format, File file, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		InputStream in = new FileInputStream(file);
		transformMultiNanopub(format, in, out);
	}

	public static void transformMultiNanopub(final RDFFormat format, InputStream in, final OutputStream out)
			throws IOException, RDFParseException, RDFHandlerException, MalformedNanopubException {
		MultiNanopubRdfHandler.process(format, in, new NanopubHandler() {

			@Override
			public void handleNanopub(Nanopub np) {
				try {
					np = MakeTrustyNanopub.transform(np);
					RDFWriter w = Rio.createWriter(format, out);
					NanopubUtils.propagateToHandler(np, w);
				} catch (RDFHandlerException ex) {
					throw new RuntimeException(ex);
				} catch (TrustyUriException ex) {
					throw new RuntimeException(ex);
				}
			}

		});
		out.close();
	}

}
