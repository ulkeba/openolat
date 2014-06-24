/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.repository.ui.author;

import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiTableDataSourceModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;

/**
 * 
 * Initial date: 28.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
class AuthoringEntryDataModel extends DefaultFlexiTableDataSourceModel<AuthoringEntryRow> {
	
	public AuthoringEntryDataModel(AuthoringEntryDataSource source, FlexiTableColumnModel columnModel) {
		super(source, columnModel);
	}

	@Override
	public DefaultFlexiTableDataSourceModel<AuthoringEntryRow> createCopyWithEmptyList() {
		return new AuthoringEntryDataModel(getSourceDelegate(), getTableColumnModel());
	}

	@Override
	public AuthoringEntryDataSource getSourceDelegate() {
		return (AuthoringEntryDataSource)super.getSourceDelegate();
	}

	@Override
	public void clear() {
		super.clear();
		getSourceDelegate().resetCount();
	}

	@Override
	public Object getValueAt(int row, int col) {
		AuthoringEntryRow item = getObject(row);
		if(item == null) {
			return null;//don't break here
		}
		
		switch(Cols.values()[col]) {
			case key: return item.getKey();
			case ac: return item;
			case type: return item;
			case displayName: return item.getDisplayname();
			case lifecycleLabel: return item.getLifecycleLabel();
			case lifecycleSoftkey: return item.getLifecycleSoftKey();
			case lifecycleStart: return item.getLifecycleStart();
			case lifecycleEnd: return item.getLifecycleEnd();
			case externalId: return item.getExternalId();
			case externalRef: return item.getExternalRef();
			case author: return item.getAuthor();
			case authors: return item.getAuthors();
			case access: return item;
			case creationDate: return item.getCreationDate();
			case lastUsage: return item.getLastUsage();
			case mark: return item.getMarkLink();
		}
		return null;
	}
	
	public enum Cols {
		key("table.header.key"),
		ac("table.header.ac"),
		type("table.header.typeimg"),
		lifecycleLabel("table.header.lifecycle.label"),
		lifecycleSoftkey("table.header.lifecycle.softkey"),
		lifecycleStart("table.header.lifecycle.start"),
		lifecycleEnd("table.header.lifecycle.end"),
		externalId("table.header.externalid"),
		externalRef("table.header.externalref"),
		displayName("cif.displayname"),
		author("table.header.author"),
		authors("table.header.author"),
		access("table.header.access"),
		creationDate("table.header.date"),
		lastUsage("table.header.lastusage"),
		mark("table.header.mark");
		
		private final String i18nKey;
		
		private Cols(String i18nKey) {
			this.i18nKey = i18nKey;
		}
		
		public String i18nKey() {
			return i18nKey;
		}
	}
}