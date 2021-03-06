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

package org.olat.course.nodes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import org.olat.core.commons.services.notifications.NotificationsManager;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.BreadcrumbPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionExpression;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.export.CourseEnvironmentMapper;
import org.olat.course.nodes.fo.FOCourseNodeEditController;
import org.olat.course.nodes.fo.FOCourseNodeRunController;
import org.olat.course.nodes.fo.FOPeekviewController;
import org.olat.course.nodes.fo.FOPreviewController;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.properties.PersistingCoursePropertyManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumCallback;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.archiver.ForumArchiveManager;
import org.olat.modules.fo.archiver.formatters.ForumStreamedRTFFormatter;
import org.olat.properties.Property;
import org.olat.repository.RepositoryEntry;

/**
 * Initial Date: Feb 9, 2004
 * 
 * @author Mike Stock Comment:
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class FOCourseNode extends AbstractAccessableCourseNode {

	private static final long serialVersionUID = 2281715263255594865L;
	private static final String PACKAGE_FO = Util.getPackageName(FOCourseNodeRunController.class);
	private static final String TYPE = "fo";
	private Condition preConditionReader, preConditionPoster, preConditionModerator;
	// null means no precondition / always accessible
	public static final String FORUM_KEY = "forumKey";

	/**
	 * Default constructor to create a forum course node
	 */
	public FOCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
		// restrict moderator access to course admins and course coaches
		preConditionModerator = getPreConditionModerator();
		preConditionModerator.setEasyModeCoachesAndAdmins(true);
		preConditionModerator.setConditionExpression(preConditionModerator.getConditionFromEasyModeConfiguration());
		preConditionModerator.setExpertMode(false);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	@Override
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, BreadcrumbPanel stackPanel, ICourse course, UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		FOCourseNodeEditController childTabCntrllr = new FOCourseNodeEditController(ureq, wControl, this, course, euce);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, euce, childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			final UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		
		Forum theForum = loadOrCreateForum(userCourseEnv.getCourseEnvironment());
		boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		boolean isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();
		// Add message id to business path if nodemcd is available
		if (nodecmd != null) {
			try {
				Long messageId = Long.valueOf(nodecmd);
				BusinessControlFactory bcf =  BusinessControlFactory.getInstance();
				BusinessControl businessControl = bcf.createFromString("[Message:"+messageId+"]");
				wControl = bcf.createBusinessWindowControl(businessControl, wControl);
			} catch (NumberFormatException e) {
				// ups, nodecmd is not a message, what the heck is it then?
				Tracing.createLoggerFor(this.getClass()).warn("Could not create message ID from given nodemcd::" + nodecmd, e);
			}
		}
		// Create subscription context and run controller
		SubscriptionContext forumSubContext = CourseModule.createSubscriptionContext(userCourseEnv.getCourseEnvironment(), this);
		FOCourseNodeRunController forumC = new FOCourseNodeRunController(ureq, userCourseEnv, wControl, theForum, 
				new ForumNodeForumCallback(ne, isOlatAdmin, isGuestOnly, forumSubContext), this);
		return new NodeRunConstructionResult(forumC);
	}

	/**
	 * Private helper method to load the forum from the configuration or create on
	 * if it does not yet exist
	 * 
	 * @param userCourseEnv
	 * @return the loaded forum
	 */
	public Forum loadOrCreateForum(final CourseEnvironment courseEnv) {
		updateModuleConfigDefaults(false);				
		final ForumManager fom = ForumManager.getInstance();
		final CoursePropertyManager cpm = courseEnv.getCoursePropertyManager();
		final CourseNode thisCourseNode = this;
		Forum theForum = null;
			
		Property forumKeyProp = cpm.findCourseNodeProperty(thisCourseNode, null, null, FORUM_KEY);
		//System.out.println("System.out.println - findCourseNodeProperty");
		if(forumKeyProp!=null) {
      // Forum does already exist, load forum with key from properties
		  Long forumKey = forumKeyProp.getLongValue();
		  theForum = fom.loadForum(forumKey);
		  if (theForum == null) { throw new OLATRuntimeException(FOCourseNode.class, "Tried to load forum with key " + forumKey.longValue() + " in course "
				+ courseEnv.getCourseResourceableId() + " for node " + thisCourseNode.getIdent()
				+ " as defined in course node property but forum manager could not load forum.", null); }
		} else {
      //creates resourceable from FOCourseNode.class and the current node id as key
			OLATResourceable courseNodeResourceable = OresHelper.createOLATResourceableInstance(FOCourseNode.class, new Long(this.getIdent()));
      //o_clusterOK by:ld 
		  theForum = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(courseNodeResourceable, new SyncerCallback<Forum>(){
			  public Forum execute() {
			  Forum forum = null;
			  Long forumKey;
			  Property forumKeyProperty = cpm.findCourseNodeProperty(thisCourseNode, null, null, FORUM_KEY);			  
			  if (forumKeyProperty == null) {
				  // First call of forum, create new forum and save forum key as property			  	
				  forum = fom.addAForum();
				  forumKey = forum.getKey();
				  forumKeyProperty = cpm.createCourseNodePropertyInstance(thisCourseNode, null, null, FORUM_KEY, null, forumKey, null, null);
				  cpm.saveProperty(forumKeyProperty);	
				  //System.out.println("Forum added");
			  } else {
			    // Forum does already exist, load forum with key from properties
					forumKey = forumKeyProperty.getLongValue();
					forum = fom.loadForum(forumKey);
					if (forum == null) { throw new OLATRuntimeException(FOCourseNode.class, "Tried to load forum with key " + forumKey.longValue() + " in course "
							+ courseEnv.getCourseResourceableId() + " for node " + thisCourseNode.getIdent()
							+ " as defined in course node property but forum manager could not load forum.", null); }
					}
			  //System.out.println("Forum already exists");
			  return forum;
		  }});
		}
		return theForum;
	}

	protected void calcAccessAndVisibility(ConditionInterpreter ci, NodeEvaluation nodeEval) {
		// evaluate the preconditions
		boolean reader = (getPreConditionReader().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionReader()));
		nodeEval.putAccessStatus("reader", reader);
		boolean poster = (getPreConditionPoster().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionPoster()));
		nodeEval.putAccessStatus("poster", poster);
		boolean moderator = (getPreConditionModerator().getConditionExpression() == null ? true : ci
				.evaluateCondition(getPreConditionModerator()));
		nodeEval.putAccessStatus("moderator", moderator);

		boolean visible = (getPreConditionVisibility().getConditionExpression() == null ? true : ci
				.evaluateCondition(getPreConditionVisibility()));
		nodeEval.setVisible(visible);
	}

	/**
	 * implementation of the previewController for forumnode
	 * 
	 * @see org.olat.course.nodes.CourseNode#createPreviewController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	public Controller createPreviewController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv, NodeEvaluation ne) {
		return new FOPreviewController(ureq, wControl, ne);
	}
	
	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPeekViewRunController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPeekViewRunController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv,
			NodeEvaluation ne) {
		if (ne.isAtLeastOneAccessible()) {
			// Create a forum peekview controller that shows the latest two messages		
			Forum theForum = loadOrCreateForum(userCourseEnv.getCourseEnvironment());
			Controller peekViewController = new FOPeekviewController(ureq, wControl, theForum, getIdent(), 2);
			return peekViewController;			
		} else {
			// use standard peekview
			return super.createPeekViewRunController(ureq, wControl, userCourseEnv, ne);
		}
	}

	/**
	 * @return Returns the preConditionModerator.
	 */
	public Condition getPreConditionModerator() {
		if (preConditionModerator == null) {
			preConditionModerator = new Condition();
		}
		preConditionModerator.setConditionId("moderator");
		return preConditionModerator;
	}

	/**
	 * @param preConditionModerator The preConditionModerator to set.
	 */
	public void setPreConditionModerator(Condition preConditionModerator) {
		if (preConditionModerator == null) {
			preConditionModerator = getPreConditionModerator();
		}
		preConditionModerator.setConditionId("moderator");
		this.preConditionModerator = preConditionModerator;
	}

	/**
	 * @return Returns the preConditionPoster.
	 */
	public Condition getPreConditionPoster() {
		if (preConditionPoster == null) {
			preConditionPoster = new Condition();
		}
		preConditionPoster.setConditionId("poster");
		return preConditionPoster;
	}

	/**
	 * @param preConditionPoster The preConditionPoster to set.
	 */
	public void setPreConditionPoster(Condition preConditionPoster) {
		if (preConditionPoster == null) {
			preConditionPoster = getPreConditionPoster();
		}
		preConditionPoster.setConditionId("poster");
		this.preConditionPoster = preConditionPoster;
	}

	/**
	 * @return Returns the preConditionReader.
	 */
	public Condition getPreConditionReader() {
		if (preConditionReader == null) {
			preConditionReader = new Condition();
		}
		preConditionReader.setConditionId("reader");
		return preConditionReader;
	}

	/**
	 * @param preConditionReader The preConditionReader to set.
	 */
	public void setPreConditionReader(Condition preConditionReader) {
		if (preConditionReader == null) {
			preConditionReader = getPreConditionReader();
		}
		preConditionReader.setConditionId("reader");
		this.preConditionReader = preConditionReader;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	public StatusDescription isConfigValid() {
		/*
		 * first check the one click cache
		 */
		if(oneClickStatusCache!=null) {
			return oneClickStatusCache[0];
		}
		
		return StatusDescription.NOERROR;
	}


	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		oneClickStatusCache = null;
		//only here we know which translator to take for translating condition error messages
		String translatorStr = Util.getPackageName(FOCourseNodeEditController.class);
		List<StatusDescription> sds = isConfigValidWithTranslator(cev, translatorStr,getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}
	
	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}

	@Override
	public boolean archiveNodeData(Locale locale, ICourse course, ArchiveOptions options, ZipOutputStream exportStream, String charset) {
		CoursePropertyManager cpm = course.getCourseEnvironment().getCoursePropertyManager();
		Property forumKeyProperty = cpm.findCourseNodeProperty(this, null, null, FORUM_KEY);
		if(forumKeyProperty == null) {
			return false;
		}
		Long forumKey = forumKeyProperty.getLongValue();
		if(ForumManager.getInstance().countThreadsByForumID(forumKey) <= 0) {
			return false;
		}
		
		String forumName = "forum_" + Formatter.makeStringFilesystemSave(getShortTitle())
				+ "_" + Formatter.formatDatetimeFilesystemSave(new Date(System.currentTimeMillis()));
		ForumStreamedRTFFormatter rtff = new ForumStreamedRTFFormatter(exportStream, forumName, false);	
		ForumArchiveManager.getInstance().applyFormatter(rtff, forumKey, null);
		return true;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#informOnDelete(org.olat.core.gui.UserRequest,
	 *      org.olat.course.ICourse)
	 */
	public String informOnDelete(Locale locale, ICourse course) {
		CoursePropertyManager cpm = PersistingCoursePropertyManager.getInstance(course);
		Property forumKeyProperty = cpm.findCourseNodeProperty(this, null, null, FORUM_KEY);
		if (forumKeyProperty == null) return null; // no forum created yet
		return new PackageTranslator(PACKAGE_FO, locale).translate("warn.forumdelete");
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#cleanupOnDelete(org.olat.course.ICourse)
	 */
	public void cleanupOnDelete(ICourse course) {
		// mark the subscription to this node as deleted
		SubscriptionContext forumSubContext = CourseModule.createTechnicalSubscriptionContext(course.getCourseEnvironment(), this);
		NotificationsManager.getInstance().delete(forumSubContext);

		// delete the forum, if there is one (is created on demand only)
		CoursePropertyManager cpm = PersistingCoursePropertyManager.getInstance(course);
		Property forumKeyProperty = cpm.findCourseNodeProperty(this, null, null, FORUM_KEY);
		if (forumKeyProperty == null) return; // no forum created yet
		Long forumKey = forumKeyProperty.getLongValue();
		ForumManager.getInstance().deleteForum(forumKey); // delete the forum
		cpm.deleteProperty(forumKeyProperty); // delete the property
	}

	/**
	 * Update the module configuration to have all mandatory configuration flags
	 * set to usefull default values
	 * 
	 * @param isNewNode true: an initial configuration is set; false: upgrading
	 *          from previous node configuration version, set default to maintain
	 *          previous behaviour
	 */
	@Override
	public void updateModuleConfigDefaults(boolean isNewNode) {
		ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode || config.getConfigurationVersion() < 2) {
			// use defaults for new course building blocks
			config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, Boolean.FALSE.booleanValue());
			config.setConfigurationVersion(2);
		}
		// else node is up-to-date - nothing to do
		config.remove(NodeEditController.CONFIG_INTEGRATION);
	}
	
	@Override
	protected void postImportCopyConditions(CourseEnvironmentMapper envMapper) {
		super.postImportCopyConditions(envMapper);
		postImportCondition(preConditionReader, envMapper);
		postImportCondition(preConditionPoster, envMapper);
		postImportCondition(preConditionModerator, envMapper);
	}

	@Override
	public void postExport(CourseEnvironmentMapper envMapper, boolean backwardsCompatible) {
		super.postExport(envMapper, backwardsCompatible);
		postExportCondition(preConditionReader, envMapper, backwardsCompatible);
		postExportCondition(preConditionPoster, envMapper, backwardsCompatible);
		postExportCondition(preConditionModerator, envMapper, backwardsCompatible);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#getConditionExpressions()
	 */
	public List<ConditionExpression> getConditionExpressions() {
		List<ConditionExpression> retVal;
		List<ConditionExpression> parentsConditions = super.getConditionExpressions();
		if (parentsConditions.size() > 0) {
			retVal = new ArrayList<ConditionExpression>(parentsConditions);
		}else {
			retVal = new ArrayList<ConditionExpression>();
		}
		//
		String coS = getPreConditionModerator().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			ConditionExpression ce = new ConditionExpression(getPreConditionModerator().getConditionId());
			ce.setExpressionString(getPreConditionModerator().getConditionExpression());
			retVal.add(ce);
		}
		coS = getPreConditionPoster().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			ConditionExpression ce = new ConditionExpression(getPreConditionPoster().getConditionId());
			ce.setExpressionString(getPreConditionPoster().getConditionExpression());
			retVal.add(ce);
		}
		coS = getPreConditionReader().getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			ConditionExpression ce = new ConditionExpression(getPreConditionReader().getConditionId());
			ce.setExpressionString(getPreConditionReader().getConditionExpression());
			retVal.add(ce);
		}
		//
		return retVal;
	}
}
/**
 * 
 * Description:<br>
 * ForumCallback implementation.
 * 
 */
