package au.edu.qimr.qannotate.options;

import static java.util.Arrays.asList;
import joptsimple.OptionSet;
import au.edu.qimr.qannotate.Messages;


public class Vcf2mafOptions extends Options {
	 public Vcf2mafOptions( ) {  super(Options.MODE.vcf2maf);	  }
	 
	 public static final String tumourid_Default = "TEST";
	 public static final String normalid_Default = "CONTROL";
	 public static final String unkown = "unkown";
	 public static final String null_String = "null";
	 
	 String center ; 
	 String sequencer; 
	 String tumourid;
	 String normalid;
	 
	 
	 final String  Description_sequencer = "eg.  <Illumina GAIIx, Illumina HiSeq,SOLID,454, ABI 3730xl, Ion Torrent PGM,Ion Torrent Proton,PacBio RS, Illumina MiSeq,Illumina HiSeq 2500,454 GS FLX Titanium,AB SOLiD 4 System>";
	 
	 @Override
	    public boolean parseArgs(final String[] args) throws Exception{  	
	    	 
	        parser.acceptsAll( asList("h", "help"), Messages.getMessage("HELP_OPTION_DESCRIPTION"));
	        parser.acceptsAll( asList("i", "input"), Messages.getMessage("INPUT_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("input vcf");
	        parser.acceptsAll( asList("o", "output"),  "output maf file with full path").withRequiredArg().ofType(String.class).describedAs("output maf"); 
	        
	        parser.accepts("tumour",  Messages.getMessage("TUMOUR_SAMPLEID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("tumourSample");
	        parser.accepts("normal", Messages.getMessage("NORMAL_SAMPLEID_DESCRIPTION")).withRequiredArg().ofType(String.class).describedAs("normalSample");	        
	        parser.accepts("center", "Genome sequencing center").withRequiredArg().ofType(String.class).describedAs("center");
	        parser.accepts("sequencer", Description_sequencer).withRequiredArg().ofType(String.class).describedAs("Sequencer");
	        
	        
	        parser.accepts("mode", "run vcf2maf").withRequiredArg().ofType(String.class).describedAs("vcf2maf");
	       // "(compulsary) database location"
 	        parser.accepts("log", LOG_DESCRIPTION).withRequiredArg().ofType(String.class);
	        parser.accepts("loglevel",  LOG_LEVEL_OPTION_DESCRIPTION).withRequiredArg().ofType(String.class);
	        final OptionSet options = parser.parse(args);   
	        
	        if(options.has("h") || options.has("help")){
	        	displayHelp(Messages.getMessage("VCF2MAF_USAGE"));
	            return false;
	        }
	                               
	        if( !options.has("log")){
	            System.out.println(Messages.getMessage("LOG_OPTION_DESCRIPTION"));            
	            return false;
	        } else{  
	        	logFileName = (String) options.valueOf("log");  	
	        	logLevel = (String) options.valueOf("loglevel");
	        }
	    
	        commandLine = Messages.reconstructCommandLine(args) ;
	        //check IO
	        inputFileName = (String) options.valueOf("i") ;      	 
	        outputFileName = (String) options.valueOf("o") ; 
	        
	        center = (options.has("center"))? (String)options.valueOf("center") : unkown;
	        sequencer = (options.has("sequencer"))? (String)options.valueOf("sequencer") : unkown;	        
	        tumourid = (options.has("tumour"))? (String)options.valueOf("tumour") : tumourid_Default;
	        normalid = (options.has("normal"))? (String)options.valueOf("normal") : normalid_Default;
	        
  	        return true;
	     } 
	 
	 public String getCenter(){  return center; }
	 public String getSequencer(){  return sequencer; }
	 public String getTumourid(){  return (tumourid.equalsIgnoreCase(null_String))? null:tumourid; }
	 public String getNormalid(){  return (normalid.equalsIgnoreCase(null_String))? null:normalid; }
	 
}