package eu.transkribus.core.model.builder.pdf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.dea.fimagestore.core.beans.ImageMetadata;
import org.dea.fimgstoreclient.beans.ImgType;
import org.dea.util.pdf.APdfDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.awt.geom.Line2D;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;

import eu.transkribus.core.model.beans.EdFeature;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.customtags.AbbrevTag;
import eu.transkribus.core.model.beans.customtags.CommentTag;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.customtags.CustomTagFactory;
import eu.transkribus.core.model.beans.customtags.GapTag;
import eu.transkribus.core.model.beans.customtags.StructureTag;
import eu.transkribus.core.model.beans.customtags.SuppliedTag;
import eu.transkribus.core.model.beans.customtags.TextStyleTag;
import eu.transkribus.core.model.beans.pagecontent.BaselineType;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.RegionType;
import eu.transkribus.core.model.beans.pagecontent.TableRegionType;
import eu.transkribus.core.model.beans.pagecontent.TextLineType;
import eu.transkribus.core.model.beans.pagecontent.TextRegionType;
import eu.transkribus.core.model.beans.pagecontent.UnknownRegionType;
import eu.transkribus.core.model.beans.pagecontent.WordType;
import eu.transkribus.core.model.beans.pagecontent_trp.ITrpShapeType;
import eu.transkribus.core.model.beans.pagecontent_trp.RegionTypeUtil;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpBaselineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpElementReadingOrderComparator;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpShapeTypeUtils;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTableCellType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTableRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.model.builder.ExportCache;
import eu.transkribus.core.model.builder.ExportUtils;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.PointStrUtils;
import eu.transkribus.interfaces.types.util.TrpImageIO;


/**
 * Wrapper class for building PDFs from TrpDocuments with Itext.
 * Based on FEP's PDF_Document
 * @author philip and schorsch
 *
 */
public class TrpPdfDocument extends APdfDocument {
	private static final Logger logger = LoggerFactory.getLogger(APdfDocument.class);
	private final boolean useWordLevel;
	private final boolean highlightTags;
	private final boolean highlightArticles;
	private final boolean doBlackening;
	private final boolean createTitle;
	
	private boolean imgOnly = false;
	private boolean extraTextPage = false;
	
	InputStream is1 = this.getClass().getClassLoader().getResourceAsStream("fonts/FreeSerif.ttf");
	byte[] rBytes1 = IOUtils.toByteArray(is1);
	InputStream is2 = this.getClass().getClassLoader().getResourceAsStream("fonts/FreeSerifBold.ttf");
	byte[] rBytes2 = IOUtils.toByteArray(is2);
	InputStream is3 = this.getClass().getClassLoader().getResourceAsStream("fonts/FreeSerifItalic.ttf");
	byte[] rBytes3 = IOUtils.toByteArray(is3);
	InputStream is4 = this.getClass().getClassLoader().getResourceAsStream("fonts/FreeSerifBoldItalic.ttf");
	byte[] rBytes4 = IOUtils.toByteArray(is4);
	
	BaseFont bfSerif = BaseFont.createFont("freeserif.ttf", BaseFont.IDENTITY_H, true, false, rBytes1, null);
	BaseFont bfSerifBold = BaseFont.createFont("freeserifbold.ttf", BaseFont.IDENTITY_H, true, false, rBytes2, null);
	BaseFont bfSerifItalic = BaseFont.createFont("freeserifitalic.ttf", BaseFont.IDENTITY_H, true, false, rBytes3, null);
	BaseFont bfSerifBoldItalic = BaseFont.createFont("freeserifbolditalic.ttf", BaseFont.IDENTITY_H, true, false, rBytes4, null);	
//	BaseFont bfUnifontBold = BaseFont.createFont("unifont.ttf", BaseFont.IDENTITY_H, true, false, rBytes5, null);

//	Font fontSerifBold = new Font(bfSerifBold);
//	Font fontSerifItalic = new Font(bfSerifItalic);

	Font mainExportFont;
	Font boldFont;
	Font italicFont;
	Font boldItalicFont;
	
	BaseFont mainExportBaseFont;
	BaseFont boldBaseFont;
	BaseFont italicBaseFont;
	BaseFont boldItalicBaseFont;
	
	ImgType imgType = ImgType.view;
	
	/*
	 * divide page into twelth * twelth regions to have a nice print
	 * first column is divisions
	 * second (0 is x direction, 1 is y direction)
	 */
	float[][] twelfthPoints = new float[13][2];

	//protected float scaleFactorX = 1.0f;
	//protected float scaleFactorY = 1.0f;
	float lineMeanHeight = 0;
	float prevLineMeanHeight = 0;
	//float overallLineMeanHeight = 0;
	float smallerRegionMaxX = 0;
	//Durchschuss (+ Textgröße = Zeilenabstand)
	private int leading = 3;
	int wordOffset = 0;
	
	java.util.List<java.util.Map.Entry<Line2D,String>> lineAndColorList= new java.util.ArrayList<>();

//	public TrpPdfDocument(final File pdfFile) throws DocumentException, IOException {
//		this(pdfFile, 0, 0, 0, 0, false, false, false, false);
//	}
	
	public TrpPdfDocument(final File pdfFile, boolean useWordLevel, boolean highlightTags, boolean highlightArticles, boolean doBlackening, boolean createTitle, String exportFontname, ImgType pdfImgType) throws DocumentException, IOException {
		this(pdfFile, 0, 0, 0, 0, useWordLevel, highlightTags, highlightArticles, doBlackening, createTitle, exportFontname, pdfImgType);
	}
	
