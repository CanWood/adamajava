package au.edu.qimr.qannotate.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import htsjdk.samtools.SAMRecord;
import org.qcmg.common.model.ChrPosition;

/**
 * pileup on snp position, retrive the number of discard read, duplicate read, overlapped pairs, forward/backward reads, novel start reads for each Alleles
 * @author christiX
 *
 */
public class SnpPileup {
//	private static final short MD_TAG = SAMTagUtil.getSingleton().MD;
	private final ChrPosition pos;
	private int overSame = 0; //number of fragment with overlap reads and contain same base
	private int overDiff = 0; //number of fragment with overlap reads but different base
	private final int[] atgco = new int[5]; //counts of fragment on pileup base on A T G C and others
	int coverage = 0;
	
	/**
	 * 
	 * @param chrP
	 * @param ref: here we can only deal with SNP that is a single char
	 * @param pool
	 */
	public SnpPileup(ChrPosition chrP, List<SAMRecord>  pool){
		this.pos = chrP;
				
		coverage = pool.size();		//total number of reads
		Map<String,  List<SAMRecord>> pairPool = new HashMap<>();
		for(SAMRecord re : pool) {
			if( ! re.getDuplicateReadFlag()) {
				pairPool.computeIfAbsent(re.getReadName(),k->new ArrayList<>()).add(re);
			}
		}
		for(List<SAMRecord> pair: pairPool.values()){
			if( pair.size() == 1){
				int base = getSnpBaseCode( pair.get(0) );
				atgco[base] ++;
			}else if( pair.size() == 2 ){
				int base1 = getSnpBaseCode( pair.get(0) );
				int base2 = getSnpBaseCode( pair.get(1) );				

				if(base1 != base2 ){ overDiff ++; continue; }
				atgco[base1] ++;
				overSame ++;
									
			}else  	// shouldn't happen
				System.err.println( pair.size() + " reads with same name: " + pair.get(0).getReadName());				
			 
		}		
	}
	
	private int getSnpBaseCode(  SAMRecord re ){
		int offset = re.getReadPositionAtReferencePosition( pos.getStartPosition() );
 		if(offset > 0) {
			byte[] bases = re.getReadBases();		
			if( (char) bases[offset-1]  == 'A' )
				return 0;
			else if( (char) bases[offset-1]  == 'C' )
				return 1;
			else if( (char) bases[offset-1]  == 'G' )
				return 2;
			else if( (char) bases[offset-1]  == 'T' )
				return 3;
		}  		
		return 4;
	}
	
	public String getAnnotation(){
		//coverage[coverage, vendor, secondary ,supply, duplicate,errMD]
		String str = String.format("%d[%d,%d,A%d,C%d,G%d,T%d,O%d]",coverage, overSame, overDiff, atgco[0], atgco[1], atgco[2], atgco[3], atgco[4] );

		return str;
	}	
		
	public ChrPosition getPosition(){ return pos; }
}
