package eu.transkribus.core.model.builder.tei;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.JAXBPageTranscript;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.builder.ExportUtils;
import eu.transkribus.core.model.builder.tei.TeiExportPars.TeiExportMode;

public abstract class ATeiBuilder {
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ATeiBuilder.class);
	
	public static final String TEI_NS = "http://www.tei-c.org/ns/1.0";
	public static final String TEI_FILE_NAME = "tei.xml";
	public static final String FACS_ID_PREFIX = "facs_";
	public static final String DEFAULT_PUBLISHER = "tranScriptorium";
	
	protected final TrpDoc trpDoc;
	
//	protected TeiExportMode mode = TeiExportMode.SIMPLE;
	
	//keep the transcripts here. When changing the mode we won't need to get them again
	protected Map<Integer, PcGtsType> transcrBuffer;
	
	IProgressMonitor monitor;
	
//	Set<Integer> pageIndices;
//	Set<String> selectedTags;
	
	TeiExportPars pars;
	
	public ATeiBuilder(TrpDoc doc, TeiExportMode mode, IProgressMonitor monitor, Set<Integer> pageIndices, Set<String> selectedTags) {
		this.trpDoc = doc;
		this.transcrBuffer = new HashMap<>();
		
		this.pars = new TeiExportPars();
		pars.mode = mode;
		pars.pageIndices = pageIndices;
		pars.selectedTags = selectedTags;
		
//		this.mode = mode;
		this.monitor = monitor;
//		this.pageIndices = pageIndices;
//		this.selectedTags = selectedTags;
		
		Assert.assertNotNull("tei pars is null!", this.pars);
	}
	
	public ATeiBuilder(TrpDoc doc, TeiExportPars pars, IProgressMonitor monitor) {
		this.trpDoc = doc;
		this.transcrBuffer = new HashMap<>();
		this.monitor = monitor;
		
		if (pars != null)
			this.pars = pars;
		else
			this.pars = new TeiExportPars();
		
		Assert.assertNotNull("tei pars is null!", this.pars);
	}
	
	public IProgressMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	public void setMode(final TeiExportMode mode) throws JAXBException, ParserConfigurationException{
		this.pars.mode = mode;
	}
	
	public abstract String getTeiAsString();
	
	public void writeTeiXml(File file) throws IOException {
		final String teiStr = getTeiAsString();
		
		FileOutputStream fos = new FileOutputStream(file);
		OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
//		FileWriterWithEncoding writer = new FileWriterWithEncoding(file, Charset.forName("UTF-8"));
		BufferedWriter bWriter = new BufferedWriter(osw);
		bWriter.write(teiStr);
		
		bWriter.flush();
		bWriter.close();
	}
		
	public void buildTei() throws Exception{		
		startDocument();
		
		setHeader(trpDoc.getMd());
		
		setContent(trpDoc.getPages());
		
		endDocument();
	}

	protected abstract void startDocument() throws Exception;
	protected abstract void endDocument();
	
	protected abstract void setHeader(TrpDocMetadata md);
	protected abstract void setContent(List<TrpPage> pages) throws JAXBException, InterruptedException;
	
//	protected abstract void setTextRegion(TextRegionType r, int pageNr);
	
	protected PcGtsType getPcGtsTypeForPage(TrpPage p) throws JAXBException {
		PcGtsType pc;
		if(transcrBuffer.containsKey(p.getPageNr())){
			pc = transcrBuffer.get(p.getPageNr());
		} else {
			TrpTranscriptMetadata tMd = p.getCurrentTranscript();
			try{
//				JAXBPageTranscript tr = new JAXBPageTranscript(tMd);
//				tr.build();
//				pc = tr.getPageData();
				
				//replaces previous loading (3 lines above) to avoid double unmarshalling
				JAXBPageTranscript pt = ExportUtils.getPageTranscriptAtIndex(p.getPageNr()-1);
				if (pt != null){
					pc = pt.getPageData();
				}
				else{
					JAXBPageTranscript tr = new JAXBPageTranscript(tMd);
					tr.build();
					pc = tr.getPageData();
				}
				
				
			} catch (IOException je){
				throw new JAXBException("Could not unmarshal page " + p.getPageNr(), je);
			}
			transcrBuffer.put(p.getPageNr(), pc);
		}
		return pc;
	}
}
