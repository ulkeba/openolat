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
package org.olat.commons.calendar.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.time.DateUtils;
import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarUtils;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.model.KalendarEventLink;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.commons.calendar.ui.events.KalendarGUIEditEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.helpers.Settings;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;

/**
 * 
 * Initial date: 09.04.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CalendarDetailsController extends BasicController {

	private final KalendarEvent calEvent;
	private final KalendarRenderWrapper calWrapper;
	
	private Link editButton;
	private final VelocityContainer mainVC;
	
	private final boolean isGuestOnly;
	
	public CalendarDetailsController(UserRequest ureq, WindowControl wControl,
			KalendarEvent event, KalendarRenderWrapper calWrapper) {
		super(ureq, wControl, Util.createPackageTranslator(CalendarManager.class, ureq.getLocale()));
		this.calEvent = event;
		this.calWrapper = calWrapper;
		isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();
		mainVC = createVelocityContainer("event_details");
		
		if(!isGuestOnly) {
			editButton = LinkFactory.createButton("edit", mainVC, this);
			mainVC.put("edit", editButton);
		}
		mainVC.contextPut("date", formatDate());
		mainVC.contextPut("subject", event.getSubject());
		if(StringHelper.containsNonWhitespace(event.getLocation())) {
			mainVC.contextPut("location", event.getLocation());
		}
		mainVC.contextPut("links", renderEventLinks());
		putInitialPanel(mainVC);
	}
	
	private String formatDate() {
		Locale locale = getLocale();
		Calendar cal = CalendarUtils.createCalendarInstance(locale);
		Date begin = calEvent.getBegin();
		Date end = calEvent.getEnd();	
		cal.setTime(begin);
		
		StringBuilder sb = new StringBuilder();
		sb.append(StringHelper.formatLocaleDateFull(begin.getTime(), locale));
		if (!calEvent.isAllDayEvent()) {
			sb.append("<br />").append(StringHelper.formatLocaleTime(begin.getTime(), locale));
			sb.append(" - ");
			if (!DateUtils.isSameDay(begin, end)) {
				sb.append(StringHelper.formatLocaleDateFull(end.getTime(), locale)).append(", ");
			} 
			sb.append(StringHelper.formatLocaleTime(end.getTime(), locale));
		}
		return sb.toString();
	}
	
	
	private List<LinkWrapper> renderEventLinks() {
		List<LinkWrapper> linkWrappers = new ArrayList<LinkWrapper>();
		List<KalendarEventLink> kalendarEventLinks = calEvent.getKalendarEventLinks();
		if (kalendarEventLinks != null && !kalendarEventLinks.isEmpty()) {
			String rootUri = Settings.getServerContextPathURI();
			for (KalendarEventLink link: kalendarEventLinks) {
				LinkWrapper wrapper = new LinkWrapper();
				
				String uri = link.getURI();
				String iconCssClass = link.getIconCssClass();
				if(!StringHelper.containsNonWhitespace(iconCssClass)) {
					String displayName = link.getDisplayName();
					iconCssClass = CSSHelper.createFiletypeIconCssClassFor(displayName);
				}
				
				wrapper.setUri(uri);
				wrapper.setDisplayName(link.getDisplayName());
				wrapper.setTitle(StringEscapeUtils.escapeHtml(link.getDisplayName()));
				if (StringHelper.containsNonWhitespace(iconCssClass)) {
					wrapper.setCssClass(iconCssClass);
				}

				if(uri.startsWith(rootUri)) {
					//intern link with absolute URL
					wrapper.setIntern(true);
				} else if(uri.contains("://")) {
					//extern link with absolute URL
					wrapper.setIntern(false);
				} else {
					wrapper.setIntern(true);
				} 
				linkWrappers.add(wrapper);
			}
		}
		return linkWrappers;
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(source == editButton) {
			if(!isGuestOnly) {
				fireEvent(ureq, new KalendarGUIEditEvent(calEvent, calWrapper));
			}
		}
	}
	
	public static class LinkWrapper {
		
		private boolean intern;
		private String uri;
		private String title;
		private String cssClass;
		private String displayName;
		
		public boolean isIntern() {
			return intern;
		}

		public void setIntern(boolean intern) {
			this.intern = intern;
		}

		public String getUri() {
			return uri;
		}
		
		public void setUri(String uri) {
			this.uri = uri;
		}
		
		public String getTitle() {
			return title;
		}
		
		public void setTitle(String title) {
			this.title = title;
		}
		
		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public String getCssClass() {
			return cssClass;
		}
		
		public void setCssClass(String cssClass) {
			this.cssClass = cssClass;
		}
	}
}