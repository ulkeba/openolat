/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.search.service.document.file;

import java.io.IOException;
import java.util.Date;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.olat.core.CoreSpringFactory;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.vfs.LocalImpl;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.search.QueryException;
import org.olat.search.SearchModule;
import org.olat.search.SearchService;
import org.olat.search.ServiceNotAvailableException;
import org.olat.search.model.AbstractOlatDocument;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.SearchServiceImpl;

/**
 * Lucene document mapper.
 * <p>Supported file-types : 
 * <lu>
 * <li>pdf => PDF document</li>
 * <li>xls => Excel document</li>
 * <li>doc => Word document</li>
 * <li>ppt => Power-point document</li>
 * <li>odt, ods, odp, odf, odg => OpenDocument document</li>
 * <li>htm, html, xhtml, xml => HTML document</li>
 * <li>txt, tex, README, csv => Text document</li>
 * @author Christian Guretzki
 */
public class FileDocumentFactory {
	
	private static OLog log = Tracing.createLoggerFor(FileDocumentFactory.class);

	private final static String PDF_SUFFIX = "pdf";
	private final static String EXCEL_SUFFIX = "xls";
	private final static String WORD_SUFFIX = "doc";
	private final static String POWERPOINT_SUFFIX = "ppt";
	private final static String EXCEL_X_SUFFIX = "xlsx";
	private final static String WORD_X_SUFFIX = "docx";
	private final static String POWERPOINT_X_SUFFIX = "pptx";
	private final static String OD_TEXT_SUFFIX = "odt";
	private final static String OD_SPREADSHEET_SUFFIX = "ods";
	private final static String OD_PRESENTATION_SUFFIX = "odp";
	private final static String OD_FORMULA_SUFFIX = "odf";
	private final static String OD_GRAPHIC_SUFFIX = "odg";

	private final static String HTML_SUFFIX = "htm html xhtml";
	private final static String XML_SUFFIX = "xml";
	private final static String TEXT_SUFFIX = "txt tex readme csv";
  
	//as a special parser;
	private static final String IMS_MANIFEST_FILE = "imsmanifest.xml";
  
	private int excludedFileSizeCount = 0;

  
	private static SearchModule searchModule;
  
	/**
	 * [used by spring]
	 * @param searchModule
	 */
	public FileDocumentFactory(SearchModule module) {
		searchModule = module;
	}
	
	public static int getMaxFileSize() {
		return (int)searchModule.getMaxFileSize();
	}
	
