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

import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.tagged.MetaTagged;
import org.olat.core.util.StringHelper;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.search.model.OlatDocument;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.SimpleDublinCoreMetadataFieldsProvider;

/**
 * Lucene document mapper.
 * @author Christian Guretzki
 */
public abstract class FileDocument extends OlatDocument {

	private static final long serialVersionUID = -8977326187286155071L;
	// Must correspond with LocalString_xx.properties
	// Do not use '_' because we want to seach for certain documenttype and lucene haev problems with '_' 
	public final static String TYPE = "type.file";
	
	protected void init(SearchResourceContext leafResourceContext, VFSLeaf leaf) throws IOException,DocumentException,DocumentAccessException {
		// Load metadata for this file
		MetaInfo meta = null;
		if (leaf instanceof MetaTagged) {
			meta = ((MetaTagged)leaf).getMetaInfo();
		}
		
		// Set all know attributes
		setResourceUrl(leafResourceContext.getResourceUrl());
		setLastChange(new Date(leaf.getLastModified()));
		// Check if there are documents attributes set in resource context
		if (leafResourceContext.getDocumentType() != null && !leafResourceContext.getDocumentType().equals("")) {
			// document-type in context is set => get from there
			setDocumentType(leafResourceContext.getDocumentType());
		} else {
  		setDocumentType(TYPE);
		}

		FileContent content = readContent(leaf);
		String metaTitle;
		if(meta != null && StringHelper.containsNonWhitespace(meta.getTitle())) {
			metaTitle = meta.getTitle();
		} else if(content != null && StringHelper.containsNonWhitespace(content.getTitle())) {
			metaTitle = content.getTitle();
		} else {
			metaTitle = null;
		} 
		
		StringBuilder title = new StringBuilder();
		if (StringHelper.containsNonWhitespace(leafResourceContext.getTitle())) {
			// Title in context is set => get from there and add filename
			title.append(leafResourceContext.getTitle()).append(", ");
		}
		if(metaTitle != null) {
			title.append(metaTitle).append(" ( ");
		}
		title.append(leaf.getName());
		if(metaTitle != null) title.append(" )");
		setTitle(title.toString());

		String metaDesc = (meta == null ? null : meta.getComment());
		if (leafResourceContext.getDescription() != null && !leafResourceContext.getDescription().equals("")) {
			// Title in context is set => get from there
			setDescription(leafResourceContext.getDescription() + (metaDesc == null ? "" : " " + metaDesc));
		} else {
      //		 no description this.setDescription();
			if (metaDesc != null) this.setDescription(metaDesc);
		}
		setParentContextType(leafResourceContext.getParentContextType());
		setParentContextName(leafResourceContext.getParentContextName());
		// Add the content itself
		setContent(content.getContent());
		
		
		// Add other metadata from meta info
		if (meta != null) {
			addMetadata(SimpleDublinCoreMetadataFieldsProvider.DC_DESCRIPTION, meta.getComment());			
			addMetadata(SimpleDublinCoreMetadataFieldsProvider.DC_LANGUAGE, meta.getLanguage());
			// Date is 2009 200902 or 20090228
			String[] pubDateArray = meta.getPublicationDate();
			if (pubDateArray != null) {
				String pubDate = null;
				if (pubDateArray.length == 1) pubDate = meta.getPublicationDate()[0];
				if (pubDateArray.length == 2) pubDate = meta.getPublicationDate()[0]+meta.getPublicationDate()[1];
				if (pubDateArray.length == 3) pubDate = meta.getPublicationDate()[0]+meta.getPublicationDate()[1]+meta.getPublicationDate()[2];
				addMetadata(SimpleDublinCoreMetadataFieldsProvider.DC_DATE, pubDate);							
			}
			addMetadata(SimpleDublinCoreMetadataFieldsProvider.DC_PUBLISHER, meta.getPublisher());	
			addMetadata(SimpleDublinCoreMetadataFieldsProvider.DC_SOURCE, meta.getSource());	
			addMetadata(SimpleDublinCoreMetadataFieldsProvider.DC_SOURCE, meta.getUrl());	
			// use creator and author as olat author 
			setAuthor((meta.getCreator() == null ? meta.getAuthor() : meta.getAuthor() + " " + meta.getCreator()));
			addMetadata(SimpleDublinCoreMetadataFieldsProvider.DC_CREATOR, meta.getCreator());				
		}
		// Add file type
		String mimeType = WebappHelper.getMimeType(leaf.getName());
		addMetadata(SimpleDublinCoreMetadataFieldsProvider.DC_FORMAT, mimeType);		
		
	}
	
	abstract protected FileContent readContent(VFSLeaf leaf) throws IOException, DocumentException, DocumentAccessException;

}
