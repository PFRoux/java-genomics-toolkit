package edu.unc.genomics.dna;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import net.sf.picard.reference.FastaSequenceIndex;
import net.sf.picard.reference.FastaSequenceIndexEntry;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.util.StringUtil;

import org.apache.log4j.Logger;
import org.genomeview.dnaproperties.DNAProperty;

import com.beust.jcommander.Parameter;

import edu.unc.genomics.CommandLineTool;
import edu.unc.genomics.CommandLineToolException;
import edu.unc.genomics.ReadablePathValidator;
import edu.unc.utils.Samtools;

/**
 * This tool calculates DNA properties from a table lookup and creates a Wig file with
 * the property for each position
 * 
 * @author timpalpant
 *
 */
public class DNAPropertyCalculator extends CommandLineTool {
	
	private static final Logger log = Logger.getLogger(DNAPropertyCalculator.class);

	@Parameter(names = {"-i", "--input"}, description = "Input file (FASTA)", required = true, validateWith = ReadablePathValidator.class)
	public Path inputFile;
	@Parameter(names = {"-p", "--property"}, description = "DNA property to calculate", required = true)
	public String propertyName;
	@Parameter(names = {"-n", "--normalize"}, description = "Output normalized values")
	public boolean normalize = false;
	@Parameter(names = {"-o", "--output"}, description = "Output file (Wiggle)", required = true)
	public Path outputFile;
	
	@Override
	public void run() throws IOException {
		DNAProperty property = DNAProperty.create(propertyName);
		if (property == null) {
			log.error("Unknown DNA property: "+propertyName);
			throw new CommandLineToolException("Unknown DNA property: "+propertyName);
		}
		
		if (!IndexedFastaSequenceFile.canCreateIndexedFastaReader(inputFile.toFile())) {
			try {
				Samtools.indexFasta(inputFile);
			} catch (Exception e) {
				log.error("Error indexing FASTA file with samtools. Is samtools available in the PATH?");
				throw new CommandLineToolException("Error indexing FASTA file with samtools");
			}
		}
		
		// Open the FASTA file and its index
		IndexedFastaSequenceFile fasta = new IndexedFastaSequenceFile(inputFile.toFile());
		Path indexFile = inputFile.resolveSibling(inputFile.getFileName()+".fai");
		FastaSequenceIndex faidx = new FastaSequenceIndex(indexFile.toFile());
		
		try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())) {
			// Write the Wiggle track header
			writer.write("track type=wiggle_0 name='"+propertyName+"' description='"+propertyName+"'");
			writer.newLine();
			
			// Process each entry in the FASTA file in chunks
			for (FastaSequenceIndexEntry contig : faidx) {
				log.debug("Processing FASTA entry "+contig.getContig()+" (length = "+contig.getSize()+")");
				// Write the contig header to output
				writer.write("fixedStep chrom="+contig.getContig()+" start=1 step=1 span=1");
				writer.newLine();
				
				long start = 1;
				while (start <= contig.getSize()) {
					long stop = Math.min(start + DEFAULT_CHUNK_SIZE - 1, contig.getSize());
					log.debug("Processing chunk "+contig.getContig()+":"+start+"-"+stop);
					ReferenceSequence seq = fasta.getSubsequenceAt(contig.getContig(), start, stop);
					
					double[] values;
					if (normalize) {
						values = property.normalizedProfile(StringUtil.bytesToString(seq.getBases()));
					} else {
						values = property.profile(StringUtil.bytesToString(seq.getBases()));
					}
					
					for (double value : values) {
						writer.write(Float.toString((float)value));
						writer.newLine();
					}
					
					start = stop + 1;
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DNAPropertyCalculator().instanceMain(args);
	}

}