class ForumNodeForumCallback implements ForumCallback {

	private NodeEvaluation ne;
	private boolean isOlatAdmin;
	private boolean isGuestOnly;
	private final SubscriptionContext subscriptionContext;

	/**
	 * @param ne the nodeevaluation for this coursenode
	 * @param isOlatAdmin true if the user is olat-admin
	 * @param isGuestOnly true if the user is olat-guest
	 * @param subscriptionContext
	 */
	public ForumNodeForumCallback(NodeEvaluation ne, boolean isOlatAdmin, boolean isGuestOnly, SubscriptionContext subscriptionContext) {
		this.ne = ne;
		this.isOlatAdmin = isOlatAdmin;
		this.isGuestOnly = isGuestOnly;
		this.subscriptionContext = subscriptionContext;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayOpenNewThread()
	 */
	public boolean mayOpenNewThread() {
		if (isGuestOnly) return false;
		return ne.isCapabilityAccessible("poster") || ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayReplyMessage()
	 */
	public boolean mayReplyMessage() {
		if (isGuestOnly) return false;
		return ne.isCapabilityAccessible("poster") || ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayEditMessageAsModerator()
	 */
	public boolean mayEditMessageAsModerator() {
		if (isGuestOnly) return false;
		return ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#mayDeleteMessageAsModerator()
	 */
	public boolean mayDeleteMessageAsModerator() {
		if (isGuestOnly) return false;
		return ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * 
	 * @see org.olat.modules.fo.ForumCallback#mayArchiveForum()
	 */
	public boolean mayArchiveForum() {
		if (isGuestOnly) return false;
		else return true;
	}
	
	/**
	 * @see org.olat.modules.fo.ForumCallback#mayFilterForUser()
	 */
	public boolean mayFilterForUser() {
		if (isGuestOnly) return false;
		return ne.isCapabilityAccessible("moderator") || isOlatAdmin;
	}

	/**
	 * @see org.olat.modules.fo.ForumCallback#getSubscriptionContext()
	 */
	public SubscriptionContext getSubscriptionContext() {
	// SubscriptionContext sc = new SubscriptionContext("coourseli", new
	// Long(123), "subident", "Einfuehrung in die Blabla", "Knoten gugus");
	// do not offer subscription to forums for guests
		return (isGuestOnly ? null : subscriptionContext);
	}

}
