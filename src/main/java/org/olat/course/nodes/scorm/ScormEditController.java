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

package org.olat.course.nodes.scorm;

import java.io.File;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.rules.RulesFactory;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.iframe.DeliveryOptions;
import org.olat.core.gui.control.generic.iframe.DeliveryOptionsConfigurationController;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.nodes.ScormCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.ScormCPFileResource;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.scorm.ScormAPIandDisplayController;
import org.olat.modules.scorm.ScormConstants;
import org.olat.modules.scorm.ScormMainManager;
import org.olat.modules.scorm.ScormPackageConfig;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.controllers.ReferencableEntriesSearchController;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description:<BR/> Edit controller for content packaging course nodes <P/>
 * 
 * Initial Date: Oct 13, 2004
 * @author Felix Jost
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class ScormEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	public static final String PANE_TAB_CPCONFIG = "pane.tab.cpconfig";
	private static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	private static final String PANE_TAB_DELIVERY = "pane.tab.delivery";


	private static final String CONFIG_KEY_REPOSITORY_SOFTKEY = "reporef";
	public static final String CONFIG_SHOWMENU = "showmenu";
	public static final String CONFIG_SKIPLAUNCHPAGE = "skiplaunchpage";
	public static final String CONFIG_SHOWNAVBUTTONS = "shownavbuttons";
	public static final String CONFIG_ISASSESSABLE = "isassessable";
	public static final String CONFIG_ASSESSABLE_TYPE = "assessabletype";
	public static final String CONFIG_ASSESSABLE_TYPE_SCORE = "score";
	public static final String CONFIG_ASSESSABLE_TYPE_PASSED = "passed";
	public static final String CONFIG_CUTVALUE = "cutvalue";
	
	public static final String CONFIG_DELIVERY_OPTIONS = "deliveryOptions";
	//fxdiff FXOLAT-116: SCORM improvements
	public final static String CONFIG_FULLWINDOW = "fullwindow";
	public final static String CONFIG_CLOSE_ON_FINISH = "CLOSEONFINISH";
	
	// <OLATCE-289>
	public static final String CONFIG_MAXATTEMPTS = "attempts";
	public static final String CONFIG_ADVANCESCORE = "advancescore";
	public static final String CONFIG_ATTEMPTSDEPENDONSCORE = "scoreattampts";
	// </OLATCE-289>

	private static final String VC_CHOSENCP = "chosencp";

	private static final String[] paneKeys = { PANE_TAB_CPCONFIG, PANE_TAB_ACCESSIBILITY };

	// NLS support:
	
	private static final String NLS_ERROR_CPREPOENTRYMISSING = "error.cprepoentrymissing";
	private static final String NLS_NO_CP_CHOSEN = "no.cp.chosen";
	private static final String NLS_CONDITION_ACCESSIBILITY_TITLE = "condition.accessibility.title";
	
	private Panel main;
	private VelocityContainer cpConfigurationVc;

	private ModuleConfiguration config;
	private ReferencableEntriesSearchController searchController;
	private CloseableModalController cmc;
	
	private ConditionEditController accessibilityCondContr;
	private DeliveryOptionsConfigurationController deliveryOptionsCtrl;
	private ScormCourseNode scormNode;

	private TabbedPane myTabbedPane;

	private ICourse course;

	private VarForm scorevarform;

	private Link previewLink;
	private Link chooseCPButton;
	private Link changeCPButton;

	/**
	 * @param cpNode CourseNode
	 * @param ureq
	 * @param wControl
	 * @param course Course Interface
	 * @param euce User course environment
	 */
	public ScormEditController(ScormCourseNode scormNode, UserRequest ureq, WindowControl wControl, ICourse course, UserCourseEnvironment euce) {
		super(ureq, wControl);
		//o_clusterOk by guido: save to hold reference to course inside editor
		this.course = course;
		this.scormNode = scormNode;
		this.config = scormNode.getModuleConfiguration();
		main = new Panel("cpmain");				
		cpConfigurationVc = this.createVelocityContainer("edit");
		
		chooseCPButton = LinkFactory.createButtonSmall("command.importcp", cpConfigurationVc, this);
		chooseCPButton.setElementCssClass("o_sel_scorm_choose_repofile");
		changeCPButton = LinkFactory.createButtonSmall("command.changecp", cpConfigurationVc, this);
		changeCPButton.setElementCssClass("o_sel_scorm_change_repofile");
		
		DeliveryOptions parentConfig = null;
		if (config.get(CONFIG_KEY_REPOSITORY_SOFTKEY) != null) {
			// fetch repository entry to display the repository entry title of the
			// chosen cp
			RepositoryEntry re = getScormCPReference(config, false);
			if (re == null) { // we cannot display the entries name, because the repository entry had been deleted 
												// between the time when it was chosen here, and now				
				showError(NLS_ERROR_CPREPOENTRYMISSING);
				cpConfigurationVc.contextPut("showPreviewButton", Boolean.FALSE);
				cpConfigurationVc.contextPut(VC_CHOSENCP, translate(NLS_NO_CP_CHOSEN));
			} else {
				cpConfigurationVc.contextPut("showPreviewButton", Boolean.TRUE);
				String displayname = StringHelper.escapeHtml(re.getDisplayname());
				previewLink = LinkFactory.createCustomLink("command.preview", "command.preview", displayname, Link.NONTRANSLATED, cpConfigurationVc, this);
				previewLink.setIconLeftCSS("o_icon o_icon-fw o_icon_preview");
				previewLink.setCustomEnabledLinkCSS("o_preview");
				previewLink.setTitle(getTranslator().translate("command.preview"));
				
				ScormPackageConfig scormConfig = ScormMainManager.getInstance().getScormPackageConfig(re.getOlatResource());
				parentConfig = scormConfig == null ? null : scormConfig.getDeliveryOptions();
			}
		} else {
			// no valid config yet
			cpConfigurationVc.contextPut("showPreviewButton", Boolean.FALSE);
			cpConfigurationVc.contextPut(VC_CHOSENCP, translate(NLS_NO_CP_CHOSEN));
		}
		
		// add the form for choosing the score variable
		boolean showMenu = config.getBooleanSafe(CONFIG_SHOWMENU, true);
		boolean showNavButtons = config.getBooleanSafe(CONFIG_SHOWNAVBUTTONS, true);
		boolean skipLaunchPage = config.getBooleanSafe(CONFIG_SKIPLAUNCHPAGE,false);
		
		// <OLATCE-289>
		boolean assessable = config.getBooleanSafe(CONFIG_ISASSESSABLE, true);
		String assessableType = null;
		if(assessable) {
			assessableType = config.getStringValue(CONFIG_ASSESSABLE_TYPE, CONFIG_ASSESSABLE_TYPE_SCORE);
		}
		boolean attemptsDependOnScore = config.getBooleanSafe(CONFIG_ATTEMPTSDEPENDONSCORE, true);
		int maxAttempts = config.getIntegerSafe(CONFIG_MAXATTEMPTS, 0);
		boolean advanceScore = config.getBooleanSafe(CONFIG_ADVANCESCORE, true);
		// </OLATCE-289>
		int cutvalue = config.getIntegerSafe(CONFIG_CUTVALUE, 0);
		//fxdiff FXOLAT-116: SCORM improvements
		boolean fullWindow = config.getBooleanSafe(CONFIG_FULLWINDOW, true);
		boolean closeOnFinish = config.getBooleanSafe(CONFIG_CLOSE_ON_FINISH, false);
		
		//= conf.get(CONFIG_CUTVALUE);
		scorevarform = new VarForm(ureq, wControl, showMenu, skipLaunchPage, showNavButtons,
				assessableType, cutvalue, fullWindow,
				closeOnFinish, maxAttempts, advanceScore, attemptsDependOnScore);
		listenTo(scorevarform);
		cpConfigurationVc.put("scorevarform", scorevarform.getInitialComponent());

		// Accessibility precondition
		Condition accessCondition = scormNode.getPreConditionAccess();
		accessibilityCondContr = new ConditionEditController(ureq, getWindowControl(), 
				accessCondition,
				AssessmentHelper.getAssessableNodes(course.getEditorTreeModel(), scormNode), euce);		
		listenTo(accessibilityCondContr);

		DeliveryOptions deliveryOptions = (DeliveryOptions)config.get(CONFIG_DELIVERY_OPTIONS);
		deliveryOptionsCtrl = new DeliveryOptionsConfigurationController(ureq, getWindowControl(), deliveryOptions, parentConfig);
		listenTo(deliveryOptionsCtrl);

		main.setContent(cpConfigurationVc);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == chooseCPButton || source == changeCPButton) { // those must be links
			removeAsListenerAndDispose(searchController);
			searchController = new ReferencableEntriesSearchController(getWindowControl(), ureq,
					ScormCPFileResource.TYPE_NAME, translate("command.choosecp"));			
			listenTo(searchController);
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), searchController.getInitialComponent(), true, translate("command.importcp"));
			listenTo(cmc);
			cmc.activate();
		} else if (source == previewLink){
			// Preview as modal dialogue
			// only if the config is valid
			RepositoryEntry re = getScormCPReference(config, false);
			if (re == null) { // we cannot preview it, because the repository entry
												// had been deleted between the time when it was
												// chosen here, and now				
				showError("error.cprepoentrymissing");
			} else {
				File cpRoot = FileResourceManager.getInstance().unzipFileResource(re.getOlatResource());
				boolean showMenu = config.getBooleanSafe(CONFIG_SHOWMENU, true);
				boolean fullWindow = config.getBooleanSafe(CONFIG_FULLWINDOW, true);
				
				ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapScormRepositoryEntry(re));
				ScormAPIandDisplayController previewController = ScormMainManager.getInstance().createScormAPIandDisplayController(ureq, getWindowControl(),
						showMenu, null, cpRoot, null, course.getResourceableId().toString(), ScormConstants.SCORM_MODE_BROWSE,
						ScormConstants.SCORM_MODE_NOCREDIT, true, null, true, fullWindow, false, null);				
				// configure some display options
				boolean showNavButtons = config.getBooleanSafe(ScormEditController.CONFIG_SHOWNAVBUTTONS, true);
				previewController.showNavButtons(showNavButtons);
				
				DeliveryOptions deliveryOptions = deliveryOptionsCtrl.getOptionsForPreview();
				previewController.setDeliveryOptions(deliveryOptions);
				previewController.activate();
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest urequest, Controller source, Event event) {
		if (source == searchController) {			
			cmc.deactivate();
			if (event == ReferencableEntriesSearchController.EVENT_REPOSITORY_ENTRY_SELECTED) { 
				// search controller done
				RepositoryEntry re = searchController.getSelectedEntry();
				if (re != null) {
					setScormCPReference(re, config);
					cpConfigurationVc.contextPut("showPreviewButton", Boolean.TRUE);
					String displayname = StringHelper.escapeHtml(re.getDisplayname());
					previewLink = LinkFactory.createCustomLink("command.preview", "command.preview", displayname, Link.NONTRANSLATED, cpConfigurationVc, this);
					previewLink.setIconLeftCSS("o_icon o_icon-fw o_icon_preview");
					previewLink.setCustomEnabledLinkCSS("o_preview");
					previewLink.setTitle(getTranslator().translate("command.preview"));
					// fire event so the updated config is saved by the
					// editormaincontroller
					fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
					
					ScormPackageConfig scormConfig = ScormMainManager.getInstance().getScormPackageConfig(re.getOlatResource());
					DeliveryOptions parentConfig = scormConfig == null ? null : scormConfig.getDeliveryOptions();
					deliveryOptionsCtrl.setParentDeliveryOptions(parentConfig);
				}
				// else cancelled repo search
			}
		} else if (source == accessibilityCondContr) {
			if (event == Event.CHANGED_EVENT) {
				Condition cond = accessibilityCondContr.getCondition();
				scormNode.setPreConditionAccess(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == scorevarform) {
			if (event == Event.DONE_EVENT) {
				//save form-values to config
				
				config.setBooleanEntry(CONFIG_SHOWMENU, scorevarform.isShowMenu());
				//fxdiff FXOLAT-322
				config.setBooleanEntry(CONFIG_SKIPLAUNCHPAGE, scorevarform.isSkipLaunchPage());
				config.setBooleanEntry(CONFIG_SHOWNAVBUTTONS, scorevarform.isShowNavButtons());
				config.setBooleanEntry(CONFIG_ISASSESSABLE, scorevarform.isAssessable());
				config.setStringValue(CONFIG_ASSESSABLE_TYPE, scorevarform.getAssessableType());
				config.setIntValue(CONFIG_CUTVALUE, scorevarform.getCutValue());
				//fxdiff FXOLAT-116: SCORM improvements
				config.setBooleanEntry(CONFIG_FULLWINDOW, scorevarform.isFullWindow());
				config.setBooleanEntry(CONFIG_CLOSE_ON_FINISH, scorevarform.isCloseOnFinish());
				// <OLATCE-289>
				config.setIntValue(CONFIG_MAXATTEMPTS, scorevarform.getAttemptsValue());
				config.setBooleanEntry(CONFIG_ADVANCESCORE, scorevarform.isAdvanceScore());
				config.setBooleanEntry(CONFIG_ATTEMPTSDEPENDONSCORE, scorevarform.getAttemptsDependOnScore());
				// </OLATCE-289>
				// fire event so the updated config is saved by the
				// editormaincontroller
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if(source == deliveryOptionsCtrl) {
			if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				config.set(CONFIG_DELIVERY_OPTIONS, deliveryOptionsCtrl.getDeliveryOptions());
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableDefaultController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	public void addTabs(TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessibilityCondContr.getWrappedDefaultAccessConditionVC(translate(NLS_CONDITION_ACCESSIBILITY_TITLE)));
		tabbedPane.addTab(translate(PANE_TAB_CPCONFIG), main); // the choose learning content tab
		tabbedPane.addTab(translate(PANE_TAB_DELIVERY), deliveryOptionsCtrl.getInitialComponent());
	}

	/**
	 * @param config the moduleConfig
	 * @param strict an assertion exception is thrown if no entry is found when
	 *          strict is set to true, otherwise, null is returned
	 * @return the repositoryentry or null if not in strict mode and no entry
	 *         found
	 * @throws AssertException when in strict mode and no entry is found
	 */
	public static RepositoryEntry getScormCPReference(ModuleConfiguration config, boolean strict) {
		if (config == null) throw new AssertException("missing config in CP");
		String repoSoftkey = (String) config.get(ScormEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
		if (repoSoftkey == null) throw new AssertException("invalid config when being asked for references");
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry entry = rm.lookupRepositoryEntryBySoftkey(repoSoftkey, strict);
		// entry can be null only if !strict
		return entry;
	}

	/**
	 * Remove the reference to the repository entry.
	 * 
	 * @param moduleConfiguration
	 */
	public static void removeScormCPReference(ModuleConfiguration moduleConfiguration) {
		moduleConfiguration.remove(ScormEditController.CONFIG_KEY_REPOSITORY_SOFTKEY);
	}

	/**
	 * Set the referenced repository entry.
	 * 
	 * @param re
	 * @param moduleConfiguration
	 */
	public static void setScormCPReference(RepositoryEntry re, ModuleConfiguration moduleConfiguration) {
		moduleConfiguration.set(CONFIG_KEY_REPOSITORY_SOFTKEY, re.getSoftkey());
	}

	/**
	 * @param moduleConfiguration
	 * @return boolean
	 */
	public static boolean isModuleConfigValid(ModuleConfiguration moduleConfiguration) {
		return (moduleConfiguration.get(CONFIG_KEY_REPOSITORY_SOFTKEY) != null);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
    //child controllers registered with listenTo() get disposed in BasicController
	}

	public String[] getPaneKeys() {
		return paneKeys;
	}

	public TabbedPane getTabbedPane() {
		return myTabbedPane;
	}
}

class VarForm extends FormBasicController {
	private SelectionElement showMenuEl;
	private SelectionElement showNavButtonsEl;
	private SelectionElement fullWindowEl;//fxdiff FXOLAT-116: SCORM improvements
	private SelectionElement closeOnFinishEl;//fxdiff FXOLAT-116: SCORM improvements
	private SelectionElement isAssessableEl;
	private SelectionElement skipLaunchPageEl; //fxdiff FXOLAT-322 : skip start-page / auto-launch
	private IntegerElement cutValueEl;
	
	private boolean showMenu, showNavButtons, skipLaunchPage;
	private String assessableType;
	private int cutValue;
	private boolean fullWindow;//fxdiff FXOLAT-116: SCORM improvements
	private boolean closeOnFinish;//fxdiff FXOLAT-116: SCORM improvements
	private String[] assessableKeys, assessableValues;

	// <OLATCE-289>
	private SingleSelection attemptsEl;
	private MultipleSelectionElement advanceScoreEl;
	private MultipleSelectionElement scoreAttemptsEl;
	
	private boolean advanceScore;
	private boolean scoreAttempts;
	private int maxattempts;
	// </OLATCE-289>
	
	/**
	 * 
	 * @param name  Name of the form
	 */
	public VarForm(UserRequest ureq, WindowControl wControl, boolean showMenu, boolean skipLaunchPage, boolean showNavButtons, 
			String assessableType, int cutValue, boolean fullWindow, boolean closeOnFinish,
			// <OLATCE-289>
			int maxattempts, boolean advanceScore, boolean attemptsDependOnScore
			// </OLATCE-289>
			) {
		super(ureq, wControl);
		this.showMenu = showMenu;
		this.skipLaunchPage = skipLaunchPage;
		this.showNavButtons = showNavButtons;
		this.assessableType = assessableType;
		this.cutValue = cutValue;
		//fxdiff FXOLAT-116: SCORM improvements
		this.fullWindow = fullWindow;
		this.closeOnFinish = closeOnFinish;
		
		// <OLATCE-289>
		this.advanceScore = advanceScore;
		this.scoreAttempts = attemptsDependOnScore;
		this.maxattempts = maxattempts;
		// </OLATCE-289>
		
		assessableKeys = new String[]{ "off", ScormEditController.CONFIG_ASSESSABLE_TYPE_SCORE,
				ScormEditController.CONFIG_ASSESSABLE_TYPE_PASSED };
		assessableValues = new String[]{
				translate("assessable.type.none"), translate("assessable.type.score"), translate("assessable.type.passed")
		};
		initForm (ureq);
	}
	

	/**
	 * @return
	 */
	public int getCutValue() {
		return cutValueEl.getIntValue();
	}
	//fxdiff FXOLAT-116: SCORM improvements
	public boolean isFullWindow() {
		return fullWindowEl.isMultiselect() && fullWindowEl.isSelected(0);
	}
	//fxdiff FXOLAT-116: SCORM improvements
	public boolean isCloseOnFinish() {
		return closeOnFinishEl.isMultiselect() && closeOnFinishEl.isSelected(0);
	}

	public boolean isShowMenu() {
		return showMenuEl.isSelected(0);
	}
	
	public boolean isSkipLaunchPage() {
		return skipLaunchPageEl.isSelected(0);
	}

	public boolean isShowNavButtons() {
		return showNavButtonsEl.isSelected(0);
	}

	public boolean isAssessable() {
		return !isAssessableEl.isSelected(0);
	}
	
	public String getAssessableType() {
		if(isAssessableEl.isSelected(0)) {
			return null;
		} else if(isAssessableEl.isSelected(1)) {
			return ScormEditController.CONFIG_ASSESSABLE_TYPE_SCORE;
		} else if(isAssessableEl.isSelected(2)) {
			return ScormEditController.CONFIG_ASSESSABLE_TYPE_PASSED;
		}
		return null;
	}
	

	
	@Override
	protected void formOK(UserRequest ureq) {
		fireEvent (ureq, Event.DONE_EVENT);
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;

		return allOk && super.validateFormLogic(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("headerform");
		setFormContextHelp("org.olat.course.nodes.scorm","ced-scorm-settings.html","help.hover.scorm-settings-filename");
		
		showMenuEl = uifactory.addCheckboxesHorizontal("showmenu", "showmenu.label", formLayout, new String[]{"xx"}, new String[]{null});
		showMenuEl.select("xx", showMenu);
		
		skipLaunchPageEl = uifactory.addCheckboxesHorizontal("skiplaunchpage", "skiplaunchpage.label", formLayout, new String[]{"xx"}, new String[]{null});
		skipLaunchPageEl.select("xx", skipLaunchPage);
		
		showNavButtonsEl = uifactory.addCheckboxesHorizontal("shownavbuttons", "shownavbuttons.label", formLayout, new String[]{"xx"}, new String[]{null});
		showNavButtonsEl.select("xx", showNavButtons);
		//fxdiff FXOLAT-116: SCORM improvements
		fullWindowEl = uifactory.addCheckboxesHorizontal("fullwindow", "fullwindow.label", formLayout, new String[]{"fullwindow"}, new String[]{null});
		fullWindowEl.select("fullwindow", fullWindow);
		
		closeOnFinishEl = uifactory.addCheckboxesHorizontal("closeonfinish", "closeonfinish.label", formLayout, new String[]{"closeonfinish"}, new String[]{null});
		closeOnFinishEl.select("closeonfinish", closeOnFinish);

		isAssessableEl = uifactory.addRadiosVertical("isassessable", "assessable.label", formLayout, assessableKeys, assessableValues);
		if(ScormEditController.CONFIG_ASSESSABLE_TYPE_SCORE.equals(assessableType)) {
			isAssessableEl.select(assessableKeys[1], true);
		} else if(ScormEditController.CONFIG_ASSESSABLE_TYPE_PASSED.equals(assessableType)) {
			isAssessableEl.select(assessableKeys[2], true);
		} else {
			isAssessableEl.select(assessableKeys[0], true);
		}
		
		cutValueEl = uifactory.addIntegerElement("cutvalue", "cutvalue.label", 0, formLayout);
		cutValueEl.setIntValue(cutValue);
		cutValueEl.setDisplaySize(3);
		
		// <OLATCE-289>
		isAssessableEl.addActionListener(FormEvent.ONCHANGE);
		advanceScoreEl = uifactory.addCheckboxesHorizontal("advanceScore", "advance.score.label", formLayout, new String[]{"ison"}, new String[]{null});
		advanceScoreEl.select("ison", advanceScore);
		advanceScoreEl.addActionListener(FormEvent.ONCHANGE);

		//assessable type score/passed -> show "Prevent subsequent attempts from decreasing score"
		RulesFactory.createHideRule(isAssessableEl, assessableKeys[0], advanceScoreEl, formLayout);
		RulesFactory.createShowRule(isAssessableEl, assessableKeys[1], advanceScoreEl, formLayout);
		RulesFactory.createShowRule(isAssessableEl, assessableKeys[2], advanceScoreEl, formLayout);

		//assessable type score or none -> show "Score needed to pass"
		RulesFactory.createShowRule(isAssessableEl, assessableKeys[0], cutValueEl, formLayout);
		RulesFactory.createShowRule(isAssessableEl, assessableKeys[1], cutValueEl, formLayout);
		RulesFactory.createHideRule(isAssessableEl, assessableKeys[2], cutValueEl, formLayout);

		scoreAttemptsEl = uifactory.addCheckboxesHorizontal("scoreAttempts", "attempts.depends.label", formLayout, new String[]{"ison"}, new String[]{null});
		scoreAttemptsEl.select("ison", scoreAttempts);
		scoreAttemptsEl.addActionListener(FormEvent.ONCHANGE);
		
		RulesFactory.createShowRule(advanceScoreEl, "ison", scoreAttemptsEl, formLayout);
		RulesFactory.createHideRule(advanceScoreEl, null, scoreAttemptsEl, formLayout);
		
		// <BPS-252> BPS-252_1
		int maxNumber = 21;
		String[] attemptsKeys = new String[maxNumber];
		attemptsKeys[0] = "0"; // position 0 means no restriction
		for (int i = 1; i < maxNumber; i++) {
            attemptsKeys[i] = (String.valueOf(i));
        }
		String[] attemptsValues = new String[maxNumber];
		attemptsValues[0] = translate("attempts.noLimit");
	    for (int i = 1; i < maxNumber; i++) {
	            attemptsValues[i] = (String.valueOf(i) + " x");
	        }
		if (maxattempts >= maxNumber) {
			maxattempts = 0;
		}

		attemptsEl = uifactory.addDropdownSingleselect("attempts.label", formLayout, attemptsKeys, attemptsValues, null);
		attemptsEl.select("" + maxattempts, true);
		// </OLATCE-289>
		
		uifactory.addFormSubmitButton("save", formLayout);
	}
	
	// <OLATCE-289>
	public int getAttemptsValue() {
		return Integer.valueOf(attemptsEl.getSelectedKey()); 
	}
	public boolean isAdvanceScore() {
		return advanceScoreEl.isSelected(0);
	}
	
	public boolean getAttemptsDependOnScore() {
		return scoreAttemptsEl.isSelected(0);
	}

	@Override
	protected void doDispose() {
		//
	}
}