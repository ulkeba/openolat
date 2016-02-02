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

package org.olat.modules.webFeed.portfolio;

import javax.persistence.Entity;

import org.olat.fileresource.types.BlogFileResource;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.modules.webFeed.models.Feed;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

/**
 * 
 * Description:<br>
 * The LifeBlogArtefact integrated a full featured blog in a map.
 * 
 * <P>
 * Initial Date:  8 nov. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
@Entity
public class LiveBlogArtefact extends AbstractArtefact {

	private static final long serialVersionUID = -5053371748195771395L;
	public static final String TYPE = "liveblog";

	@Override
	public String getResourceableTypeName() {
		return TYPE;
	}

	@Override
	public String getIcon() {
		return "o_ep_icon_liveblog";
	}
	
	public Feed getFeedLight() {
		String businessPath = getBusinessPath();
		Long resid = Long.parseLong(businessPath.substring(10, businessPath.length() - 1));
		OLATResource ores = OLATResourceManager.getInstance().findResourceable(resid, BlogFileResource.TYPE_NAME);
		return FeedManager.getInstance().getFeed(ores);
	}
	
}
