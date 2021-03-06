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

package org.olat.core.gui.components.form.flexible.impl.elements.richText.plugins.olatmovieviewer;

import java.util.HashMap;
import java.util.Map;

import org.olat.core.dispatcher.impl.StaticMediaDispatcher;
import org.olat.core.gui.components.form.flexible.impl.elements.richText.plugins.TinyMCECustomPlugin;

/**
 * Description:<br>
 * The OLAT movie viewer plugin provides a flash based movie player that can
 * playback streamed movies.
 * <p>
 * In addition to the normal movie playback feature the plugin implements some
 * pseudo security. For a hardcoded list of streaming servers the movie player
 * adds a token to the URL. Thus, when looking at the HTML sourcecode the movie
 * URL is not the real movie URL. This prevents user from copy/paste the movie
 * URL and making them available to not authorized persons. However, whe using a
 * proxy or sniffer software the real movie URL can easily be revealed, but it
 * requires some extra effort. For unknown streaming servers the movie player
 * uses just the normal movie URL.
 * 
 * <P>
 * Initial Date: 28.05.2009 <br>
 * 
 * @author gnaegi
 */
public class OlatMovieViewerPlugin extends TinyMCECustomPlugin {
	public static final String PLUGIN_NAME = "olatmovieviewer";
	public static final String BUTTONS = "olatmovieviewer";

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}
	
	@Override
	public Map<String, String> getPluginParameters() {
		// Create only if not already present.
		Map<String, String> params = super.getPluginParameters();
		if (params == null) {
			params = new HashMap<String, String>();
		}

		// Get static URI for transparent GIF.
		params.put("transparentImage", StaticMediaDispatcher.createStaticURIFor("images/transparent.gif", false));
		params.put("playerScript", StaticMediaDispatcher.createStaticURIFor("movie/player.js", true));

		setPluginParameters(params);
		return params;
	}
}
