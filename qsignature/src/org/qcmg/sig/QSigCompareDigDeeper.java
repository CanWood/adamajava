/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerArray;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.sig.util.SignatureUtil;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class QSigCompareDigDeeper {
	
	private static QLogger logger;
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	private final static int maxCacheSize = 10;
	
	private int minCoverage = 20;
	private float cutoff = 0.035f;
	
	private final  List<File> vcfFiles = new ArrayList<File>();
	private final Map<File, String[]> fileStats = new HashMap<File, String[]>();
	
	private static final List<ChrPosition> snps = new ArrayList<ChrPosition>();
	
	private Map<ChrPosition, int[]> currentFileRatios = new HashMap<ChrPosition, int[]>();
	private final Map<File, Map<ChrPosition, int[]>> fileRatiosCache = new HashMap<File, Map<ChrPosition, int[]>>();
	private final List<String[]> displayResults = new ArrayList<String[]>();
	
	private int engage() throws Exception {
		
		// populate the random SNP Positions map
		for (String s : cmdLineInputFiles) loadVcfFiles(s);
		
		if ( ! vcfFiles.isEmpty() && vcfFiles.size() > 1) {
			// lets go
			selectFilesForComparison();
			
			// log some stats:
			logger.info("SUMMARY STATS:");
			for (String [] stat : displayResults) {
				logger.info(Arrays.deepToString(stat));
			}
			
			// write xml output
			writeOutput();
		} else {
			if (vcfFiles.isEmpty())
				logger.error("No vcf files found in : " + Arrays.deepToString(cmdLineInputFiles));
			else
				logger.error("Only one vcf file found in : " + Arrays.deepToString(cmdLineInputFiles));
		}
		
		return exitStatus;
	}
	
	private void writeOutput() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error("Couldn't create new Document Builder", e);
		}
		
		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("QSigCompare");
		doc.appendChild(rootElement);
		
		Element bamFilesElement = doc.createElement("BAMFiles");
		rootElement.appendChild(bamFilesElement);
		
		// bam file elements
		for (File f : vcfFiles) {
			Element bamElement = doc.createElement("BAM");
			bamFilesElement.appendChild(bamElement);
			
			String [] fileAttributes = fileStats.get(f);
			
			// set some attributes on this bam element
			bamElement.setAttribute("id", "" + (vcfFiles.indexOf(f) + 1));
			bamElement.setAttribute("patient", fileAttributes[0]);
			bamElement.setAttribute("library", fileAttributes[1]);
			bamElement.setAttribute("inputType", fileAttributes[2]);
			bamElement.setAttribute("snpFile", fileAttributes[3]);
			bamElement.setAttribute("bam", f.getAbsolutePath());
			
			// coverage
			bamElement.setAttribute("coverage", fileAttributes[4]);
			
		}
		
		Element resultsElement = doc.createElement("Results");
		rootElement.appendChild(resultsElement);
		
		for (String [] s : displayResults) {
			Element resultElement = doc.createElement("result");
			resultsElement.appendChild(resultElement);
			
			resultElement.setAttribute("files", s[0] + " vs " + s[1]);
			resultElement.setAttribute("rating", s[2]);
			resultElement.setAttribute("score", s[3]);
			resultElement.setAttribute("snpsUsed", s[4]);
			resultElement.setAttribute("flag", s[5]);
			resultElement.setAttribute("minCoverage", s[6]);
		}
		
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			logger.error("Can't create new transformer factory", e);
		}
		DOMSource source = new DOMSource(doc);
		
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		
		// if no output specified, output to standard out
		StreamResult result = null;
		if (cmdLineOutputFiles == null || cmdLineOutputFiles.length < 1)
			result = new StreamResult(System.out );
		else 
			result = new StreamResult(new File(cmdLineOutputFiles[0]));
		 
		 try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			logger.error("Can't transform file", e);
		}
	}
	
	private void loadVcfFiles(String directory) {
		// does not recurse - add boolean param to method if this is required
		vcfFiles.addAll(Arrays.asList(FileUtils.findFilesEndingWithFilter(directory, ".vcf")));
		// and any zipped files
		vcfFiles.addAll(Arrays.asList(FileUtils.findFilesEndingWithFilter(directory, ".vcf.gz")));
		
		Collections.sort(vcfFiles);
	}
	
	private void selectFilesForComparison() throws Exception {
		// loop through the vcfFiles list, and run comparisons if no entry exists in the results map
		
		int cacheCutoff = vcfFiles.size() - maxCacheSize;
		
		for (int i = 0 , size = vcfFiles.size(); i < size ; i++) {
			File firstFile = vcfFiles.get(i);
			
			// set currentFileRatios to null
			currentFileRatios = null;
			
			logger.info("starting comparisons for " + firstFile.getName());
			
			for (int j = i + 1; j < size ; j++) {
				File secondFile = vcfFiles.get(j);
				doComparison(firstFile, secondFile, j >= cacheCutoff);
			}
			
			
			logger.info("comparisons done for " + firstFile.getName());
			// can now remove firstFile data from the fileRatios map
			// this may be required to keep memory usage down
//			 fileRatiosCache.remove(firstFile);
		}
	}
	
	private void loadFileMetaData(File file) throws Exception {
		// check that metadata does not already exist
		String[] metadata = fileStats.get(file);
		if (null == metadata) {
			metadata = new String[5];
			TabbedFileReader vcf1 = new TabbedFileReader(file);
			try {
				TabbedHeader vcfHeader1 = vcf1.getHeader();
				String [] vcfHeaderDetails = QSigCompare.getDetailsFromVCFHeader(vcfHeader1);
				for (int i = 0 ; i < vcfHeaderDetails.length ; i++) {
					metadata[i] = vcfHeaderDetails[i];
				}
			} finally {
				vcf1.close();
			}
			fileStats.put(file, metadata);
		}
	}
	
	private void doComparison(File f1, File f2, boolean cacheF2) throws Exception {
		
		// load metadata
		loadFileMetaData(f1);
		loadFileMetaData(f2);
		
		// try and get file ratios from cache (f1 from current if not null
		Map<ChrPosition, int[]> file1Ratios = currentFileRatios != null ? currentFileRatios : fileRatiosCache.get(f1);
		Map<ChrPosition, int[]> file2Ratios = fileRatiosCache.get(f2);
		
		if (null == file1Ratios) {
			file1Ratios = loadRatiosFromFile(f1);
			currentFileRatios = file1Ratios;
		}
		if (null == file2Ratios) {
			file2Ratios = loadRatiosFromFile(f2);
			
			// cache f2 if instructed to do so
			if (cacheF2) fileRatiosCache.put(f2, file2Ratios);
		}
		
		// update fileStats
		String[] file1Data = fileStats.get(f1);
		if (null != file1Data && file1Data.length > 4 && StringUtils.isNullOrEmpty(file1Data[4]))
			file1Data[4] = getCoverageAcrossSnps(file1Ratios);
		
		String[] file2Data  = fileStats.get(f2);
		if (null != file2Data && file2Data.length > 4 && StringUtils.isNullOrEmpty(file2Data[4]))
			file2Data[4] = getCoverageAcrossSnps(file2Ratios);
		
		
		AtomicIntegerArray noOfPositionsUsed = new AtomicIntegerArray(minCoverage);
		
		logger.info("about to compareRatios with sizes: " + file1Ratios.size() + " + " + file2Ratios.size());
		float [] totalDiffs = compareRatios(file1Ratios, file2Ratios, minCoverage, noOfPositionsUsed);
		
		String rating = getRating(f1, f2);
		for (int i = 0 , len = totalDiffs.length ; i < len ; i++) {
			
			float score = noOfPositionsUsed.get(i) == 0 ? Float.NaN : (totalDiffs[i] / noOfPositionsUsed.get(i));
			String flag = QSigCompare.getFlag(rating, score, noOfPositionsUsed.get(i), cutoff);
			
			displayResults.add(new String[] {"" + (vcfFiles.indexOf(f1) + 1) , "" +  (vcfFiles.indexOf(f2) + 1), 
					rating , ""+score , ""+noOfPositionsUsed.get(i) , flag, ""+(i+1)}); 
		}
	}
	
	public static float[] compareRatios(final Map<ChrPosition, int[]> file1Ratios,
			final Map<ChrPosition, int[]> file2Ratios, final int maxCoverage, AtomicIntegerArray noOfPOsitionsUsed) {
		float [] totalDifference = new float[maxCoverage];
		
//		boolean debugMode = logger.isLevelEnabled(QLevel.DEBUG);
		
		for (Entry<ChrPosition, int[]> file1RatiosEntry : file1Ratios.entrySet()) {
			
			
			// if coverage is zero, skip
			final int[] file1Ratio = file1RatiosEntry.getValue();
			final int f1TotalCount = file1Ratio[1];
			if (f1TotalCount == 0) continue;
			
			// get corresponding entry from file 2 array
			// if null, skip
			final int[] file2Ratio = file2Ratios.get(file1RatiosEntry.getKey());
			if (file2Ratio == null) continue;
			// if coverage is zero, skip
			final int f2TotalCount = file2Ratio[1];
			if (f2TotalCount == 0) continue;
			
			// have both positions with non-zero coverage - calculate diff
			final int f2NonRefCount = file2Ratio[0];
			final int f1NonRefCount = file1Ratio[0];
			float diffAtPos = Math.abs(((float)f1NonRefCount / f1TotalCount)- ((float)f2NonRefCount / f2TotalCount));
			
//			if (debugMode && diffAtPos >= 0.4f && f2TotalCount >= 10) {
//				logger.info(file1RatiosEntry.getKey().toIGVString() + " diff: " + diffAtPos);
//				logger.info("f1NonRefCount: " + f1NonRefCount + ", f1TotalCount: " + f1TotalCount + ", f2NonRefCount: " + f2NonRefCount + ", f2TotalCount: " + f2TotalCount);
////				logger.debug(file1RatiosEntry.getKey().toIGVString() + " diff: " + diffAtPos);
//			}
			
			int minCov = Math.min(maxCoverage, Math.min(f1TotalCount, f2TotalCount));
			
			for (int i = 0 ; i < minCov ; i++) {
				totalDifference[i] += diffAtPos;
				noOfPOsitionsUsed.incrementAndGet(i);
			}
		}
		
		return totalDifference;
	}
	
	public final String getRating(File f1, File f2) {
		
		String[] s1 = fileStats.get(f1);
		String[] s2 = fileStats.get(f2);
		
		if (null == s1 || null == s2 || s1.length < 3 || s2.length < 3) 
			throw new IllegalArgumentException("invalid argument passed to getRating");
		
		// array now contains coverage info too, will never be an exact metch between files, so need to explicitly examine the first 3 array items
		
		if ((null != s1[0] && s1[0].equals(s2[0])) && null != s1[1] && s1[1].equals(s2[1]) && null != s1[2] && s1[2].equals(s2[2]))
			return "AAA";
		else if (null != s1[0] && s1[0].equals(s2[0]) && null != s1[1] && s1[1].equals(s2[1]))
			return "AAB";
		else if (null != s1[1] &&  s1[1].equals(s2[1]) && null != s1[2] && s1[2].equals(s2[2]))
			return "BAA";
		else if (null != s1[0] && s1[0].equals(s2[0]) && null != s1[2] &&  s1[2].equals(s2[2]))
			return "ABA";
		else if (null != s1[0] && s1[0].equals(s2[0]) )
			return "ABB";
		else if (null != s1[1] && s1[1].equals(s2[1]) )
			return "BAB";
		else if (null != s1[2] && s1[2].equals(s2[2]) )
			return "BBA";
		
		return "BBB";
	}
	
	
	
	public static String getCoverageAcrossSnps(Map<ChrPosition, int[]> ratios) {
		final int arraySize = 2048*2048;
		final AtomicIntegerArray coverage = new AtomicIntegerArray(arraySize); 
		
		for (int[] intArray : ratios.values()) {
			// second entry in intArray is the coverage
			int arrayPosition = intArray[1];
			coverage.incrementAndGet(arrayPosition);
		}
		
		// build a result string
		// zeros are total in snps minus total in ratios
		int zeroCount = snps.size() - ratios.size();
		StringBuilder sb = new StringBuilder("0:");
		sb.append(zeroCount);
		
		int over20Counter = 0;
		
		for (int i = 0 ; i < arraySize ; i++) {
			if (coverage.get(i) > 0) {
				if (i > 20) {
					over20Counter += coverage.get(i);
				} else {
					if (sb.length() > 0) sb.append(',');
					sb.append(i);
					sb.append(':');
					sb.append(coverage.get(i));
				}
			}
		}
		// add in the over 20 info
		if (over20Counter > 0) {
			if (sb.length() > 0) sb.append(',');
			sb.append("21+");
			sb.append(':');
			sb.append(over20Counter);
		}
		
		return sb.toString();
	}
	
	private Map<ChrPosition, int[]> loadRatiosFromFile(File file) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(file);
		Map<ChrPosition, int[]> ratios = null;
		int zeroCov = 0, invalidRefCount = 0;
		try {
			logger.info("loading ratios from file: " + file.getAbsolutePath());
			
			boolean populateSnps = snps.isEmpty(); 
			
			ratios = new HashMap<ChrPosition, int[]>();
			
			for (TabbedRecord vcfRecord : reader) {
				String[] params = TabTokenizer.tokenize(vcfRecord.getData());
				ChrPosition chrPos = new ChrPosition(params[0], Integer.parseInt(params[1]));
				
				if (populateSnps)
					snps.add(chrPos);
				
				String coverage = params[7];
				
				// only populate ratios with non-zero values
				// attempt to keep memory usage down...
				if (SignatureUtil.EMPTY_COVERAGE.equals(coverage))  {
					zeroCov++;
					continue;
				}
				
				char ref = params[3].charAt(0);
				if ( ! BaseUtils.isACGTN(ref)) {
					if ('-' != ref && '.' != ref)
						logger.info("invalid reference base for record: " + Arrays.deepToString(params));
					invalidRefCount++;
					continue;
				}
				
				ratios.put(chrPos, QSigCompare.getRatioFromCoverageString(coverage, ref));
			}
		} finally {
			reader.close();
		}
		logger.info("zero cov count: " + zeroCov + ", ratios count: " + ratios.size() + ", snps count: " + snps.size() + ", invalid ref count: " + invalidRefCount);
		return ratios;
	}
	
	public static void main(String[] args) throws Exception {
		QSigCompareDigDeeper sp = new QSigCompareDigDeeper();
		int exitStatus = 0;
		try {
			exitStatus = sp.setup(args);
		} catch (Exception e) {
			exitStatus = 2;
			if (null != logger)
				logger.error("Exception caught whilst running QSigCompareDigDeeper:", e);
			else {
				System.err.println("Exception caught whilst running QSigCompareDigDeeper: " + e.getMessage());
				System.err.println(Messages.USAGE);
			}
		}
		
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = 1;
		if (null == args || args.length == 0) {
			System.err.println(Messages.USAGE);
			System.exit(1);
		}
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLog();
			logger = QLoggerFactory.getLogger(QSigCompareDigDeeper.class, logFile, options.getLogLevel());
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QSignatureException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QSignatureException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QSignatureException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			if (options.getMinCoverage() > 0)
				minCoverage =  options.getMinCoverage();
			if (options.getCutoff() > 0.0f)
				cutoff =  options.getCutoff();
			
			logger.logInitialExecutionStats("QSigCompareDigDeeper", QSigCompareDigDeeper.class.getPackage().getImplementationVersion(), args);
			logger.tool("Will use minCoverage of: " + minCoverage + ", and a cutoff of: " + cutoff);
			
			return engage();
		}
		return returnStatus;
	}
}