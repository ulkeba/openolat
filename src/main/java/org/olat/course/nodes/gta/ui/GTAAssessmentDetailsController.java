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
package org.olat.course.nodes.gta.ui;

import java.util.List;

import org.olat.basesecurity.GroupRoles;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.course.nodes.GTACourseNode;
import org.olat.course.nodes.gta.GTAManager;
import org.olat.course.nodes.gta.GTAType;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.modules.ModuleConfiguration;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 09.03.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class GTAAssessmentDetailsController extends BasicController {

	private GTACoachController coachingCtrl;
	private GTACoachedGroupListController groupListCtrl;
	
	private final Link backLink;
	private final VelocityContainer mainVC;
	
	private final GTACourseNode gtaNode;
	private final CourseEnvironment courseEnv;
	private final Identity assessedIdentity;
	
	@Autowired
	private GTAManager gtaManager;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RepositoryManager repositoryManager;
	
	public GTAAssessmentDetailsController(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, GTACourseNode gtaNode) {
		super(ureq, wControl);
		this.gtaNode = gtaNode;
		this.courseEnv = userCourseEnv.getCourseEnvironment();
		
		mainVC = createVelocityContainer("assessment_details");
		backLink = LinkFactory.createLinkBack(mainVC, this);
		
		assessedIdentity = userCourseEnv.getIdentityEnvironment().getIdentity();
		
		ModuleConfiguration config = gtaNode.getModuleConfiguration();
		if(GTAType.group.name().equals(config.getStringValue(GTACourseNode.GTASK_TYPE))) {
			List<BusinessGroup> participatingGroups = gtaManager.getParticipatingBusinessGroups(assessedIdentity, gtaNode);
			//filters by coaching habilities
			if(participatingGroups.size() > 1) {
				String msg = translate("error.duplicate.coaching");
				mainVC.contextPut("multipleGroupsWarning", msg);
			}
			
			Roles roles = ureq.getUserSession().getRoles();
			RepositoryEntry courseRe = courseEnv.getCourseGroupManager().getCourseEntry();
			if(!roles.isOLATAdmin() && !repositoryManager.isInstitutionalRessourceManagerFor(getIdentity(), roles, courseRe)) {
				List<String> reRoles = repositoryService.getRoles(getIdentity(), courseRe);
				if(reRoles.contains(GroupRoles.owner.name())) {
					//view all groups;
				} else if(reRoles.contains(GroupRoles.coach.name())) {
					List<BusinessGroup> coachedGroups = gtaManager.getCoachedBusinessGroups(getIdentity(), gtaNode);
					participatingGroups.retainAll(coachedGroups);
				} else {
					participatingGroups.clear();
				}
			}
			
			if(participatingGroups.isEmpty()) {
				String title = translate("error.not.member.title");
				String message = translate("error.not.member.message");
				Controller msgCtrl = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
				listenTo(msgCtrl);
				mainVC.put("list", msgCtrl.getInitialComponent());
			} else if(participatingGroups.size() == 1) {
				doSelectBusinessGroup(ureq, participatingGroups.get(0));
			} else {
				groupListCtrl = new GTACoachedGroupListController(ureq, getWindowControl(), courseEnv, gtaNode, participatingGroups);
				listenTo(groupListCtrl);
				mainVC.put("list", groupListCtrl.getInitialComponent());
			}	
		} else {
			doSelectParticipant(ureq, assessedIdentity);
		}
		
		putInitialPanel(mainVC);
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(groupListCtrl == source) {
			if(event instanceof SelectBusinessGroupEvent) {
				SelectBusinessGroupEvent selectEvent = (SelectBusinessGroupEvent)event;
				doSelectBusinessGroup(ureq, selectEvent.getBusinessGroup());
				backLink.setVisible(true);
			}
		}
		super.event(ureq, source, event);
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(backLink == source) {
			back();
		}
	}
	
	private void back() {
		if(coachingCtrl != null) {
			mainVC.remove(coachingCtrl.getInitialComponent());
			removeAsListenerAndDispose(coachingCtrl);
			coachingCtrl = null;
		}
		backLink.setVisible(false);
	}
	
	private void doSelectBusinessGroup(UserRequest ureq, BusinessGroup group) {
		removeAsListenerAndDispose(coachingCtrl);
		coachingCtrl = new GTACoachController(ureq, getWindowControl(), courseEnv, gtaNode, group, true, true);
		listenTo(coachingCtrl);
		mainVC.put("selection", coachingCtrl.getInitialComponent());
	}
	
	private void doSelectParticipant(UserRequest ureq, Identity identity) {
		removeAsListenerAndDispose(coachingCtrl);
		coachingCtrl = new GTACoachController(ureq, getWindowControl(), courseEnv, gtaNode, identity, false, false);
		listenTo(coachingCtrl);
		mainVC.put("selection", coachingCtrl.getInitialComponent());
	}
}
