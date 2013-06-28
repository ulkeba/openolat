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
package org.olat.portfolio.ui.structel;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.basesecurity.Invitation;
import org.olat.basesecurity.Policy;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.group.BusinessGroup;
import org.olat.portfolio.manager.EPMapPolicy;

/**
 * 
 * Initial date: 28.06.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class EPSharePolicyWrapper {
	private EPMapPolicy mapPolicy;
	private String componentName;
	private FormLayoutContainer userListBox;
	private FormLayoutContainer groupListBox;
	private DateChooser fromChooser;
	private DateChooser toChooser;
	private TextElement firstNameEl;
	private TextElement lastNameEl;
	private TextElement mailEl;
	private boolean invitationSend = false;;
	
	public EPSharePolicyWrapper() {
		this.mapPolicy = new EPMapPolicy();
	}
	
	public EPSharePolicyWrapper(EPMapPolicy mapPolicy) {
		this.mapPolicy = mapPolicy;
	}
	
	public EPMapPolicy getMapPolicy() {
		return mapPolicy;
	}

	public String getComponentName() {
		return componentName;
	}
	
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}
	
	public boolean isInvitationSend() {
		return invitationSend;
	}

	public void setInvitationSend(boolean invitationSend) {
		this.invitationSend = invitationSend;
	}

	public Invitation getInvitation() {
		return mapPolicy.getInvitation();
	}
	
	public void setInvitation(Invitation invitation) {
		mapPolicy.setInvitation(invitation);
	}
	
	public List<Policy> getPolicies() {
		return mapPolicy.getPolicies();
	}
	
	public void setPolicies(List<Policy> policies) {
		mapPolicy.setPolicies(policies);
	}
	
	public void addPolicy(Policy policy) {
		mapPolicy.addPolicy(policy);
	}

	public Date getTo() {
		return mapPolicy.getTo();
	}

	public void setTo(Date to) {
		mapPolicy.setTo(to);
	}

	public Date getFrom() {
		return mapPolicy.getFrom();
	}

	public void setFrom(Date from) {
		mapPolicy.setFrom(from);
	}

	public EPMapPolicy.Type getType() {
		return mapPolicy.getType();
	}

	public void setType(EPMapPolicy.Type type) {
		if(!type.equals(mapPolicy.getType())) {
			mapPolicy.setType(type);
			mapPolicy.getPolicies().clear();
		}
	}
	
	public Map<String,String> getIdentitiesValue() {
		if(mapPolicy.getIdentities() == null) return new HashMap<String,String>();
		
		Map<String,String> values = new HashMap<String,String>();
		for(Identity identity:mapPolicy.getIdentities()) {
			String login = identity.getName();//TODO username
			values.put(formatIdentity(identity), login);
		}
		return values;
	}
	
	protected String formatIdentity(Identity ident) {
		User u = ident.getUser();
		String login = ident.getName();//TODO username
		String first = u.getProperty(UserConstants.FIRSTNAME, null);
		String last = u.getProperty(UserConstants.LASTNAME, null);
		return login + ": " + last + " " + first;
	}

	public List<Identity> getIdentities() {
		return mapPolicy.getIdentities();
	}

	public void setIdentities(List<Identity> identities) {
		mapPolicy.setIdentities(identities);
	}
	
	public String getGroupsToString() {
		if(mapPolicy.getGroups() == null) return "";
		
		StringBuilder sb = new StringBuilder();
		for(BusinessGroup group:mapPolicy.getGroups()) {
			if(sb.length() > 0) sb.append(", ");
			sb.append(group.getName() == null ? "???" : group.getName());
		}
		return sb.toString();
	}

	public List<BusinessGroup> getGroups() {
		return mapPolicy.getGroups();
	}

	public void setGroups(List<BusinessGroup> groups) {
		mapPolicy.setGroups(groups);
	}
	
	public void addGroup(BusinessGroup group) {
		mapPolicy.addGroup(group);
	}
	
	public String calc(String cmpName) {
		return cmpName + "." + componentName;
	}

	public FormLayoutContainer getUserListBox() {
		return userListBox;
	}

	public void setUserListBox(FormLayoutContainer userListBox) {
		this.groupListBox = null;
		this.userListBox = userListBox;
	}

	public FormLayoutContainer getGroupListBox() {
		return groupListBox;
	}

	public void setGroupListBox(FormLayoutContainer groupListBox) {
		this.userListBox = null;
		this.groupListBox = groupListBox;
	}

	public DateChooser getFromChooser() {
		return fromChooser;
	}

	public void setFromChooser(DateChooser fromChooser) {
		this.fromChooser = fromChooser;
	}

	public DateChooser getToChooser() {
		return toChooser;
	}

	public void setToChooser(DateChooser toChooser) {
		this.toChooser = toChooser;
	}

	public TextElement getFirstNameEl() {
		return firstNameEl;
	}

	public void setFirstNameEl(TextElement firstNameEl) {
		this.firstNameEl = firstNameEl;
	}

	public TextElement getLastNameEl() {
		return lastNameEl;
	}

	public void setLastNameEl(TextElement lastNameEl) {
		this.lastNameEl = lastNameEl;
	}

	public TextElement getMailEl() {
		return mailEl;
	}

	public void setMailEl(TextElement mailEl) {
		this.mailEl = mailEl;
	}
}