	public TrpPdfDocument(final File pdfFile, final int marginLeft, final int marginTop, final int marginBottom, final int marginRight, final boolean useWordLevel, final boolean highlightTags, final boolean highlightArticles, final boolean doBlackening, boolean createTitle, final String exportFontname, ImgType pdfImgType) throws DocumentException, IOException {
		super(pdfFile, marginLeft, marginTop, marginBottom, marginRight);
		this.useWordLevel = useWordLevel;
		this.highlightTags = highlightTags;
		this.highlightArticles = highlightArticles;
		this.doBlackening = doBlackening;
		this.createTitle = createTitle;
		
		if (pdfImgType != null){
			this.imgType = pdfImgType;
		}
		
		//logger.debug(" path to fonts : " + this.getClass().getClassLoader().getResource("fonts").getPath());
		FontFactory.registerDirectory(this.getClass().getClassLoader().getResource("fonts").getPath());
	    Set<String> fonts = new TreeSet<String>(FontFactory.getRegisteredFonts());
//	    for (String fontname : fonts) {
//	        logger.debug("registered font name : " + fontname);
//	    }
	    
	    if (exportFontname != null){
		    logger.debug("chosen font name: " + exportFontname);
		    mainExportFont = FontFactory.getFont(exportFontname, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
		    boldFont = FontFactory.getFont(exportFontname + " bold", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
		    italicFont = FontFactory.getFont(exportFontname + " italic", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
		    boldItalicFont = FontFactory.getFont(exportFontname + " bold italic", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
	    }
	    
	    mainExportFont = (mainExportFont != null && mainExportFont.getFamilyname() != "unknown")? mainExportFont : new Font(bfSerif);
	    mainExportBaseFont = (mainExportFont.getBaseFont() != null)? mainExportFont.getBaseFont() : bfSerif;
	    
	    boldFont = (boldFont != null && boldFont.getFamilyname() != "unknown")? boldFont : new Font(bfSerifBold);
	    boldBaseFont = (boldFont.getBaseFont() != null)? boldFont.getBaseFont() : bfSerifBold;
	    
	    italicFont = (italicFont != null && italicFont.getFamilyname() != "unknown")? italicFont : new Font(bfSerifItalic);
	    italicBaseFont = (italicFont.getBaseFont() != null)? italicFont.getBaseFont() : bfSerifItalic;
		
	    boldItalicFont = (boldItalicFont != null && boldItalicFont.getFamilyname() != "unknown")? boldItalicFont : new Font(bfSerifBoldItalic);
	    boldItalicBaseFont = (boldItalicFont.getBaseFont() != null)? boldItalicFont.getBaseFont() : bfSerifBoldItalic;

		logger.debug("main font family found: " + mainExportFont.getFamilyname());
    	logger.info("main base font for PDF export is: " + mainExportBaseFont.getPostscriptFontName());
	}
	

	@SuppressWarnings("unused")
	public void addPage(URL imgUrl, TrpDoc doc, PcGtsType pc, boolean addAdditionalPlainTextPage, boolean imageOnly, ImageMetadata md, boolean doBlackening, ExportCache cache) throws MalformedURLException, IOException, DocumentException, JAXBException, URISyntaxException {
		
		imgOnly = imageOnly;
		extraTextPage = addAdditionalPlainTextPage;
		//FIXME use this only on cropped (printspace) images!!
		java.awt.Rectangle printspace = null;
//		if(pc.getPage() != null && pc.getPage().getPrintSpace() != null){
//			java.awt.Polygon psPoly = PageXmlUtils.buildPolygon(pc.getPage().getPrintSpace().getCoords());
//			printspace = psPoly.getBounds();
//		}
		
		/*
		 * try to read image - if the image is not readable try the original one
		 */
		BufferedImage imgBuffer = null;
		try {			
			imgBuffer = TrpImageIO.read(imgUrl);
		} catch (FileNotFoundException e) {
			logger.error("File was not found at url " + imgUrl);
			if (imgUrl.getFile().endsWith("view")){
				imgUrl = new URL(imgUrl.getProtocol(), imgUrl.getHost(), imgUrl.getFile().replace("view", "orig"));	
			}
			else if (imgUrl.getFile().endsWith("orig")){
				imgUrl = new URL(imgUrl.getProtocol(), imgUrl.getHost(), imgUrl.getFile().replace("orig", "view"));	
			}
			logger.debug("try alternative file location " + imgUrl);
			imgBuffer = TrpImageIO.read(imgUrl);
		}
				
	    Graphics2D graph = imgBuffer.createGraphics();
	    graph.setColor(Color.BLACK);
	    
		List<TrpRegionType> regions = pc.getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
		
		//regions should be sorted after their reading order at this point - so no need to resort
		//Collections.sort(regions, new TrpElementCoordinatesComparator<RegionType>());
		int nrOfTextRegions = 0;

		for(RegionType r : regions){
			//TODO add paths for tables etc.
			
			//used later to decide if new page is necessary if there is at least one text OR table region 
			if(r instanceof TextRegionType || r instanceof TableRegionType){
				nrOfTextRegions++;
			}
			else if (r instanceof UnknownRegionType && doBlackening){
				UnknownRegionType urt = (UnknownRegionType) r;
				ITrpShapeType trpShape = (ITrpShapeType) r;
				boolean isBlackening = RegionTypeUtil.isBlackening(trpShape);
				if (isBlackening){
					//Rectangle blackRect = (Rectangle) PageXmlUtils.buildPolygon(urt.getCoords().getPoints()).getBounds();
					Rectangle blackRect = urt.getBoundingBox();
					graph.fillRect((int)blackRect.getMinX(), (int)blackRect.getMinY(), (int)blackRect.getWidth(), (int)blackRect.getHeight());
					
				}
			}
		}
				
		graph.dispose();
		
//		ByteArrayOutputStream baos=new ByteArrayOutputStream();
//		ImageIO.write(imgBuffer,"JPEG",baos);
//		byte[] imageBytes = baos.toByteArray();
//		Image img = Image.getInstance(imageBytes);
		
		//direct access instead of the version above
		Image img = Image.getInstance(imgUrl);
		
		//baos.close();
		imgBuffer.flush();
		imgBuffer = null;
		
		/*
		 * take resolution from metadata of image store, values in img are not always set
		 */
		if(md != null){
			double resolutionX = (float) md.getxResolution();
			double resolutionY = (float) md.getyResolution();
			//logger.debug("Dpi: " + md.getXResolution());
			img.setDpi((int)resolutionX, (int)resolutionY);
		}
		

//		else{
//		
//		 Image img = Image.getInstance(imgUrl);
//		}
		int cutoffLeft=0;
		int cutoffTop=0;
		
		if(printspace==null) {
			/*
			 * 1 Punkt pro cm  = 2,54 dpi
			 * img.getPlainWidth() = horizontal size in Pixel
			 * img.getPlainHeight() = vertical size in Pixel
			 * img.getDpiX() = resolution of x direction
			 * Size in cm: img.getDpiX() / (img.getDpiX()/2,54)
			 */
//			logger.debug("Horizontal size in cm: img.getPlainWidth() / (img.getDpiX()/2,54): " + img.getPlainWidth() / (img.getDpiX()/2.54));
//			logger.debug("Vertical size in cm: img.getPlainHeight() / (img.getDpiY()/2,54): " + img.getPlainHeight() / (img.getDpiY()/2.54));
			
			

			setPageSize(img);
		} else {
			int width=(int)printspace.getWidth();
			int height=(int)printspace.getHeight();
			setPageSize(new com.itextpdf.text.Rectangle(width, height));
			cutoffLeft=printspace.x;
			cutoffTop=printspace.y;
		}
		
		float xSize;
		float ySize;
		
		//FimgStoreImgMd imgMd = storage.getImageMetadata();
		

		
		/*
		 * calculate size of image with respect to Dpi of the image and the default points of PDF which is 72
		 * PDF also uses the same basic measurement unit as PostScript: 72 points == 1 inch
		 */
		if (img.getDpiX() > 72f){
			 xSize = (float) (img.getPlainWidth() / img.getDpiX()*72);
			 ySize = (float) (img.getPlainHeight() / img.getDpiY()*72);
			 scaleFactorX = scaleFactorY = (float) (72f / img.getDpiX());
		}
		else{
			xSize = (float) (img.getPlainWidth() / 300*72);
			ySize = (float) (img.getPlainHeight() / 300*72);
			scaleFactorX = scaleFactorY = 72f / 300;
		}
		
		/*
		 * construct the grid for the added page
		 */
        for (int i=0; i<=12; i++)
        {
                twelfthPoints[i][0] = i*(img.getPlainWidth()/12);
                twelfthPoints[i][1] = i*(img.getPlainHeight()/12);
        }
		
        //TODO use scaleToFit instead?
		img.scaleAbsolute(xSize, ySize);
		img.setAbsolutePosition(0, 0);
		
		/*
		 * calculate physical size of image in inch and assign text size dependent on these values
		 */
		if (img.getScaledWidth()/72f < 9 && img.getScaledHeight()/72f < 12){
			lineMeanHeight = 12/scaleFactorY;
		}
		else{
			lineMeanHeight = 17/scaleFactorY;
		}
		
//		logger.debug("img scaled width: " + img.getScaledWidth());
//		logger.debug("img scaled heigth: " + img.getScaledHeight());
		//System.in.read();

		//img.scalePercent(72f / img.getDpiX() * 100);
		//img.setAbsolutePosition(0, 0);
		//document.setPageSize(new Rectangle(img.getPlainWidth()*2, img.getPlainHeight()));
		
		if(doc != null && createTitle){
			addTitlePage(doc);
			//logger.debug("page number " + getPageNumber());
			if (getPageNumber()%1 != 0){
				logger.debug("odd page number -> add one new page");
				document.newPage();
				//necessary that an empty page can be created
				writer.setPageEmpty(false);
			}
		}
		
		document.newPage();
		addTextAndImage(pc ,cutoffLeft,cutoffTop, img, imageOnly, cache);	
		
		if(addAdditionalPlainTextPage){

			if (nrOfTextRegions > 0){
				logger.debug("add uniform text");
				document.newPage();			
				addUniformText(pc ,cutoffLeft,cutoffTop, cache);
			}
		}
	}
	


	private void addTextAndImage(PcGtsType pc, int cutoffLeft, int cutoffTop, Image img, boolean imageOnly, ExportCache cache) throws DocumentException, IOException {
		lineAndColorList.clear();
		
		PdfContentByte cb = writer.getDirectContentUnder();

		cb.setColorFill(BaseColor.BLACK);
		cb.setColorStroke(BaseColor.BLACK);
		//BaseFont bf = BaseFont.createFont(BaseFont.TIMES_ROMAN, "UTF-8", BaseFont.NOT_EMBEDDED);
		if (!imageOnly){
			cb.beginLayer(ocrLayer);
			//cb.setFontAndSize(bfArial, 32);
			cb.setFontAndSize(mainExportBaseFont, 32);
					
			List<TrpRegionType> regions = pc.getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
			
			/*
			 * use reading order comparator for sorting since at this time reading order is more trustable
			 * other sorting is not transitive and seldomly produces "Comparison violates its general contract" exception
			 */
//			Collections.sort(regions, new TrpElementReadingOrderComparator<RegionType>(true));
			TrpShapeTypeUtils.sortShapesByReadingOrderOrCoordinates(regions);
	
			for(RegionType r : regions){
				//TODO add paths for tables etc.
				if (r instanceof TrpTableRegionType){
					
					exportTable(r, cb, cutoffLeft, cutoffTop, false, cache);

				}
				else if(r instanceof TextRegionType){
					TextRegionType tr = (TextRegionType)r;
					//PageXmlUtils.buildPolygon(tr.getCoords().getPoints()).getBounds().getMinX();
					addTextFromTextRegion(tr, cb, cutoffLeft, cutoffTop, mainExportBaseFont, cache);
				}
			}
			
			//scale after calculating lineMeanHeightForAllRegions
			//lineMeanHeight = lineMeanHeight/scaleFactorX;
			
			cb.endLayer();
		}
				
		cb.beginLayer(imgLayer);		
		cb.addImage(img);
		cb.endLayer();
		
		if (highlightTags || highlightArticles){
			
			highlightAllTagsOnImg(lineAndColorList, cb, cutoffLeft, cutoffTop);
		}
		
		/*
		 * draw tag lines
		 */
		

		
//		addTocLinks(doc, page,cutoffTop);
	}
	
	private void exportTable(RegionType r, PdfContentByte cb, int cutoffLeft, int cutoffTop, boolean addUniformText, ExportCache cache) throws IOException, DocumentException {
		logger.debug("is table");
		TrpTableRegionType table = (TrpTableRegionType) r;
		
		int cols = table.getNCols();
		int rows = table.getNRows();
							
		List<List<TrpTableCellType>> allRowCells = new ArrayList<List<TrpTableCellType>>();
		for (int k = 0; k<rows; k++){
			allRowCells.add(table.getRowCells(k));
		}
		
        List<HashMap<Integer, TrpTableCellType>> allRows = new ArrayList<HashMap<Integer, TrpTableCellType>>();
        
        HashMap<Integer, TrpTableCellType> nextRowMap = new HashMap<Integer, TrpTableCellType>();
       
        for (List<TrpTableCellType> rowCells : allRowCells){
      
        	HashMap<Integer, TrpTableCellType> currRowMap = new HashMap<Integer, TrpTableCellType>();
        	
        	/*
        	 * fill up all cells which are not set in TRP (needed for vertical cell merge)
        	 * the nextRowMap contains already all cells which span vertically with the cells above - means they got merged 
        	 * in the table but have to be considered here 
        	 */
			currRowMap.putAll(nextRowMap);
			nextRowMap.clear();
        	
        	for (TrpTableCellType cell : rowCells){
            	//logger.debug("table cell text " + cell.getUnicodeTextFromLines());
            	currRowMap.put(cell.getCol(), cell);
            	if (cell.getRowSpan() > 1){
            		nextRowMap.put(cell.getCol(), null);
            	}
        	}
        	allRows.add(currRowMap);
        }
        
        for (HashMap<Integer, TrpTableCellType> entry : allRows) {
        	for (Integer key : entry.keySet()) {
        		if (entry.get(key) == null){
        			continue;
        		}
        		if (addUniformText){
					float textBlockXStart = getAverageBeginningOfBaselines(entry.get(key));
					textBlockXStart += 40;
					addUniformTextFromTextRegion(entry.get(key), cb, cutoffLeft, cutoffTop, mainExportBaseFont, textBlockXStart, cache);
				}
        		else{
        			addTextFromTextRegion(entry.get(key), cb, cutoffLeft, cutoffTop, mainExportBaseFont, cache);
        		}

        	}
        }
		
	}

	private void addUniformText(PcGtsType pc, int cutoffLeft, int cutoffTop, ExportCache cache) throws DocumentException, IOException {
		PdfContentByte cb = writer.getDirectContentUnder();
		cb.setColorFill(BaseColor.BLACK);
		cb.setColorStroke(BaseColor.BLACK);
	    /** The path to the font. */
	    //FontFactory.register("c:/windows/fonts/arialbd.ttf");
		//BaseFont bf = BaseFont.createFont("/fonts/arialbd.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
		
		cb.beginLayer(ocrLayer);
		//FontFactory.register("arialbd.ttf", "my_bold_font");
		//Font fontTest = FontFactory.getFont("arialbd.ttf", Font.BOLDITALIC);
		
		cb.setFontAndSize(mainExportBaseFont, 10);
				
		List<TrpRegionType> regions = pc.getPage().getTextRegionOrImageRegionOrLineDrawingRegion();
		
		/*
		 * use reading order comparator for sorting since at this time reading order is more trustable
		 * other sorting is not transitive and seldomly produces "Comparison violates its general contract" exception
		 */
//		Collections.sort(regions, new TrpElementReadingOrderComparator<RegionType>(true));
		TrpShapeTypeUtils.sortShapesByReadingOrderOrCoordinates(regions);
		
		float textBlockXStart = 0;

		int i = 0;
		for(TrpRegionType r : regions){
			//TODO add paths for tables etc.			
			if (r instanceof TrpTableRegionType){
				exportTable(r, cb, cutoffLeft, cutoffTop, true, cache);
			}
			else if(r instanceof TrpTextRegionType){
				TrpTextRegionType tr = (TrpTextRegionType) r;
				
				//compute average text region start
				//textBlockXStart = (float) (PageXmlUtils.buildPolygon(tr.getCoords().getPoints()).getBounds().getMinX());
				//double minX = PageXmlUtils.buildPolygon(tr.getCoords().getPoints()).getBounds().getMinX();
				//this should result in the the same value as the method in the line above which is deprecated
				double minX = tr.getBoundingBox().getMinX();
				double maxX = tr.getBoundingBox().getMaxX();
				double trWidth = tr.getBoundingBox().getWidth();
				
//				logger.debug("region "+ ++i);
//				logger.debug("region minX " + minX);
//				logger.debug("region maxX " + tr.getBoundingBox().getMaxX());

				//if there is only one text region at this vertical section start the text block at the second twelfth
				//if (hasSmallerColumn(regions, tr)){
				if (isOnlyRegionInThisRow(regions, tr)){
				//if (regions.size() == 1){
					logger.debug("only one region in this row!!");
					//indent start of text block under certain preconditions
					if (minX < twelfthPoints[1][0] && (twelfthPoints[1][0] < maxX && trWidth > twelfthPoints[2][0])){
						textBlockXStart = twelfthPoints[1][0];
					}
					//if textregion contains only one line this is probably a headline
					else if (tr.getTextLine().size() == 1){
						//logger.debug("tr.getTextLine().size() == 1 ");
						textBlockXStart = getPrintregionStartX((float) (minX), tr.getBoundingBox().getMaxX());
					}
					else if (twelfthPoints[2][0] < maxX && trWidth > twelfthPoints[3][0]){
						//logger.debug("twelfthPoints[2][0] < tr.getBoundingBox().getMaxX() ");
						textBlockXStart = twelfthPoints[2][0];
					}
					else{
						textBlockXStart = (float) minX;
					}
				}
				else{
					
					logger.debug("several columns found, minX of text region is : " + minX);
					//float startWithThisX = (float) (minX < smallerRegionMaxX ? smallerRegionMaxX : minX);
					//textBlockXStart = getPrintregionStartX((float) (startWithThisX));
					
					/*
					 * this is then used for all lines of a region as start point
					 */
					textBlockXStart = getAverageBeginningOfBaselines(tr);
					textBlockXStart += 40;

				}
				//logger.debug("textBlockXStart " + textBlockXStart);
				addUniformTextFromTextRegion(tr, cb, cutoffLeft, cutoffTop, mainExportBaseFont, textBlockXStart, cache);
			}
		}
		
		cb.endLayer();	
		
//		addTocLinks(doc, page,cutoffTop);
	}
	
	private boolean isOnlyRegionInThisRow(List<TrpRegionType> regions, TextRegionType regionToCompare) {
		float minX = 0;
		float minY = 0;
		float maxX = 0;
		float maxY = 0;
		float meanX = 0;
		float meanY = 0;
		
		java.awt.Rectangle compareBlock = regionToCompare.getBoundingBox();
		float compareMinX = (float) compareBlock.getMinX();
		float compareMinY = (float) compareBlock.getMinY();
		float compareMaxX = (float) compareBlock.getMaxX();
		float compareMaxY = (float) compareBlock.getMaxY();
		
		float compareMeanX = compareMinX+(compareMaxX - compareMinX)/2;
		float compareMeanY = compareMinY+(compareMaxY - compareMinY)/2;
		
		boolean foundSmallerColumn = false;
		
//		logger.debug("nr of regions " + regions.size());
//		logger.debug("regionToCompare id " + regionToCompare.getId());
		
		if (regions.size() == 1){
			return true;
		}
		else{

			for(RegionType r : regions){
				//TODO add paths for tables etc.
				if(r instanceof TextRegionType && r.getId() != regionToCompare.getId()){
					TextRegionType tr = (TextRegionType)r;
					
					//empty region can be ignored
					if (tr.getTextLine().isEmpty())
						continue;
					else{
						//region with empty lines can also be ignored
						boolean textFound = false;
						for (TextLineType tlt : tr.getTextLine()){
							TrpTextLineType l = (TrpTextLineType)tlt;
							textFound = !l.getUnicodeText().isEmpty();
							if (textFound){
								break;
							}
						}
						//no text in region -> go to next region
						if (!textFound){
							continue;
						}
					}
					//logger.debug("tr id " + tr.getId());

					//compute average text region start
					//java.awt.Rectangle block = PageXmlUtils.buildPolygon(tr.getCoords().getPoints()).getBounds();
					java.awt.Rectangle block = tr.getBoundingBox();
					minX = (float) block.getMinX();
					maxX = (float) block.getMaxX();
					minY = (float) block.getMinY();
					maxY = (float) block.getMaxY();
					
					//meanX = minX+(maxX - minX)/2;
					meanY = minY+(maxY - minY)/2;
					
					if ( ( (meanY > compareMinY && meanY < compareMaxY) ||
							(compareMeanY > minY && compareMeanY < maxY) ) ){

						return false;
					}	
				}
			}
		}
		return true;
	}

	/*
	 * calculate where the line alignment should be placed
	 * take average of starting points of all lines
	 * problem when some lines are on the right
	 * So take only lines starting within the first 1/10 of the text region width
	 */
	private float getAverageBeginningOfBaselines(TextRegionType tr) {
		
		//logger.debug("calculate average beginning of baselines ");
		
		float avgStartOfLines = 0;
		if (tr == null){
			return -1;
		}
		double width = tr.getBoundingBox().getWidth();
		float firstTenth = (float) (width/10);
		int nrOfLines = 0;

		for (TextLineType l : tr.getTextLine()){
			TrpBaselineType bl = (TrpBaselineType) l.getBaseline();
			if (bl != null && bl.getBoundingBox().getMinX() <= firstTenth){
				avgStartOfLines += bl.getBoundingBox().getMinX();
				nrOfLines += 1;
			}
			
		}
		if (nrOfLines > 0){
			avgStartOfLines = avgStartOfLines/nrOfLines;
			return avgStartOfLines;
		}
		return (float) tr.getBoundingBox().getMinX();       

    }
	
	private float getPrintregionStartX(float textBlockX, double textBlockMaxX) {
		
		float shortestDistance = 0;
		float closestX = twelfthPoints[2][0];
		int j = -1;

        for (int i=2; i<=12; i++)
        {

        	if (textBlockX < twelfthPoints[i][0]){
//                logger.debug("twelfthPoints[i][0]: " + twelfthPoints[i][0]);
//                logger.debug("The closest x of this textRegion is " + closestX);
        		j++;
                float distance = twelfthPoints[i][0] - textBlockX;

                //set shortestDistance and closestPoints to the first iteration
                if (j == 0)
                {
                    shortestDistance = distance;
                    closestX = twelfthPoints[i][0];
                }
                //then check if any further iterations have shorter distances
                else if (distance < shortestDistance)
                {
                    shortestDistance = distance;
                    if(i==12){
                    	closestX = twelfthPoints[i-1][0];
                    }
                    else{
                    	closestX = twelfthPoints[i][0];
                    }
                }
        	}
            
        }
//        logger.debug("The shortest distance is: " + shortestDistance);
//        logger.debug("The closest x of this textRegion is " + closestX);
//        try {
//			System.in.read();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        
        return closestX < textBlockMaxX ? closestX : textBlockX;
        

    }		
	
	/*
	 * idea is to divide each textregion into a grid (e.g. 10 columns) 
	 * and return the one which is nearest to the particular line
	 * result is a nice?? print layout
	 */
	private float getLinePositionInTextregionGrid(float[][] tenthRegion, double minXOfLine) {
			
		float shortestDistance = 0;
		float closestX = tenthRegion[1][0];
		int j = -1;

        for (int i=1; i<=10; i++)
        {

        	if (minXOfLine < tenthRegion[i][0]){
//                logger.debug("twelfthPoints[i][0]: " + twelfthPoints[i][0]);
//                logger.debug("The closest x of this textRegion is " + closestX);
        		j++;
                float distance = (float) (tenthRegion[i][0] - minXOfLine);
                distance = Math.abs(distance);

                //set shortestDistance and closestPoints to the first iteration
                if (j == 0)
                {
                    shortestDistance = distance;
                    closestX = tenthRegion[i][0];
                }
                //then check if any further iterations have shorter distances
                else if (distance < shortestDistance)
                {
                    shortestDistance = distance;
                    if(i==10){
                    	closestX = tenthRegion[i-1][0];
                    }
                    else{
                    	closestX = tenthRegion[i][0];
                    }
                }
        	}
            
        }      
        
        return closestX;

    }	
	

	private void addUniformTextFromTextRegion(final TextRegionType tr, final PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf, float lineStartX, ExportCache cache) throws IOException, DocumentException {
		List<TextLineType> lines = tr.getTextLine();
		if(lines != null && !lines.isEmpty()){
			int i = 0;
			float lineStartY = 0;
			
			//sort according to reading order
//			Collections.sort(lines, new TrpElementReadingOrderComparator<TextLineType>(true));
			TrpShapeTypeUtils.sortShapesByReadingOrderOrCoordinates(lines);
			
			double minY = 0;
			double maxY = 0;
			
			//get min and max values of region y direction for later calculation of textline height
			//java.awt.Rectangle regionRect = PageXmlUtils.buildPolygon(tr.getCoords().getPoints()).getBounds();
			
			int maxIdx = lines.size()-1;
			//java.awt.Rectangle firstLineRectOld = PageXmlUtils.buildPolygon(lines.get(0).getCoords().getPoints()).getBounds();
			//logger.debug("OLDDDDD: firstLineRectOld minX = " + firstLineRectOld.getMinX());
			java.awt.Rectangle firstLineRect = ((TrpTextLineType) lines.get(0)).getBoundingBox();
			//logger.debug("NEWWWWW: firstLineRect minX = " + firstLineRect.getMinX());
			//java.awt.Rectangle lastLineRect = PageXmlUtils.buildPolygon(lines.get(maxIdx).getCoords().getPoints()).getBounds();
			java.awt.Rectangle lastLineRect = ((TrpTextLineType) lines.get(maxIdx)).getBoundingBox();
					
			double firstLineRotation = computeRotation((TrpBaselineType) lines.get(0).getBaseline());
			double lastLineRotation = computeRotation((TrpBaselineType) lines.get(maxIdx).getBaseline());
			
			boolean isVerticalRegion = false;
			
			//first and last line rotated -> seems to be vertical
			//use X coords to compute the total line gap
			if (Math.abs(firstLineRotation) == 90 && Math.abs(lastLineRotation) == 90){
				
				//since the reading order is not clear if the text is vertically -> could be right to left or vice versa
				double tmpMinX1 = firstLineRect.getMinX();
				double tmpMinX2 = lastLineRect.getMinX();
				
				double tmpMaxX1 = firstLineRect.getMaxX();
				double tmpMaxX2 = lastLineRect.getMaxX();
				
				minY = Math.min(tmpMinX1, tmpMinX2);
				maxY = Math.max(tmpMaxX1, tmpMaxX2);
				
				isVerticalRegion = true;
			}
			else{
			
				minY = firstLineRect.getMinY();
				maxY = lastLineRect.getMaxY();
			}
			
			/*
			 * if start of line is too tight on the upper bound - set to the first 1/12 of t page from above
			 * BUT: Is not good since page number and other informations are often in this section
			 */
//			if (minY < twelfthPoints[1][1]){
//				minY = twelfthPoints[1][1];
//			}		
//			for(TextLineType lt : lines){
//				
//				TrpTextLineType l = (TrpTextLineType)lt;
//				java.awt.Rectangle lineRect = PageXmlUtils.buildPolygon(l.getCoords().getPoints()).getBounds();
//				
//
//				
//				if (lines.size() == 1){
//					minY = lineRect.getMinY();
//					maxY = lineRect.getMaxY();
//					
//				}
//				else if (l.getIndex() == 0){
//					minY = lineRect.getMinY();
//				}
//				else if (l.getIndex() == lines.size()-1){
//					maxY = lineRect.getMaxY();
//				}
//				
//			}
			
			double lineGap = (maxY - minY)/lines.size();
			
			//use default values if only one line  and no previous line mean height computed
			if (lines.size() == 1){
				lineMeanHeight = (prevLineMeanHeight != 0 ? prevLineMeanHeight : lineMeanHeight); 
			}
			else if (lines.size() > 1){
				lineMeanHeight = (float) (2*(lineGap/3));
				leading = (int) (lineGap/3);
				prevLineMeanHeight = lineMeanHeight;
				//logger.debug("Line Mean Height for Export " + lineMeanHeight);
				//overallLineMeanHeight = ( (overallLineMeanHeight != 0) ? overallLineMeanHeight+lineMeanHeight/2 : lineMeanHeight);
			}
			
			//logger.debug("Line Mean Height for Export " + lineMeanHeight);
			
			for(TextLineType lt : lines){

				wordOffset = 0;
				TrpTextLineType l = (TrpTextLineType)lt;
				TrpBaselineType baseline = (TrpBaselineType) l.getBaseline();

				java.awt.Rectangle lineRect = l.getBoundingBox();//PageXmlUtils.buildPolygon(l.getCoords().getPoints()).getBounds();
				java.awt.Rectangle baseLineRect = baseline == null ? null : baseline.getBoundingBox();//PageXmlUtils.buildPolygon(baseline.getPoints()).getBounds();
				
				if (baseLineRect == null){
					logger.debug("Baseline is null - ignore this line");
					continue;
				}
				
				float tmpLineStartX = lineStartX;
				float regionStartMinX = (float) tr.getBoundingBox().getMinX();//PageXmlUtils.buildPolygon(tr.getCoords().getPoints()).getBounds().getMinX();
				double regionWidth = tr.getBoundingBox().getWidth();
				
				//first line
				if (i == 0){
					
					lineStartY = (float) (minY + lineMeanHeight);

					/*
					 * if first line of a text region is indented then take this into account in printed text
					 */
					if (lineRect.getMinX() > regionStartMinX){
						if (lineRect.getMinX() - regionStartMinX > regionWidth/4){
							//tmpLineStartX = (float) lineStartX + twelfthPoints[1][0];
							tmpLineStartX = (float) baseLineRect.getMinX();
						}
					}
					
				}
				
				//for subsequent lines
				else{
					/*
					 * construct the grid for the current text region
					 */
//					float[][] twelfthRegion = new float[11][2];
//					Rectangle trRect = tr.getBoundingBox();
//			        for (int j=0; j<=10; j++)
//			        {
//			        	twelfthRegion[j][0] = (float) (trRect.getMinX() + (float) (j*(tr.getBoundingBox().getWidth()/10)));
//			        	//twelfthRegion[j][1] = (float) (trRect.getMinX() + (float) (j*(tr.getBoundingBox().getHeight()/12)));
//			        }
			        
					if (lineRect.getMinX() > regionStartMinX){
						if (lineRect.getMinX() - regionStartMinX > regionWidth/4){
							//tmpLineStartX = (float) lineStartX + twelfthPoints[1][0];
							tmpLineStartX = (float) baseLineRect.getMinX();
						}
					}
			        
			        //tmpLineStartX = getLinePositionInTextregionGrid(twelfthRegion, lineRect.getMinX());
					lineStartY = lineStartY + lineMeanHeight + leading;
					
					
//					for (TrpTextRegionType region : tr.getPage().getTextRegions(true)){
//
//						double regionMinX = PageXmlUtils.buildPolygon(region.getCoords().getPoints()).getBounds().getMinX();
//						double regionMaxX = PageXmlUtils.buildPolygon(region.getCoords().getPoints()).getBounds().getMaxX();
//						Rectangle rec = PageXmlUtils.buildPolygon(region.getCoords().getPoints()).getBounds();
//
//						if (rec.contains(tmpLineStartX, lineStartY) && !tr.getId().equals(region.getId()) && tmpLineStartX < regionMaxX){
//							logger.debug("region contains point " + tr.getId() + " region ID " + region.getId());
//							tmpLineStartX = (float) regionMaxX;
//							break;
//						}
//					
//						
//					}
					
					
//					if (lineRect.getMinX() > lineStartX){
//						if (lineRect.getMinX() - lineStartX > twelfthPoints[1][0]){
//							tmpLineStartX = (float) lineRect.getMinX();
//						}
//					}
					
				}
				
				if(baseLineRect != null && regionStartMinX < baseLineRect.getMinX() && (baseLineRect.getMinX()-regionStartMinX) > twelfthPoints[1][0]){
					
					//logger.debug("try to find smaller region for baseline !!!!!!! " );
					for (TrpTextRegionType region : tr.getPage().getTextRegions(false)){

						if (!region.getId().equals(tr.getId())){

							double regionMinX = region.getBoundingBox().getMinX();//PageXmlUtils.buildPolygon(region.getCoords().getPoints()).getBounds().getMinX();
							double regionMaxX = region.getBoundingBox().getMaxX();
							double regionMinY = region.getBoundingBox().getMinY();
							double regionMaxY = region.getBoundingBox().getMaxY();
							double meanX = regionMinX+(regionMaxX-regionMinX)/2;
	
							//Rectangle rec = PageXmlUtils.buildPolygon(region.getCoords().getPoints()).getBounds();
							
//							logger.debug("meanX " + meanX);
//							logger.debug("regionStartMinX " + regionStartMinX);
//							logger.debug("baseLineRect.getMinX() " + baseLineRect.getMinX());
//							
//							logger.debug("regionMaxY " + regionMaxY);
//							logger.debug("baseLineRect.getMaxY() " + baseLineRect.getMaxY());
//							
//							logger.debug("baseLineRect.getMinY() " + baseLineRect.getMinY());
//							logger.debug("regionMinY " + regionMinY);
													
							//another region before the lines 
							if (meanX > regionStartMinX && meanX < baseLineRect.getMinX() && baseLineRect.getMinY() < regionMaxY && baseLineRect.getMinY() > regionMinY ){
								tmpLineStartX = (float) regionMaxX + lineMeanHeight;
								logger.debug("region " + region.getId() + " overlaps this other region " + tr.getId());
								//logger.debug("new tmplineStartX is " + regionMaxX);
								break;
							}	
						}
						
					}
					
					//tmpLineStartX = (float) baseLineRect.getMinX();
				}
				
				i++;

				/*
				 * word level bei uniform output nicht sinnvoll?
				 * besser nur ganze lines ausgeben
				 */
//				if(useWordLevel && !l.getWord().isEmpty()){
//					List<WordType> words = l.getWord();
//					for(WordType wt : words){
//						TrpWordType w = (TrpWordType)wt;
//						if(!w.getUnicodeText().isEmpty()){
//							java.awt.Rectangle boundRect = PageXmlUtils.buildPolygon(w.getCoords()).getBounds();
//							
//							addUniformString(boundRect, lineMeanHeight, lineStartX, lineStartY, w.getUnicodeText(), cb, cutoffLeft, cutoffTop, bf);
//						} else {
//							logger.info("No text content in word: " + w.getId());
//						}
//					}
//				} else if(!l.getUnicodeText().isEmpty()){
				
				
				/*
				 * make chunks out of the lineText
				 * so it is possible to have differnt fonts, underlines and other text styles in one line
				 * 
				 * possible text styles are:
				 * 		new CustomTagAttribute("fontFamily", true, "Font family", "Font family"),
						new CustomTagAttribute("serif", true, "Serif", "Is this a serif font?"),
						new CustomTagAttribute("monospace",true, "Monospace", "Is this a monospace (i.e. equals width characters) font?"),
						new CustomTagAttribute("fontSize", true, "Font size", "The size of the font in points"),
						new CustomTagAttribute("kerning", true, "Kerning", "The kerning of the font, see: http://en.wikipedia.org/wiki/Kerning"),
						new CustomTagAttribute("textColour", true, "Text colour", "The foreground colour of the text"),
						new CustomTagAttribute("bgColour", true, "Background colour", "The background colour of the text"),
						new CustomTagAttribute("reverseVideo", true, "Reverse video", "http://en.wikipedia.org/wiki/Reverse_video"),
						new CustomTagAttribute("bold", true, "Bold", "Bold font"),
						new CustomTagAttribute("italic", true, "Italic", "Italic font"),
						new CustomTagAttribute("underlined", true, "Underlined", "Underlined"),
						new CustomTagAttribute("subscript", true, "Subscript", "Subscript"),
						new CustomTagAttribute("superscript", true, "Superscript", "Superscript"),
						new CustomTagAttribute("strikethrough", true, "Strikethrough", "Strikethrough"),
						new CustomTagAttribute("smallCaps", true, "Small caps", "Small capital letters at the height as lowercase letters, see: http://en.wikipedia.org/wiki/Small_caps"),
						new CustomTagAttribute("letterSpaced", true, "Letter spaced", "Equals distance between characters, see: http://en.wikipedia.org/wiki/Letter-spacing"),
				 */ 
				List<Chunk> chunkList = new ArrayList<Chunk>();

				/*
				 * if line is empty -> use the words of this line as line text
				 * otherwise take the text in the line
				 */
				List<TextStyleTag> styleTags = new ArrayList<TextStyleTag>();
				
				String shapeText = "";
				
				if(l.getUnicodeText().isEmpty() || useWordLevel){
					//logger.debug("in word based path " + useWordLevel);
					List<WordType> words = l.getWord();
					
					int chunkIndex = 0;
					for(WordType wt : words){
						TrpWordType w = (TrpWordType)wt;
						String wordText = "";
						//add empty space after each word
						if (chunkIndex > 0){
							chunkList.add(chunkIndex, new Chunk(" "));
							chunkIndex++;
						}
						if(!w.getUnicodeText().isEmpty()){
							//remember all style tags for text formatting later on
							styleTags.addAll(w.getTextStyleTags());
							if (!shapeText.equals("")){
								shapeText = shapeText.concat(" ");
							}
							wordText = wordText.concat(w.getUnicodeText());	
							shapeText = shapeText.concat(w.getUnicodeText());

							for (int j=0; j<wordText.length(); ++j) {
								
								String currentCharacter = wordText.substring(j, j+1);

								chunkList.add(chunkIndex, formatText(currentCharacter, styleTags, j, w, cache));
								chunkIndex++;

							}
							styleTags.clear();
						}
					}
				}
				else if (!l.getUnicodeText().isEmpty()){
					String lineText = l.getUnicodeText();
					shapeText = lineText;
					//logger.debug("line Text is " + lineText);
					styleTags.addAll(l.getTextStyleTags());
					for (int j=0; j<lineText.length(); ++j) {
						
						String currentCharacter = lineText.substring(j, j+1);

						chunkList.add(j, formatText(currentCharacter, styleTags, j, l, cache));

					}
				}
				//empty shape
				else{
					logger.debug("empty shape ");
					continue;
				}
				
				Phrase phrase = new Phrase();
				
				//trim is important to get the 'real' first char for rtl definition
				boolean rtl = textIsRTL(shapeText.trim());
				if (rtl){
					logger.debug("&&&&&&&& STRING IS RTL : ");
				}
				
				/*
				 * add all runs after the actual shape chars are analized and finished
				 * for text right to left (rtl) we need to reverse the content
				 */
				
				for (int j = chunkList.size()-1; j>=0; j--){
					if (rtl){
						phrase.add(chunkList.get(j));
					}
					else{
						phrase.addAll(chunkList);
						break;
					}
				}
				
				//phrase.addAll(chunkList);
				
				//logger.debug("curr phrase is: " + phrase.getContent());
				
				
				//compute rotation of text, if rotation higher PI/16 than rotate otherwise even text
				/*
				 * No rotation for single lines in a overall horizontal text region 
				 * Reason: Vertical line uses too much space - calculated for horizontal
				 */
				double rotation = 0;
				if (isVerticalRegion){

					rotation = (baseline != null ? computeRotation(baseline) : 0);
				
					if(rotation != 0){
						/*
						 * if we rotate e.g. 90° than we should use the actual x location of the line
						 * so vertical text must be treated different than horizontal text 
						 */
						if (baseLineRect != null){
							if (rtl){
								tmpLineStartX = (float) baseLineRect.getMaxX();
							}
							else{
								tmpLineStartX = (float) baseLineRect.getMinX();
							}
							
							lineStartY = (rotation < 0) ? (float) baseLineRect.getY() : (float) baseLineRect.getMaxY();
//							logger.debug("bl_rect maxY " + baseLineRect.getMaxY());
//							logger.debug("bl_rect minY " + baseLineRect.getMinY());
//							logger.debug("bl_rect Y " + baseLineRect.getY());
						}
						else if(lineRect != null){
							tmpLineStartX = lineRect.x;
							lineStartY = (float) lineRect.getY();
						}
						
					}
				}

				//blacken Strings if wanted
//				Set<Entry<CustomTag, String>> blackSet = CustomTagUtils.getAllTagsOfThisTypeForShapeElement(l, RegionTypeUtil.BLACKENING_REGION.toLowerCase()).entrySet();
//				
//				if (!lineText.equals("") && doBlackening && blackSet.size() > 0){
//					
//					//for all blackening regions replace text with ****
//					for (Map.Entry<CustomTag, String> currEntry : blackSet){
//						
//						if (!currEntry.getKey().isIndexed()){
//							//logger.debug("line not indexed : " + lineText);
//							lineText = lineText.replaceAll(".", "*");
//						}
//						else{		
//							lineText = blackenString(currEntry, lineText);
//							//logger.debug("lineText after blackened : " + lineText);
//						}
//					}
//				}

				//for rtl export
				float lineEndX = 0;
				float width = 0;
				if (baseLineRect != null){
					lineEndX = (float) baseLineRect.getMaxX();
					width = (float) baseLineRect.getWidth();
					//this leads to an extra start for each line instead of having a combined start for all lines in a region
					//tmpLineStartX = (float) (lineEndX - baseLineRect.getWidth());
				}
				else if(lineRect != null){
					lineEndX = lineRect.x + lineRect.width;
					width = (float) lineRect.getWidth();
				}
				
//				logger.debug("tmpLineStartX " + tmpLineStartX);
//				logger.debug("lineEndX " + lineEndX);
				
				//mainly for very small regions at the very left of a page
				if(tmpLineStartX > lineEndX){
					lineEndX = tmpLineStartX + width;
				}
				
//				logger.debug("width " + width);
//				logger.debug("lineEndX " + lineEndX);

				//first add uniform String (=line), ,after that eventaully highlight the tags in this line using the current line information like x/y position, 
				//addUniformString(lineMeanHeight, tmpLineStartX, lineStartY, lineText, cb, cutoffLeft, cutoffTop, bf, twelfthPoints[1][0], false, null, rotation);
				addUniformString(tr.getBoundingBox(), lineMeanHeight, tmpLineStartX, lineStartY, lineEndX, phrase, cb, cutoffLeft, cutoffTop, bf, twelfthPoints[1][0], false, null, rotation, rtl);
				
				/*
				 * old:
				 * highlight all tags of this text line if property is set
				 * no highlighting is done during chunk formatting and not in an extra step
				 */
//				if (highlightTags){
//		
//
//					Set<Entry<CustomTag, String>> entrySet = CustomTagUtils.getAllTagsForShapeElement(l).entrySet();
//					
//					highlightUniformString(entrySet, tmpLineStartX, lineStartY, l, cb, cutoffLeft, cutoffTop, bf);
//					
//					List<WordType> words = l.getWord();
//					for(WordType wt : words){
//						TrpWordType w = (TrpWordType)wt;
//						
//						Set<Entry<CustomTag, String>> entrySet2 = CustomTagUtils.getAllTagsForShapeElement(w).entrySet();
//						
//						highlightUniformString(entrySet2, tmpLineStartX, lineStartY, l, cb, cutoffLeft, cutoffTop, bf);
//					}					
//
//				}
			}
		}	
	}
	
	
	private double computeRotation(TrpBaselineType baseline) {
		
		if (baseline==null){
			return 0;
		}
		double rotation = 0;
		List<Point> lp;
		try {
			
			lp = PointStrUtils.parsePoints(baseline.getCoordinates());
	
			Point p2 = lp.get(lp.size()-1);
			Point p1 = lp.get(0);
			
			double gk = Math.abs(p2.getY() - p1.getY());
			double ak = Math.abs(p2.getX() - p1.getX());
			double alpha = (Math.atan2(gk, ak));
			
			if(p1.y < p2.y){
				alpha = -alpha;
			}
			
	//		logger.debug("p1.y " + p1.y);
	//		logger.debug("p2.y " + p2.y);
	//		
	//		logger.debug("Rotate this content? " + phrase.getContent());
			
			
			// if rotation is not over this border keep it straight
			if (Math.abs(alpha) > Math.PI/16){
				//convert from RAD to DEG
				rotation = alpha*57.296;
			}
			
			//with this steepness we change to a 90° rotation
			if (Math.abs(alpha) > 7*Math.PI/16){
	//			tmpLineStartX = p1.x;
	//			lineStartY = p1.y;
				rotation = ((p1.y < p2.y)? -90 : 90);
				
				//logger.debug("alpha to rotate " + alpha);
			}
					
	//					if (alpha > Math.PI/8 && alpha <= Math.PI/3){
	//						rotation = alpha;
	//					}
	//					//=90° in degrees
	//					else if (alpha > Math.PI/3){ 
	//						rotation = Math.(Math.PI/9);
	//					}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return rotation;
	}

	private Chunk formatText(String currCharacter, List<TextStyleTag> styleTags, int currentIndex, ITrpShapeType currShape, ExportCache cache) throws IOException {
		
		//first blacken char if needed
		Set<Entry<CustomTag, String>> blackSet = ExportUtils.getAllTagsOfThisTypeForShapeElement(currShape, RegionTypeUtil.BLACKENING_REGION.toLowerCase()).entrySet();
		
		if (!currCharacter.equals("") && doBlackening && blackSet.size() > 0){

			//for all blackening regions replace text with ****
			for (Map.Entry<CustomTag, String> currEntry : blackSet){
				
				int beginIndex = currEntry.getKey().getOffset();
				int endIndex = beginIndex + currEntry.getKey().getLength();
				
				if(currentIndex >= beginIndex && currentIndex < endIndex){
					currCharacter = "*";
				}
			}
		}
		
		//create new chunk
		Chunk currChunk = new Chunk(currCharacter);
//		Font arial = new Font(bfArial, lineMeanHeight);
//		Font arialBold = new Font(bfArialBold, lineMeanHeight);
//		Font arialItalic = new Font(bfArialItalic, lineMeanHeight);
		//currChunk.setFont(FontFactory.getFont("times-roman", BaseFont.CP1252, BaseFont.EMBEDDED));
		currChunk.setFont(mainExportFont);
		
		Set<Entry<CustomTag, String>> commentSet = ExportUtils.getAllTagsOfThisTypeForShapeElement(currShape, "comment").entrySet();
		for (Map.Entry<CustomTag, String> currEntry : commentSet){
			
			int beginIndex = currEntry.getKey().getOffset();
			int endIndex = beginIndex + currEntry.getKey().getLength();
			
			if(currentIndex >= beginIndex && currentIndex < endIndex){
				//hex string #FFF8B0: yellow color
				currChunk.setBackground(new BaseColor(Color.decode("#FFF8B0").getRGB()));
			}
		}
//		TrpWordType w = null;
//		if (useWordLevel){
//			List<WordType> words = currShape.getWord();
//
//			for(WordType wt : words){
//				w = (TrpWordType)wt;
////				logger.debug("index " + j);
////				logger.debug("word " + w.getUnicodeText());
//				if(!w.getUnicodeText().isEmpty()){
//					if (!(currentIndex < (wordOffset+w.getUnicodeText().length()+1))){
//						wordOffset = wordOffset+w.getUnicodeText().length()+1;
//						
//					}
//					break;
//				}
//			}
//		}
//		
//		logger.debug("curr offset und index " + wordOffset + " " + currentIndex);
		

		
		
		/*
		 * format according to custom style tag - check for each char in the text if a special style should be set
		 */
		for (TextStyleTag styleTag : styleTags){
			
			if (currentIndex >= (wordOffset+styleTag.getOffset()) && currentIndex < (wordOffset+styleTag.getOffset()+styleTag.getLength())){
				
				boolean bold = CoreUtils.val(styleTag.getBold());
				boolean italic = CoreUtils.val(styleTag.getItalic());
				
				Font styleFont = null;
				if (bold && italic){
					styleFont = boldItalicFont;
				}
				else if (bold){
					styleFont = boldFont;
				}
				else if (italic){
					styleFont = italicFont;
				}
				
				if (styleFont != null){
					currChunk.setFont(styleFont);
				}
				
				/*
				 * idea was to take the user set font size but it destroys the overall uniformity - the generated size is normally bigger then the set size (e.g. 9.5 is rather small)
				 * use case: It would help a lot if at least bold, italics and font-size where represented properly in the PDF produced by Transkribus, 
				 * because we use GROBID-dictionaries for entry segmentation, a tool that wants PDF as input and extracts features like font-style from the text layer; 
				 * these features are then used to train the tool so it learns where to insert segmentation markup. (docId 53351, page 1)
				 * 
				 * bodl and italics is contained
				 */
//				if (styleTag.getFontSize() != null && styleTag.getFontSize() > 0){
//					//currChunk.setLineHeight(styleTag.getFontSize()/scaleFactorY);
//					//logger.debug("SIIIIIIIZE" + styleTag.getFontSize());
//					//currChunk.setFont(FontFactory.getFont(BaseFont.TIMES_BOLD, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 12));
//					Font fontToUse = styleFont != null ? new Font(styleFont.getBaseFont(), styleTag.getFontSize()) : new Font(mainExportFont.getBaseFont(), styleTag.getFontSize());
//					currChunk.setFont(fontToUse);
//				}
				
//				if (styleTag.getFontFamily() != null){
//					currChunk.setFont(new Font();
//				}
								
//				if (CoreUtils.val(styleTag.getBold())) {
//					//logger.debug("BOOOOOOOOOLD");
//					if (styleTag.getFontSize() != null && styleTag.getFontSize() > 0){
//						currChunk.setFont(new Font(bfSerifBold, styleTag.getFontSize()));
//					}
//					else{
//						currChunk.setFont(boldFont);
//					}
//					
//				}			
//				if (CoreUtils.val(styleTag.getItalic())) {
//					//logger.debug("ITAAAAAAAAAAAALIC");
//					if (styleTag.getFontSize() != null && styleTag.getFontSize() > 0){
//						currChunk.setFont(new Font(bfSerifItalic, styleTag.getFontSize()));
//					}
//					else{
//						currChunk.setFont(italicFont);
//					}
//					
//				}
				if (CoreUtils.val(styleTag.getStrikethrough())) {
					//logger.debug("Striiiiiiiiikethrough");
					currChunk.setUnderline(0.2f, 3f);
				}
//				if (CoreUtils.val(ts.isSubscript())) {
//					text = RtfText.subscript(text);
//				}
//				if (CoreUtils.val(ts.isSuperscript())) {
//					text = RtfText.superscript(text);
//				}
				if (CoreUtils.val(styleTag.getUnderlined())) {
					//logger.debug("Underliiiiiiined");
					currChunk.setUnderline(0.2f, -3f);
				}
				
			}

		}
		
		if (highlightTags){
			
			Set<Entry<CustomTag, String>> entrySet;
			entrySet = ExportUtils.getAllTagsForShapeElement(currShape).entrySet();
			
			
			int k = 1;
			int tagId = 0;
			int [] prevLength = new int[entrySet.size()];
			int [] prevOffset = new int[entrySet.size()];
			
			for (Map.Entry<CustomTag, String> currEntry : entrySet){
				
				//Set<String> wantedTags = ExportUtils.getOnlyWantedTagnames(CustomTagFactory.getRegisteredTagNames());
				Set<String> wantedTags = cache.getOnlySelectedTagnames(CustomTagFactory.getRegisteredTagNames());
				
				if (wantedTags.contains(currEntry.getKey().getTagName())){

//					logger.debug("current tag name "+ currEntry.getKey().getTagName());
//					logger.debug("current tag text "+ currEntry.getKey().getContainedText());
					String color = CustomTagFactory.getTagColor(currEntry.getKey().getTagName());
										
					int currLength = currEntry.getKey().getLength();
					int currOffset = wordOffset+currEntry.getKey().getOffset();

					if (color!=null && currentIndex >= (currOffset) && currentIndex <= (currOffset+currLength)){
						
						/**
						 * if the current tag overlaps one of the previous tags
						 * -> increase the distance of the line under the textline
						 */
						if (isOverlaped(prevOffset, prevLength, currOffset, currLength)){
							k++;
							//logger.debug("overlapped is true, k = " + k);
							
						}
						else{
							k=1;
							//logger.debug("overlapped is not true, k = " + k);
							
						}
						
						currChunk.setUnderline(new BaseColor(Color.decode(color).getRGB()), 0.8f, 0.0f, -2f*+1f*k, 0.0f, PdfContentByte.LINE_CAP_BUTT);
						
						//logger.debug("UNDERLINE curr chunk " + currChunk.getContent() + " k = " + k);
					}
										
					prevOffset[tagId] = currOffset;
					prevLength[tagId] = currLength;
					tagId++;
					

					
					//yShift -> vertical shift of underline if several tags are at the same position
					//float yShift = (lineMeanHeight/6) * k;
				}
			}
			
		}
	
		//logger.debug("chunk content is " + currChunk.getContent());
		return currChunk;
	}
	
	
	//not used anymore
	private void highlightUniformString(Set<Entry<CustomTag, String>> entrySet, float tmpLineStartX, float lineStartY, TrpTextLineType l, PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf) throws IOException {
		
		int k = 1;
		int tagId = 0;
		int [] prevLength = new int[entrySet.size()];
		int [] prevOffset = new int[entrySet.size()];
		
		for (Map.Entry<CustomTag, String> currEntry : entrySet){
			
			Set<String> wantedTags = ExportUtils.getOnlyWantedTagnames(CustomTagFactory.getRegisteredTagNames());
			
			if (wantedTags.contains(currEntry.getKey().getTagName())){

				
				//logger.debug("current tag text "+ currEntry.getKey().getContainedText());
				String color = CustomTagFactory.getTagColor(currEntry.getKey().getTagName());
				
				int currLength = currEntry.getKey().getLength();
				int currOffset = currEntry.getKey().getOffset();
				
				/**
				 * if the current tag overlaps one of the previous tags
				 * -> increase the distance of the line under the textline
				 */
				if (isOverlaped(prevOffset, prevLength, currOffset, currLength)){
					k++;
				}
				else{
					k=1;
				}
				
				prevOffset[tagId] = currOffset;
				prevLength[tagId] = currLength;
				tagId++;
				
				//yShift -> vertical shift of underline if several tags are at the same position
				float yShift = (lineMeanHeight/6) * k;
				highlightUniformTagString(lineMeanHeight, tmpLineStartX, lineStartY, l.getUnicodeText(), currEntry.getKey().getContainedText(), cb, cutoffLeft, cutoffTop, bf, twelfthPoints[1][0], color, yShift, currOffset);
			}
		}
		
	}

	/*
	 * to find out if two tags overlap each other
	 */
	private boolean isOverlaped(int[] prevOffset, int[] prevLength,
			int currOffset, int currLength) {
		int currX1 = currOffset;
		int currX2 = currOffset+currLength;
		for (int i = 0; i < prevOffset.length; i++){
			int prevX1 = prevOffset[i];
			int prevX2 = prevOffset[i]+prevLength[i];
			if ( (currX1>=prevX1 && currX1<prevX2) || (currX2 > prevX1 && currX2 <= prevX2) ){
				return true;
			}
		}
		return false;
	}
	
	/*
	 * to find out if two tags overlap each other
	 */
	private int getAmountOfOverlaps(int[] prevOffset, int[] prevLength,
			int currOffset, int currLength) {
		int currX1 = currOffset;
		int currX2 = currOffset+currLength;
		
		int countOverlaps = 1;
		for (int i = 0; i < prevOffset.length; i++){
			int prevX1 = prevOffset[i];
			int prevX2 = prevOffset[i]+prevLength[i];
			if ( (currX1>=prevX1 && currX1<prevX2) || (currX2 > prevX1 && currX2 <= prevX2) ){
				countOverlaps++;
			}
		}
		return countOverlaps;
	}

	private void addTextFromTextRegion(final TextRegionType tr, final PdfContentByte cb, int cutoffLeft, int cutoffTop, BaseFont bf, ExportCache cache) throws IOException {
		List<TextLineType> lines = (tr != null ? tr.getTextLine() : null);
		
		boolean firstLine;
		if(lines != null && !lines.isEmpty()){
			//sort according to reading order

//			Collections.sort(lines, new TrpElementReadingOrderComparator<TextLineType>(true));
			TrpShapeTypeUtils.sortShapesByReadingOrderOrCoordinates(lines);
			
			double baseLineMeanY = 0;
			double baseLineMeanYPrev = 0;
			double baseLineMeanGap = 0;
			//logger.debug("Processing " + lines.size() + " lines in TextRegion " + tr.getId());
			for(TextLineType lt : lines){
				
				TrpTextLineType l = (TrpTextLineType)lt;
				//java.awt.Rectangle lineRect = PageXmlUtils.buildPolygon(l.getCoords().getPoints()).getBounds();
				
				//compute rotation of text, if rotation higher PI/16 than rotate otherwise even text
				TrpBaselineType baseline = (TrpBaselineType) l.getBaseline();
				double rotation = (baseline != null ? computeRotation(baseline) : 0);
				
//				if (lineRect.height > 0){
//					float lineHeight = lineRect.height /3;
//					
//					logger.debug("line height: "+ lineHeight);
//					
//					//ignore actual lineHeigth if three times the size of the actual line mean heigth
//					if (!(lineHeight > lineMeanHeight*4) || lineMeanHeight == 0){
//						//calculate line mean Height
//						lineMeanHeight = (lineMeanHeight == 0 ? lineHeight : (lineMeanHeight + lineHeight)/2);
//						logger.debug("lineMeanHeight: "+ lineMeanHeight);
//					}
//				}
				//get the mean baseline y-value
				baseLineMeanYPrev = baseLineMeanY;
				if(baseline != null){
					//use lowest point in baseline and move up one half of the distance to the topmost point
					java.awt.Rectangle baseLineRect = l.getBoundingBox();
					baseLineMeanY =  baseLineRect.getMaxY() - ((baseLineRect.getMaxY() - baseLineRect.getMinY())/2);
					if (baseLineMeanYPrev != 0){
						baseLineMeanGap = baseLineMeanY - baseLineMeanYPrev;
					}
				}
				
				boolean rtl = false;
					
				if( (l.getUnicodeText().isEmpty() || useWordLevel) && !l.getWord().isEmpty()){

					List<WordType> words = l.getWord();
					for(WordType wt : words){
						TrpWordType w = (TrpWordType)wt;
						if(!w.getUnicodeText().isEmpty()){
							//java.awt.Rectangle boundRect = PageXmlUtils.buildPolygon(w.getCoords()).getBounds();
							java.awt.Rectangle boundRect = w.getBoundingBox();
							String text = w.getUnicodeText();
							rtl = textIsRTL(text.trim());
							addString(boundRect, baseLineMeanY, text, cb, cutoffLeft, cutoffTop, bf, rotation, rtl);
						} else {
							//logger.info("No text content in word: " + w.getId());
						}
						
					}
				} else if(!l.getUnicodeText().isEmpty()){
					
					String lineTextTmp = l.getUnicodeText();
					//get surrounding rectangle coords of this line
					java.awt.Rectangle boundRect = l.getBoundingBox();
					
					Set<Entry<CustomTag, String>> blackSet = ExportUtils.getAllTagsOfThisTypeForShapeElement(l, RegionTypeUtil.BLACKENING_REGION.toLowerCase()).entrySet();
					
					if (doBlackening && blackSet.size() > 0){
						
						//for all blackening regions replace text with ****
						for (Map.Entry<CustomTag, String> currEntry : blackSet){
							
							if (!currEntry.getKey().isIndexed()){
								//logger.debug("line not indexed : " + lineTextTmp);
								lineTextTmp = lineTextTmp.replaceAll(".", "*");
							}
							else{
								//logger.debug("lineText before blackened : " + lineTextTmp);
								lineTextTmp = blackenString(currEntry, lineTextTmp);
								//logger.debug("lineText after blackened : " + lineTextTmp);

							}
						}
					}

					rtl = textIsRTL(lineTextTmp.trim());
					addString(boundRect, baseLineMeanY, lineTextTmp, cb, cutoffLeft, cutoffTop, bf, rotation, rtl);
					/*
					 * highlight all tags of this text line if property is set
					 */
//					if (highlightTags){
//						highlightTagsForShape(l);
//						
//					}

				} else {
					//logger.info("No text content in line: " + l.getId());
				}
				
				
				if (highlightTags || highlightArticles){
					
					if ((l.getUnicodeText().isEmpty() || useWordLevel) && !l.getWord().isEmpty()){
					
						List<WordType> words = l.getWord();
						for(WordType wt : words){
							TrpWordType w = (TrpWordType)wt;
							if (highlightTags){
								highlightTagsForShape(w, rtl, cache);
							}
							if (highlightArticles){
								highlightArticlesForShape(w, rtl, cache);
							}
						}
					}
					else{
						if (highlightTags){
							highlightTagsForShape(l, rtl, cache);
						}
						if (highlightArticles){
							highlightArticlesForShape(l, rtl, cache);
						}
						
					}
				}
			}		
		}	
	}

	
	private boolean textIsRTL(String text) {
		if (!text.isEmpty()){
			return (Character.getDirectionality(text.charAt(0)) == Character.DIRECTIONALITY_RIGHT_TO_LEFT
				    || Character.getDirectionality(text.charAt(0)) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
				    || Character.getDirectionality(text.charAt(0)) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
				    || Character.getDirectionality(text.charAt(0)) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
				    );
		}
		return false;
	}
	
	private void highlightArticlesForShape(ITrpShapeType shape, boolean rtl, ExportCache cache) throws IOException {
		
		Set<Entry<CustomTag, String>> entrySet = ExportUtils.getAllTagsForShapeElement(shape).entrySet();
		
		boolean falling = true;
		
		BaselineType baseline = null;
		if (shape instanceof TrpTextLineType){
			TrpTextLineType l = (TrpTextLineType) shape;
			baseline = l.getBaseline();
		}
		else if (shape instanceof TrpWordType){
			TrpWordType w = (TrpWordType) shape;
			TrpTextLineType l = (TrpTextLineType) w.getParentShape();
			baseline = l.getBaseline();
		}
		
		
		try {
			List<Point> ptsList = null;
			if (baseline != null){
				ptsList = PointStrUtils.parsePoints(baseline.getPoints());
			}
			if (ptsList != null){
				int size = ptsList.size();
				//logger.debug("l.getBaseline().getPoints() " + l.getBaseline().getPoints());
				if (size >= 2 && ptsList.get(0).y < ptsList.get(size-1).y){
					//logger.debug("falling is false ");
					falling = false;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (Map.Entry<CustomTag, String> currEntry : entrySet){
			
			//for articles
			if (currEntry.getKey().getTagName().equals("structure")){
				String color = CustomTagFactory.getNewTagColor();
				StructureTag st = (StructureTag) currEntry.getKey();
				if (st.getType().equals("article")){
					logger.debug("article id " + st.getId());
					String id = st.getId();
					String [] splits = id.split("a");
//					for (int i = 0; i<splits.length; i++){
//						logger.debug("split " + splits[i]);
//					}
					if (splits.length > 1){
						int articleId = Integer.parseInt(splits[1]);
						//logger.debug("articleId id " + articleId);
						int choice = articleId % 4;
						switch (choice){	
							case 0 : color = "#FF0000"; break;//red
							case 1 : color = "#9932CC"; break;//dark orchid
							case 2 : color = "#228B22"; break;//forest green
							case 3 : color = "#B8860B"; break;//dark golden rod
							case 4 : color = "#00CED1"; break;//dark turquoise
						}
					}
					
				}
				//article_a1
				if(baseline != null){
					//use lowest point in baseline and move up one half of the distance to the topmost point
					//java.awt.Rectangle baseLineRect = PageXmlUtils.buildPolygon(baseline.getPoints()).getBounds();
					java.awt.Rectangle baseLineRect = ((TrpBaselineType)baseline).getBoundingBox();
					calculateTagLines(baseLineRect, shape, currEntry.getKey().getContainedText(), 0, 0, color, 0, falling, rtl);
				}
			}
		}
	}
		

	private void highlightTagsForShape(ITrpShapeType shape, boolean rtl, ExportCache cache) throws IOException {
		int tagId = 0;
		int k = 1;
		Set<Entry<CustomTag, String>> entrySet = ExportUtils.getAllTagsForShapeElement(shape).entrySet();
		
		//Set<String> wantedTags = ExportUtils.getOnlyWantedTagnames(CustomTagFactory.getRegisteredTagNames());
		Set<String> wantedTags = cache.getOnlySelectedTagnames(CustomTagFactory.getRegisteredTagNames());
		
		//logger.debug("wanted tags in TRPPDFDOC " + wantedTags.size());

		int [] prevLength = new int[entrySet.size()];
		int [] prevOffset = new int[entrySet.size()];
		boolean falling = true;
		
		BaselineType baseline = null;
		if (shape instanceof TrpTextLineType){
			TrpTextLineType l = (TrpTextLineType) shape;
			baseline = l.getBaseline();
		}
		else if (shape instanceof TrpWordType){
			TrpWordType w = (TrpWordType) shape;
			TrpTextLineType l = (TrpTextLineType) w.getParentShape();
			baseline = l.getBaseline();
		}
		
		
		try {
			List<Point> ptsList = null;
			if (baseline != null){
				ptsList = PointStrUtils.parsePoints(baseline.getPoints());
			}
			if (ptsList != null){
				int size = ptsList.size();
				//logger.debug("l.getBaseline().getPoints() " + l.getBaseline().getPoints());
				if (size >= 2 && ptsList.get(0).y < ptsList.get(size-1).y){
					//logger.debug("falling is false ");
					falling = false;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Map.Entry<CustomTag, String> currEntry : entrySet){
					
			if (wantedTags.contains(currEntry.getKey().getTagName())){
				
				String color = CustomTagFactory.getTagColor(currEntry.getKey().getTagName());
				
				int currLength = currEntry.getKey().getLength();
				int currOffset = currEntry.getKey().getOffset();
				
				/**
				 * if the current tag overlaps one of the previous tags
				 * -> increase the distance of the line under the textline
				 */
//				if (isOverlaped(prevOffset, prevLength, currOffset, currLength)){
//					k++;
//				}
//				else{
//					k=1;
//				}
				
				k = getAmountOfOverlaps(prevOffset, prevLength, currOffset, currLength);
				
//				logger.debug("current tag name "+ currEntry.getKey().getTagName() + " k is " + k);
//				logger.debug("current tag text "+ currEntry.getKey().getContainedText());
				
				prevOffset[tagId] = currOffset;
				prevLength[tagId] = currLength;
				tagId++;
				
				float yShift = (lineMeanHeight/6) * k;
				/*
				 * remember where to draw line with help of a list
				 */
				if(baseline != null){
					//use lowest point in baseline and move up one half of the distance to the topmost point
					//java.awt.Rectangle baseLineRect = PageXmlUtils.buildPolygon(baseline.getPoints()).getBounds();
					java.awt.Rectangle baseLineRect = ((TrpBaselineType)baseline).getBoundingBox();
					calculateTagLines(baseLineRect, shape, currEntry.getKey().getContainedText(), currOffset, currLength, color, yShift, falling, rtl);
				}
			}
			
		}
		
	}

	private String blackenString(Entry<CustomTag, String> currEntry, String lineText) {
		int beginIndex = currEntry.getKey().getOffset();
		int endIndex = beginIndex + currEntry.getKey().getLength();
		
//		logger.debug("lineText before : " + lineText);
//		logger.debug("lineText length : " + lineText.length());
//		logger.debug("begin : " + beginIndex);
//		logger.debug("end : " + endIndex);
		
		String beginString = "";
		if (beginIndex > 0)
			beginString = lineText.substring(0, beginIndex);
		String tagString = lineText.substring(beginIndex, endIndex);
		tagString = tagString.replaceAll(".", "*");
		String postString = lineText.substring(endIndex);
		
		return beginString.concat(tagString).concat(postString);
		
	}

	private void calculateTagLines(java.awt.Rectangle baseLineRect, ITrpShapeType shape, String tagText, int offset, int length, String color, float yShift, boolean falling, boolean rtl) {
		
		String lineText = shape.getUnicodeText();
		
		java.awt.Rectangle shapeRect = null;
		if (shape instanceof TrpWordType){
			shapeRect = ((TrpWordType) shape).getBoundingBox();
		}
		else{
			shapeRect = baseLineRect;
		}
		
		float shapeMinX = (float) shapeRect.getMinX();
		float shapeMaxX = (float) shapeRect.getMaxX();
		
		float minX = (float) baseLineRect.getMinX();
		float maxX = (float) baseLineRect.getMaxX();
		
		float minY = (float) baseLineRect.getMinY();
		float maxY = (float) baseLineRect.getMaxY();
		
		float a = maxY - minY;
		float b = maxX - minX;
		
		float angleAlpha = (float) Math.atan(a/b);
		
//		logger.debug("line Text " + lineText);
//		logger.debug("tag text " + tagText);
//		logger.debug("angle alpha " + angleAlpha);
//		
//		logger.debug("offset " + offset);
//		logger.debug("lineText.length() " + lineText.length());
//		logger.debug("offset+length " + offset+length);
				
		//relation of tagStart to entire text length
		float ratioOfTagStart = 0;
		if (offset != 0){
			ratioOfTagStart = (float) offset / (float) lineText.length();
		}
		// length == 0 for nonindexed tags like reading order or structure
		float ratioOfTagEnd = length != 0 ? (float) (offset+length) / (float) lineText.length() : 1;
		
		float tagStartX;
		float tagEndX;
		if (!rtl){
			tagStartX = shapeMinX + (ratioOfTagStart * baseLineRect.width);
			tagEndX = shapeMinX + (ratioOfTagEnd * shapeRect.width);
		}
		else{
			tagStartX = shapeMaxX - (ratioOfTagStart * baseLineRect.width);
			tagEndX = shapeMaxX - (ratioOfTagEnd * shapeRect.width);
		}
		
		float tagStartHeight = 0;
		if (tagStartX != shapeMinX && !rtl){
			tagStartHeight = (float) (Math.tan(angleAlpha) * (tagStartX-shapeMinX)); 
		}
		else if (tagStartX != shapeMaxX && rtl){
			tagStartHeight = (float) (Math.tan(angleAlpha) * (tagStartX-shapeMinX)); 
		}

		float tagEndHeight = (float) (Math.tan(angleAlpha) * (tagEndX-shapeMinX));
		
		float tagStartY;
		float tagEndY;
		
		if (falling){
//			logger.debug("tagStartHeight > tagEndHeight; tagStartY = maxY - tagStartHeight;" + (maxY - tagStartHeight));
//			logger.debug("tagStartHeight > tagEndHeight; tagEndY = maxY - tagEndHeight;" + (maxY - tagEndHeight));
			tagStartY = maxY - tagStartHeight;
			tagEndY = maxY - tagEndHeight;
		}
		else{
			tagStartY = maxY - tagEndHeight;
			tagEndY = maxY - tagStartHeight;
		}

//		logger.debug("tag startX " + tagStartX);
//		logger.debug("tag endX " + tagEndX);
//		
//		logger.debug("tag startY " + tagStartY);
//		logger.debug("tag endY " + tagEndY);
		
		Line2D line = new Line2D.Double(tagStartX, tagStartY + yShift, tagEndX, tagEndY + yShift);
		java.util.Map.Entry<Line2D,String> pair= new java.util.AbstractMap.SimpleEntry<>(line,color);
		lineAndColorList.add(pair);
		
	}

	/*
	 * checks if there is at least one text region on the left of the actual one
	 * But: if text region is completely contained in the other it should not have an effect
	 */
	private boolean hasSmallerColumn(List<TrpRegionType> regions, TextRegionType regionToCompare) throws DocumentException, IOException {
						
		float minX = 0;
		float minY = 0;
		float maxX = 0;
		float maxY = 0;
		float meanX = 0;
		float meanY = 0;
		
		//java.awt.Rectangle compareBlock = PageXmlUtils.buildPolygon(regionToCompare.getCoords().getPoints()).getBounds();
		java.awt.Rectangle compareBlock = regionToCompare.getBoundingBox();
		float compareMinX = (float) compareBlock.getMinX();
		float compareMinY = (float) compareBlock.getMinY();
		float compareMaxX = (float) compareBlock.getMaxX();
		float compareMaxY = (float) compareBlock.getMaxY();
		
		float compareMeanX = compareMinX+(compareMaxX - compareMinX)/2;
		float compareMeanY = compareMinY+(compareMaxY - compareMinY)/2;
		
		boolean foundSmallerColumn = false;
		smallerRegionMaxX = 0;
		
//		logger.debug("nr of regions " + regions.size());
//		logger.debug("regionToCompare id " + regionToCompare.getId());
		
		if (regions.size() == 1){
			return false;
		}
		else{

			for(RegionType r : regions){
				//TODO add paths for tables etc.
				if(r instanceof TextRegionType && r.getId() != regionToCompare.getId()){
					TextRegionType tr = (TextRegionType)r;
					
					//empty region can be ignored
					if (tr.getTextLine().isEmpty())
						continue;
					else{
						//region with empty lines can also be ignored
						boolean textFound = false;
						for (TextLineType tlt : tr.getTextLine()){
							TrpTextLineType l = (TrpTextLineType)tlt;
							textFound = !l.getUnicodeText().isEmpty();
							if (textFound){
								break;
							}
						}
						//no text in region -> go to next region
						if (!textFound){
							continue;
						}
					}
					//logger.debug("tr id " + tr.getId());

					//compute average text region start
					//java.awt.Rectangle block = PageXmlUtils.buildPolygon(tr.getCoords().getPoints()).getBounds();
					java.awt.Rectangle block = tr.getBoundingBox();
					minX = (float) block.getMinX();
					maxX = (float) block.getMaxX();
					minY = (float) block.getMinY();
					maxY = (float) block.getMaxY();
					
					//meanX = minX+(maxX - minX)/2;
					meanY = minY+(maxY - minY)/2;
					
					if ( ( (meanY > compareMinY && meanY < compareMaxY) ||
							(compareMeanY > minY && compareMeanY < maxY) )
							&& (maxX < compareMeanX) ){
						//to find the biggest maxX if there are several smaller columns
						if (maxX > smallerRegionMaxX){
							smallerRegionMaxX = maxX;
						}
						foundSmallerColumn = true;
					}						
	
				}
			}
		}
		return foundSmallerColumn;

	}

	public void addTags(TrpDoc doc, Set<Integer> pageIndices, boolean useWordLevel2, ExportCache cache) throws DocumentException, IOException {
		PdfContentByte cb = writer.getDirectContentUnder();
		document.newPage();
				
		int l = 0;
		float posY;
		//BaseFont bf = BaseFont.createFont(BaseFont.TIMES_ROMAN, "UTF-8", BaseFont.NOT_EMBEDDED, true, null, null);
		
		Set<String> wantedTags = cache.getOnlySelectedTagnames(CustomTagFactory.getRegisteredTagNames());
		
		//logger.debug("selectedTags Size " + selectedTags.size());
		for (String currTagname : wantedTags){
			double lineHeight = 12/scaleFactorY;
			double lineGap = 4/scaleFactorY;
			//logger.debug("currTagname " + currTagname);
			//get all custom tags with currTagname and text
			HashMap<CustomTag, String> allTagsOfThisTagname = cache.getTags(currTagname);
			//logger.debug("all Tags Of This Tagname " + currTagname);
			if(allTagsOfThisTagname.size()>0){
				
				posY = (float) (twelfthPoints[1][1]+(lineHeight+lineGap)*l);
				if (posY > twelfthPoints[10][1]){
					document.newPage();
					posY = twelfthPoints[1][1];
					l = 0;
				}
					
				l++;
				String color = CustomTagFactory.getTagColor(currTagname);
				

				addUniformTagList(lineHeight, twelfthPoints[1][0], posY, "", currTagname + " Tags:", "", cb, 0, 0, mainExportBaseFont, twelfthPoints[1][0], false, color, 0, false);
				//addUniformStringTest(lineMeanHeight, twelfthPoints[1][0], posY, currTagname + " Tags:", cb, 0, 0, bfArial, twelfthPoints[1][0], false, color, 0);
				
				Collection<String> valueSet = allTagsOfThisTagname.values();
				Collection<CustomTag> keySet = allTagsOfThisTagname.keySet();
				
				HashSet<String> uniqueValues = new HashSet<String>();
				
				Iterator<CustomTag> it = keySet.iterator();

				while (it.hasNext()){
					
					CustomTag currEntry = it.next();
					
					String currValue = allTagsOfThisTagname.get(currEntry);
					
					//case for gap tag
					if(currValue == null){
						currValue="";
					}
					String expansion = "";
					
//					logger.debug("curr tag entry " + currEntry);
//					logger.debug("curr tag value " + currValue);
										
					//handles continued tags over several lines
					while (currEntry.isContinued() && it.hasNext()){
						currEntry = it.next();
						if (currEntry.isContinued()){
							String continued = allTagsOfThisTagname.get(currEntry);
							currValue = currValue.concat(continued);
							
							//soft hyphen
							currValue = currValue.replaceAll("\u00AD", "");
							//minus
							currValue = currValue.replaceAll("\u002D", "");
							//not sign
							currValue = currValue.replaceAll("\u00AC", "");
							
							//char c = 0xFFFA; String.valueOf(c).replaceAll("\\p{C}", "?");
							
						}
					}
					
					boolean rtl = false;
					
					
					if (!currValue.isEmpty() && textIsRTL(currValue)){
						rtl = true;
						//logger.debug("rtl tag found " + currValue);
						currValue = reverseString(currValue);
					}
					
					String searchText = currValue;
					
					if (currTagname.equals(CommentTag.TAG_NAME)){
						
						CommentTag ct = (CommentTag) currEntry;
						if (ct.getComment() != ""){
							if (!rtl)
								expansion = ": " + ct.getComment();
							else
								expansion = ct.getComment() + " :";
						}
							
						//currValue = currValue.concat(": " + ct.getComment());
						//logger.debug("comment " + currValue);
					}
					
					else if (currTagname.equals(AbbrevTag.TAG_NAME)){
						
						AbbrevTag at = (AbbrevTag) currEntry;
						if (at.getExpansion() != "")
							if (!rtl)
								expansion = ": " + at.getExpansion();
							else
								expansion = at.getExpansion() + " :";
							
					}
					
					else if (currTagname.equals(GapTag.TAG_NAME)){
						
						GapTag at = (GapTag) currEntry;
						currValue = currEntry.getTextOfShape();
						searchText = currValue;
						int offset = Math.max(at.getOffset(), currValue.length()-1);
						String sub1 = currValue.substring(0, offset);
						String sub2 = currValue.substring(offset);
						String exp = (String) at.getAttributeValue("supplied");
						if ( exp != null && exp != ""){
							currValue = sub1.concat("["+exp+"]").concat(sub2);
							//expansion = "[" + (String) at.getAttributeValue("supplied") + "]";
						}
						//no supplied attribute - gap must not be in the tag list
						else{
							continue;
						}
							
					}
					
					else if (currTagname.equals(SuppliedTag.TAG_NAME)){
						
							
					}
										
					//make sure that similar tags are only exported once
					if (!uniqueValues.contains(currValue)){
						uniqueValues.add(currValue);
						
						posY = (float) (twelfthPoints[1][1]+(lineHeight+lineGap)*l);	
						if (posY > twelfthPoints[11][1]){
							document.newPage();
							posY = twelfthPoints[1][1];
							l = 1;
						}

						addUniformTagList(lineHeight, twelfthPoints[1][0], posY, searchText, currValue, expansion, cb, 0, 0, mainExportBaseFont, twelfthPoints[1][0], true, null, 0, rtl);
						//logger.debug("tag value is " + currValue);
						l++;
					}

				}
				
				l++;
				
				
			}
			
		}
		
	}
	
	private static String reverseString(String text) {
		StringBuilder sb = new StringBuilder(text);
		sb.reverse();
		return (sb.toString());
	}

	public void addTitlePage(TrpDoc doc) {
		document.newPage();
		PdfContentByte cb = writer.getDirectContentUnder();
		
		float lineHeight = twelfthPoints[1][0]/3;
		float posY = twelfthPoints[1][1];
		
		addTitleString("Title Page", posY, 0, (float) (lineHeight*1.5), cb, boldItalicBaseFont);
		posY += lineHeight*2;
		
		TrpDocMetadata docMd = doc.getMd();
		
		if (writeDocMd("Title: ", docMd.getTitle(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.5;
		}
		
		if (writeDocMd("Author: ", docMd.getAuthor(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.5;
		}

		lineHeight = twelfthPoints[1][0]/6;
		
		if (writeDocMd("Authority: ", docMd.getAuthority(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.5;
		}
		
		if (writeDocMd("External ID: ", docMd.getExternalId(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.5;
		}
		
		if (writeDocMd("Hierarchy: ", docMd.getHierarchy(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.5;
		}
		
		if (writeDocMd("Backlink: ", docMd.getBacklink(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.5;
		}
		
		if (writeDocMd("Description: ", docMd.getDesc(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.2;
		}
		
		if (writeDocMd("Genre: ", docMd.getGenre(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.2;
		}
		
		if (writeDocMd("Writer: ", docMd.getWriter(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.2;
		}
		
		if (docMd.getScriptType() != null){
			if (writeDocMd("Scripttype: ", docMd.getScriptType().toString(), posY, 0, lineHeight, cb, italicBaseFont)){
				posY += lineHeight*1.2;
			}
		}
		
		if (writeDocMd("Language: ", docMd.getLanguage(), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.2;
		}
		
		if (writeDocMd("Number of Pages in whole Document: ", String.valueOf(docMd.getNrOfPages()), posY, 0, lineHeight, cb, italicBaseFont)){
			posY += lineHeight*1.2;
		}
		
		if (docMd.getCreatedFromDate() != null){
			if (writeDocMd("Created From: ", docMd.getCreatedFromDate().toString(), posY, 0, lineHeight, cb, italicBaseFont)){
				posY += lineHeight*1.2;
			}
		}
		
		if (docMd.getCreatedToDate() != null){
			if (writeDocMd("Created To: ", docMd.getCreatedToDate().toString(), posY, 0, lineHeight, cb, italicBaseFont)){
				posY += lineHeight*1.5;
			}
		}
		//--- Export settings section
		lineHeight = twelfthPoints[1][0]/3;
		addTitleString("Export Settings: ", posY, twelfthPoints[1][0], lineHeight, cb, boldItalicBaseFont);
		
		String imageSetting = (imgOnly ? "Images without text layer" : "Images with text layer");
		String extraTextSetting = (extraTextPage ? "Extra pages for transcribed text are added" : "");
		String blackeningSetting = (doBlackening ? "Sensible data is invisible" : "Sensible data is shown if existent");
		String tagSetting = (highlightTags ? "Tags are highlighted (colored lines) and added at the end" : "No tags shown in export");
		
		lineHeight = twelfthPoints[1][0]/6;
		posY += lineHeight*1.5;
		addTitleString(imageSetting + " / " + extraTextSetting + " / " + blackeningSetting + " / " + tagSetting, posY, twelfthPoints[1][0], lineHeight, cb, boldItalicBaseFont);
		//--- Export settings section end
		
		//--- Editorial declaration section		
		lineHeight = twelfthPoints[1][0]/3;
		posY += lineHeight*1.5;
		
		List<EdFeature> efl = doc.getEdDeclList();
		
		if (efl.size() >= 0){
			addTitleString("Editorial Declaration: ", posY, twelfthPoints[1][0], lineHeight, cb, boldItalicBaseFont);
			posY += lineHeight*1.5;
			
			lineHeight = twelfthPoints[1][0]/6;
		}

		for (EdFeature edfeat : efl){
			addTitleString(edfeat.getTitle() + ": " + edfeat.getDescription() +"\n" + edfeat.getSelectedOption().toString(), posY, twelfthPoints[1][0], lineHeight, cb, mainExportBaseFont);
			//posY += lineHeight;
			//addTitleString(edfeat.getSelectedOption().toString(), posY, twelfthPoints[1][0], lineHeight, cb, bfArial);
			posY += lineHeight*1.5;
		}
		//--- Editorial declaration section	end	
		
	}

	private boolean writeDocMd(String mdName, String mdValue, float posYdirection, int horizontalPlacement,
			float lineHeight, PdfContentByte cb, BaseFont bfArialItalic) {
		if (mdValue != null && !mdValue.equals("")){
			
			if (posYdirection > (twelfthPoints[11][1])){
				posYdirection = twelfthPoints[1][1];
			}
			addTitleString(mdName + mdValue, posYdirection, horizontalPlacement, lineHeight, cb, bfArialItalic);
			return true;
		}
		return false;
		
	}



	

}
