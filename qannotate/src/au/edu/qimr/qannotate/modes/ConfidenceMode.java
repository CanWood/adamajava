package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.DonorUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.maf.util.MafUtils;

import au.edu.qimr.qannotate.modes.AbstractMode.SampleColumn;
import au.edu.qimr.qannotate.options.ConfidenceOptions;

/**
 * @author christix
 *
 */
public class ConfidenceMode extends AbstractMode{
	
	public static final int HIGH_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int HIGH_CONF_ALT_FREQ_PASSING_SCORE = 5;	
	public static final int LOW_CONF_NOVEL_STARTS_PASSING_SCORE = 4;
	public static final int LOW_CONF_ALT_FREQ_PASSING_SCORE = 4;	
	public static final String SOMATIC = "SOMATIC";
	public static final String NOVEL_STARTS = "NNS";
	
	//filters 
	public static final String PASS = "PASS";
	public static final String LESS_THAN_12_READS_NORMAL = "COVN12";
	public static final String LESS_THAN_3_READS_NORMAL = "SAN3";
	public static final String MUTATION_IN_UNFILTERED_NORMAL = "MIUN";
	public static final String LESS_THAN_12_READS_NORMAL_AND_UNFILTERED= LESS_THAN_12_READS_NORMAL + ";" + MUTATION_IN_UNFILTERED_NORMAL;
	public static final String LESS_THAN_3_READS_NORMAL_AND_UNFILTERED= LESS_THAN_3_READS_NORMAL + ";" + MUTATION_IN_UNFILTERED_NORMAL;

	public enum Confidence{	HIGH , LOW, ZERO ; }
	private final String patientId;
	
	private int test_column = -2; //can't be -1 since will "+1"
	private int control_column  = -2;
	
	//for unit testing
	ConfidenceMode(String patient){ 
		patientId = patient;
	}

	
	public ConfidenceMode(ConfidenceOptions options, QLogger logger) throws Exception{				 
		logger.tool("input: " + options.getInputFileName());
        logger.tool("verified File: " + options.getDatabaseFileName() );
        logger.tool("output annotated records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + options.getLogLevel());
 		
		inputRecord(new File( options.getInputFileName())   );	
		

		//get control and test sample column; here use the header from inputRecord(...)
		SampleColumn column = new SampleColumn(options.getTestSample(), options.getControlSample(), this.header );
		test_column = column.getTestSampleColumn();
		control_column = column.getControlSampleColumn();


		//if(options.getpatientid == null)
		patientId = DonorUtils.getDonorFromFilename(options.getInputFileName());		
		addAnnotation( options.getDatabaseFileName() );
		reheader(options.getCommandLine(),options.getInputFileName())	;	
		writeVCF(new File(options.getOutputFileName()) );	
	}

	public void setSampleColumn(int test, int control){
		test_column = test;
		control_column = control; 
	}

	/**
	 * add dbsnp version
	 * @throws Exception
	 */
	@Override
	//inherited method from super
	void addAnnotation(String verificationFile) throws Exception{

		
		//load verified file
		final Map<String, Map<ChrPosition, TorrentVerificationStatus>> verifiedDataAll = new HashMap<String, Map<ChrPosition, TorrentVerificationStatus>>();
		MafUtils.getVerifiedData(verificationFile, patientId,verifiedDataAll );
		final Map<ChrPosition, TorrentVerificationStatus> VerifiedData = verifiedDataAll.get(patientId);		
		
		//check high, low nns...
		final Iterator<  ChrPosition > it = positionRecordMap.keySet().iterator();
	    while (it.hasNext()) {
	    	final ChrPosition pos = it.next();
	    	final VcfRecord vcf = positionRecordMap.get(pos);
	    	final VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(vcf.getInfo());	
	    		    	
	        if (VerifiedData != null && VerifiedData.get(pos) != null && VerifiedData.get(pos).equals( TorrentVerificationStatus.YES))  
	        	 infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());		        
	        else if (checkNovelStarts(HIGH_CONF_NOVEL_STARTS_PASSING_SCORE, vcf) 
					&& ( getAltFrequency(vcf) >=  HIGH_CONF_ALT_FREQ_PASSING_SCORE)
					&& PASS.equals(vcf.getFilter()))  
	        	 infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.HIGH.toString());		        	 				 				
			else if (checkNovelStarts(LOW_CONF_NOVEL_STARTS_PASSING_SCORE, vcf) 
					&& ( getAltFrequency(vcf) >= LOW_CONF_ALT_FREQ_PASSING_SCORE )
					&& isClassB(vcf.getFilter()) )
				 infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.LOW.toString());					 
			 else
				 infoRecord.setField(VcfHeaderUtils.INFO_CONFIDENT, Confidence.ZERO.toString());		  
	        
	        vcf.setInfo(infoRecord.toString());	        
	    } 	
 
		//add header line  set number to 1
		header.addInfoLine(VcfHeaderUtils.INFO_CONFIDENT, "1", "String", VcfHeaderUtils.DESCRITPION_INFO_CONFIDENCE);
	}

	/**
	 * 
	 * @param filter
	 * @return true if the filter string match "PASS", "MIUN","SAN3" and "COVN12";
	 */
	private boolean isClassB(String filter){
		if (PASS.equals(filter))
				return true;
		
		//remove MUTATION_IN_UNFILTERED_NORMAL
		final String f = filter.replace(MUTATION_IN_UNFILTERED_NORMAL, "").replace(";","").trim();
		if(f.equals(Constants.EMPTY_STRING) ||  LESS_THAN_12_READS_NORMAL.equals(f)  ||   LESS_THAN_3_READS_NORMAL.equals(f))
			 return true;
		
		return false;
	}
	
	
	/**
	 * @param vcf
	 * @return
	 */
	private int getAltFrequency(VcfRecord vcf){
		 final String info =  vcf.getInfo();
		 final VcfFormatFieldRecord re = (info.contains(VcfHeaderUtils.INFO_SOMATIC)) ? vcf.getSampleFormatRecord(test_column) :  vcf.getSampleFormatRecord(control_column);
		 
		 return VcfUtils.getAltFrequency(re, vcf.getAlt());	 
	}
 
	 private boolean   checkNovelStarts(int score, VcfRecord vcf ) {
		 
		 //if marked "NNS" on filter, return false
		 String[] filters = vcf.getFilter().split(Constants.SEMI_COLON_STRING);
		 for(int i = 0; i < filters.length; i ++)
			 if(filters[i].equals(VcfHeaderUtils.FILTER_NOVEL_STARTS))
				 return false;
		 
		 return true;
	 }  
}	
	
  
	
 