	public Document createDocument(SearchResourceContext leafResourceContext, VFSLeaf leaf)
	throws IOException, DocumentAccessException {
		try {
			Document doc = null;
			try {
				String resourceUrl = leafResourceContext.getResourceUrl();
				SearchService searchService = CoreSpringFactory.getImpl(SearchServiceImpl.class);
				
				Document indexedDoc = searchService.doSearch(resourceUrl);
				if(indexedDoc != null) {
					String timestamp = indexedDoc.get(AbstractOlatDocument.TIME_STAMP_NAME);
					if(timestamp != null) {
						Date lastMod = DateTools.stringToDate(timestamp);
						Date lastMod2 = new Date(leaf.getLastModified());
						if(lastMod2.compareTo(lastMod) < 0) {
							return indexedDoc;
						}
					}
				}
			} catch (ServiceNotAvailableException | ParseException | QueryException | java.text.ParseException e) {
				log.error("", e);
			}
			
			String fileName = leaf.getName();
			String suffix = FileTypeDetector.getSuffix(leaf);
			if (log.isDebug()) log.debug("suffix=" + suffix);
			
			if (PDF_SUFFIX.indexOf(suffix) >= 0) {
				if(searchModule.getPdfFileEnabled()) {
					doc = PdfDocument.createDocument(leafResourceContext, leaf);
				}
			} else if (HTML_SUFFIX.indexOf(suffix) >= 0) {
				doc = HtmlDocument.createDocument(leafResourceContext, leaf);
			} else if (XML_SUFFIX.indexOf(suffix) >= 0) {
				if(IMS_MANIFEST_FILE.equals(fileName)) {
					doc = IMSMetadataDocument.createDocument(leafResourceContext, leaf);
				} else {
					doc = XmlDocument.createDocument(leafResourceContext, leaf);
				}
			} else if (TEXT_SUFFIX.indexOf(suffix) >= 0) {
				doc = TextDocument.createDocument(leafResourceContext, leaf);
			//microsoft openxml
			} else if (suffix.indexOf(WORD_X_SUFFIX) >= 0) {	
				doc = WordOOXMLDocument.createDocument(leafResourceContext, leaf);
			} else if (suffix.indexOf(EXCEL_X_SUFFIX) >= 0) {
				if (searchModule.getExcelFileEnabled()) {
					doc = ExcelOOXMLDocument.createDocument(leafResourceContext, leaf);
				}
			} else if (suffix.indexOf(POWERPOINT_X_SUFFIX) >= 0) {
				if(searchModule.getPptFileEnabled()) {
					doc = PowerPointOOXMLDocument.createDocument(leafResourceContext, leaf);
				}
			//microsoft
			} else if (WORD_SUFFIX.indexOf(suffix) >= 0) {
				doc = WordDocument.createDocument(leafResourceContext, leaf);
			} else if (POWERPOINT_SUFFIX.indexOf(suffix) >= 0) {
				if(searchModule.getPptFileEnabled()) {
					doc = PowerPointDocument.createDocument(leafResourceContext, leaf);
				}
			} else if (EXCEL_SUFFIX.indexOf(suffix) >= 0) {
				if (searchModule.getExcelFileEnabled()) {
					doc = ExcelDocument.createDocument(leafResourceContext, leaf);
				}
			//open document
			} else if (OD_TEXT_SUFFIX.indexOf(suffix) >= 0 || OD_SPREADSHEET_SUFFIX.indexOf(suffix) >= 0
					|| OD_PRESENTATION_SUFFIX.indexOf(suffix) >= 0 || OD_FORMULA_SUFFIX.indexOf(suffix) >= 0
					|| OD_GRAPHIC_SUFFIX.indexOf(suffix) >= 0) {
				doc = OpenDocument.createDocument(leafResourceContext, leaf);
			}
			
			if(doc == null) {
				doc = createUnkownDocument(leafResourceContext, leaf);
			}
			return doc;
		} catch(DocumentNotImplementedException e) {
			log.warn("Cannot index document (no indexer for it):" + leaf, e);
			return createUnkownDocument(leafResourceContext, leaf);
		} catch (DocumentException e) {
			log.warn("Cannot index document:" + leaf, e);
			return createUnkownDocument(leafResourceContext, leaf);
		}
	}
	
	private Document createUnkownDocument(SearchResourceContext leafResourceContext, VFSLeaf leaf) {
		try {
			return UnkownDocument.createDocument(leafResourceContext, leaf);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Check if certain file is supported.
	 * @param fileName
	 * @return
	 */
	public boolean isFileSupported(VFSLeaf leaf) {
		String fileName = leaf.getName();
		if (fileName != null && fileName.startsWith(".")) {
			//don't index all mac os x hidden files
			return false;
		}
		
		String suffix;
		try {
			suffix = FileTypeDetector.getSuffix(leaf);
		} catch (DocumentNotImplementedException e) {
			return false;
		}
		
		// 1. Check if file is not on fileBlackList
		if (searchModule.getFileBlackList().contains(fileName)) {
			// File name is on blacklist 
			return false;
		}
		
		if(leaf instanceof LocalImpl) {
			String path = ((LocalImpl)leaf).getBasefile().getAbsolutePath();
			if (searchModule.getFileBlackList().contains(path)) {
				return false;
			}
		}
		
		// 2. Check for certain file-type the file size
		if (searchModule.getFileSizeSuffixes().contains(suffix)) {
			long maxFileSize = searchModule.getMaxFileSize();
			if ( (maxFileSize != 0) && (leaf.getSize() > maxFileSize) ) {
				log.info("File too big, exlude from search index. filename=" + fileName);
				excludedFileSizeCount++;
				return false;
			}
		}
		/* 3. Check if suffix is supported
		if (supportedSuffixes.indexOf(suffix) >= 0) {
			return true;
		}*/
		//index all files (index metadatas)
		return true;
	}
	
	public int getExcludedFileSizeCount( ) {
	  return excludedFileSizeCount;
	}

	public void resetExcludedFileSizeCount( ) {
	  excludedFileSizeCount = 0;
	}

}
