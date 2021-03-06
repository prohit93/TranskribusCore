package eu.transkribus.core.io;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.util.JaxbUtils;
import eu.transkribus.core.util.PageXmlUtils;

public class XslTransformTest {
	
	private static final String ABBY_TO_PAGE_XSLT = "xslt/Abbyy10ToPage2013.xsl";
	private static final String ALTO_TO_PAGE_XSLT = "xslt/AltoToPage2013.xsl";
	
	public static void main(String[] args){
		//transformAbbyyToPage();
		
		transformAltoToPage();
	}

	private static void transformAltoToPage() {
		
		final File altoXml = new File("C:/01_Projekte/READ/Projekte/NewsEye/NLF_GT/nlf_ocr_groundtruth_fi/Test - Kopie/alto/0024-8045_1910-01-01_9_18-gt2.xml");
		File pageOutFile = new File("C:/01_Projekte/READ/Projekte/NewsEye/NLF_GT/nlf_ocr_groundtruth_fi/Test - Kopie/page/0024-8045_1910-01-01_9_18-gt2.xml");
		
		final String TEXT_STYLE_PARAM_NAME = "preserveTextStyles";
		Map<String, Object> params = new HashMap<>();
		params.put(TEXT_STYLE_PARAM_NAME, Boolean.FALSE);
		
		PcGtsType pc;
		try {
			pc = JaxbUtils.transformToObject(altoXml, ALTO_TO_PAGE_XSLT, params, PcGtsType.class);
		
			//pc.getPage().setImageFilename("744641.tiff");
			File pageXml = JaxbUtils.marshalToFile(pc, pageOutFile);
			
			PcGtsType result = PageXmlUtils.unmarshal(pageOutFile);
		} catch (TransformerException | SAXException | IOException | ParserConfigurationException
				| JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static void transformAbbyyToPage() {
		//final File abbyyXml = new File("C:/01_Projekte/READ/Projekte/newseye_testdata/newseye_testdata/ocr/744641.xml");
		final File abbyyXml = new File("C:/01_Projekte/READ/Programmierung/ConvertAbbyyTableToPageTable/2304896.xml");
		
		//final File abbyyXml = new File("C:/01_Projekte/READ/Projekte/Xslt_Test/OCRmaster/ocr/2239047.xml");
		
		//File pageOutFile = new File("C:/01_Projekte/READ/Projekte/Xslt_Test/OCRmaster/page/2239047.xml");
		File pageOutFile = new File("C:/01_Projekte/READ/Programmierung/ConvertAbbyyTableToPageTable/2304896_page.xml");
		
		final String TEXT_STYLE_PARAM_NAME = "preserveTextStyles";
		Map<String, Object> params = new HashMap<>();
		params.put(TEXT_STYLE_PARAM_NAME, Boolean.FALSE);
		
		PcGtsType pc;
		try {
			pc = JaxbUtils.transformToObject(abbyyXml, ABBY_TO_PAGE_XSLT, params, PcGtsType.class);
		
			//pc.getPage().setImageFilename("744641.tiff");
			File pageXml = JaxbUtils.marshalToFile(pc, pageOutFile);
			
			PcGtsType result = PageXmlUtils.unmarshal(pageOutFile);
		} catch (TransformerException | SAXException | IOException | ParserConfigurationException
				| JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
