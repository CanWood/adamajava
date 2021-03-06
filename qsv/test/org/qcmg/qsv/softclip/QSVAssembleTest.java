package org.qcmg.qsv.softclip;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.qcmg.qsv.assemble.ConsensusRead;
import org.qcmg.qsv.assemble.QSVAssemble;
import org.qcmg.qsv.assemble.Read;
import org.qcmg.qsv.splitread.UnmappedRead;

public class QSVAssembleTest {
	
	QSVAssemble assemble;

	@Before
	public void setUp() throws Exception {
		assemble = new QSVAssemble();
	}

	private List<Read> setUpSplitReads() throws Exception {
		List<Read> splitReads = new ArrayList<Read>();
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190","GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:6:2301:10241:71660:20120608110941621","ATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTG"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:4:1110:20608:86188:20120608092353631","CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:1:1204:3577:34360:20120608115535190","AGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGC"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:5:2113:4661:50103:20120607102754932","GATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGT"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:4:1114:3101:51165:20120608092353631","TCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGG"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:2:2214:2138:95916:2012060803293054","ACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGCTCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGN"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:6:1213:16584:89700:20120608110941621","TTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGG"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:3:1105:6813:57594:20120608103628548","TCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGG"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:4:1212:15055:33947:20120608092353631","GATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAG"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:8:1115:9658:72817:20120608020343585","TTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTT"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:7:1213:20264:2165:20120608113919562","ACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATT"));
		splitReads.add(new Read("fullclip_HWI-ST1240:47:D12NAACXX:4:2202:2958:25970:20120608092353631","TGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTA"));
		splitReads.add(new Read("split_HWI-ST1240:47:D12NAACXX:3:2116:11014:12481:20120608103628548","TCAATACCTAGTCTTCCTAAGATATTAAGGAATTGATCCCTGCCCTAAGAGCAGCAAATTGCTGAACTCCTCTGGTGGACCTCTTACACAAAGTATAATCT"));
		splitReads.add(new Read("split_HWI-ST1240:47:D12NAACXX:4:2202:20078:100239:20120608092353631","TAAGATATTAAGGAATTGATCCCTGCCCTAAGAGCAGCAAATTGCTGAACTCCTCTGGTGGACCTCTTACACAAAGTATAATCTCTTACACAAAGAGATTA"));
		splitReads.add(new Read("split_HWI-ST1240:47:D12NAACXX:2:1211:20501:83877:2012060803293054","TAAGATATTAAGGAATTGATCCCTGCCCTAAGAGCAGCAAATTGCTGAACTCCTCTGGTGGACCTCTTACACAAAGTATAATCTCTTACACAAAGAGATTA"));
		splitReads.add(new Read("split_HWI-ST1240:47:D12NAACXX:3:1301:15545:13902:20120608103628548","TCAATACCTAGTCTTCCTAAGATATTAAGGAATTGATCCCTGCCCTAAGAGCAGCAAATTGCTGAACTCCTCTGGTGGACCTCTTACACAAAGTATAATCT"));
		splitReads.add(new Read("split_HWI-ST1240:47:D12NAACXX:3:2106:5948:53437:20120608103628548","TCAATACCTAGTCTTCCTAAGATATTAAGGAATTGATCCCTGCCCTAAGAGCAGCAAATTGCTGAACTCCTCTGGTGGACCTCTTACACAAAGTATAATCT"));
		
		return splitReads;
	}
	
	private List<UnmappedRead> setUpUnmappedReads() throws Exception {
		List<UnmappedRead> splitReads = new ArrayList<UnmappedRead>();
		splitReads.add(new UnmappedRead("unmapped,HWI-ST1240:47:D12NAACXX:3:2116:11014:12481:20120608103628548,chr10,89700043,AGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGA", true));
		splitReads.add(new UnmappedRead("unmapped,HWI-ST1240:47:D12NAACXX:4:2202:20078:100239:20120608092353631,chr10,89700043,TAAGATATTAAGGAATTGATCCCTGCCCTAAGAGCAGCAAATTGCTGAACTCCTCTGGTGGACCTCTTACACAAAGTATAATCTCTTACACAAAGAGATTA", true));
		splitReads.add(new UnmappedRead("unmapped,HWI-ST1240:47:D12NAACXX:4:2202:20078:100239:20120608092353631,chr10,89700043,TAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTA", true));
		splitReads.add(new UnmappedRead("unmapped,HWI-ST1240:47:D12NAACXX:2:1211:20501:83877:2012060803293054,chr10,89700053,TAAGATATTAAGGAATTGATCCCTGCCCTAAGAGCAGCAAATTGCTGAACTCCTCTGGTGGACCTCTTACACAAAGTATAATCTCTTACACAAAGAGATTA", true));
		splitReads.add(new UnmappedRead("unmapped,HWI-ST1240:47:D12NAACXX:2:1211:20501:83877:2012060803293054,chr10,89700053,TAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTA", true));
		return splitReads;
	}

	@After
	public void tearDown() throws Exception {
		assemble = null;
	}
	
	@Test
	public void getContigs() throws Exception {
		Read read = new Read("clipContig","GATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA");
		ConsensusRead r = assemble.getContigs(8970200, read, setUpSplitReads(), false, true);
		assertNotNull(r);
		assertTrue(read.length() < r.getSequence().length());
		assertEquals("ATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA", r.getClipMateSequence());
	}
	
	@Test
	public void getFinalContig() throws Exception {
		ConsensusRead r = assemble.getFinalContig(setUpUnmappedReads());
		assertNotNull(r);
		assertEquals("TAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGA", r.getClipMateSequence());
	}
	
	@Test
	public void createFinalConsensusRead() throws Exception {
		assemble.setClipContig(new Read("clip", "AAAAAAAAAA"));
		assertClipMateSequence("AAAAAAAAAA", "AAAAAAAAAA", true, false);		
		assertClipMateSequence("GGAAAAAAAAAA", "GGAAAAAAAAAATT", true, false);
		assertClipMateSequence("AAAAAAAAAATT", "GGAAAAAAAAAATT", false, false);
		
		assertClipMateSequence("TTTTTTTTTTCC", "GGAAAAAAAAAATT", true, true);
		assertClipMateSequence("AATTTTTTTTTT", "GGAAAAAAAAAATT", false, true);
	}

	private void assertClipMateSequence(String mateSeq, String seq, boolean isLeft, boolean clipReverse) throws Exception {
		assemble.setFullContigSequence(seq);
		assemble.setOutputRead(new Read("output", seq));
		ConsensusRead r = assemble.createFinalConsensusRead(isLeft, clipReverse, 1234);
		assertEquals(mateSeq, r.getClipMateSequence());		
	}

}
