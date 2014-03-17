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

package org.olat.repository;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.ContextEntryControllerCreator;
import org.olat.core.id.context.DefaultContextEntryControllerCreator;
import org.olat.repository.site.CatalogSite;
import org.olat.repository.site.RepositorySite;

/**
 * Description:<br>
 * 
 * <P>
 * Initial Date:  11 fev. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
public class CatalogContextEntryControllerCreator extends DefaultContextEntryControllerCreator {
	
	private final RepositoryModule repositoryModule;
	
	public CatalogContextEntryControllerCreator(RepositoryModule repositoryModule) {
		this.repositoryModule = repositoryModule;
	}

	@Override
	public ContextEntryControllerCreator clone() {
		return this;
	}
	
	@Override
	public Controller createController(ContextEntry ce, UserRequest ureq, WindowControl wControl) {
		return null;
	}

	@Override
	public String getSiteClassName(ContextEntry ce, UserRequest ureq) {
		if(repositoryModule.isCatalogSiteEnabled()) {
			return CatalogSite.class.getName();
		} else {
			return RepositorySite.class.getName();
		}
	}

	@Override
	public String getTabName(ContextEntry ce, UserRequest ureq) {
		return null;
	}

	@Override
	public boolean validateContextEntryAndShowError(ContextEntry ce, UserRequest ureq, WindowControl wControl) {
		return repositoryModule.isCatalogEnabled();
	}

}
