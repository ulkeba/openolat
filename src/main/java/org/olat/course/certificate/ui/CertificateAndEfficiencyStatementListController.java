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
package org.olat.course.certificate.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.NewControllerFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableComponent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiColumnModel;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.stack.BreadcrumbPanel;
import org.olat.core.gui.components.stack.BreadcrumbPanelAware;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.CorruptedCourseException;
import org.olat.course.assessment.EfficiencyStatement;
import org.olat.course.assessment.EfficiencyStatementManager;
import org.olat.course.assessment.IdentityAssessmentEditController;
import org.olat.course.assessment.bulk.PassedCellRenderer;
import org.olat.course.assessment.model.UserEfficiencyStatementLight;
import org.olat.course.assessment.portfolio.EfficiencyStatementArtefact;
import org.olat.course.certificate.CertificateEvent;
import org.olat.course.certificate.CertificateLight;
import org.olat.course.certificate.CertificatesManager;
import org.olat.course.certificate.ui.CertificateAndEfficiencyStatementListModel.CertificateAndEfficiencyStatement;
import org.olat.course.certificate.ui.CertificateAndEfficiencyStatementListModel.Cols;
import org.olat.portfolio.EPArtefactHandler;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.ui.artefacts.collect.ArtefactWizzardStepsController;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 21.10.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CertificateAndEfficiencyStatementListController extends FormBasicController
		implements BreadcrumbPanelAware, GenericEventListener {
	
	private static final String CMD_SHOW = "cmd.show";
	private static final String CMD_LAUNCH_COURSE = "cmd.launch.course";
	private static final String CMD_DELETE = "cmd.delete";
	private static final String CMD_ARTEFACT = "cmd.artefact";
	
	private FlexiTableElement tableEl;
	private BreadcrumbPanel stackPanel;
	private FormLink coachingToolButton;
	private CertificateAndEfficiencyStatementListModel tableModel;

	private DialogBoxController confirmDeleteCtr;
	private ArtefactWizzardStepsController ePFCollCtrl;
	
	private final boolean linkToCoachingTool;
	private Identity assessedIdentity;
	
	@Autowired
	private EfficiencyStatementManager esm;
	@Autowired
	private PortfolioModule portfolioModule;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private CertificatesManager certificatesManager;
	
	public CertificateAndEfficiencyStatementListController(UserRequest ureq, WindowControl wControl) {
		this(ureq, wControl, ureq.getUserSession().getIdentity(), false);
	}
	
	public CertificateAndEfficiencyStatementListController(UserRequest ureq, WindowControl wControl,
			Identity assessedIdentity, boolean linkToCoachingTool) {
		super(ureq, wControl, "cert_statement_list");
		setTranslator(Util.createPackageTranslator(IdentityAssessmentEditController.class, getLocale(), getTranslator()));
		this.assessedIdentity = assessedIdentity;
		this.linkToCoachingTool = linkToCoachingTool;
		
		initForm(ureq);
		
		CoordinatorManager.getInstance().getCoordinator().getEventBus()
			.registerFor(this, getIdentity(), CertificatesManager.ORES_CERTIFICATE_EVENT);
	}
	
	@Override
	protected void doDispose() {
		CoordinatorManager.getInstance().getCoordinator().getEventBus()
			.deregisterFor(this, CertificatesManager.ORES_CERTIFICATE_EVENT);
	}

	@Override
	public void event(Event event) {
		if(event instanceof CertificateEvent) {
			CertificateEvent ce = (CertificateEvent)event;
			if(getIdentity().getKey().equals(ce.getOwnerKey())) {
				updateStatement(ce.getResourceKey(), ce.getCertificateKey());
			}
		}
	}
	
	private void updateStatement(Long resourceKey, Long certificateKey) {
		List<CertificateAndEfficiencyStatement> statements = tableModel.getObjects();
		for(int i=statements.size(); i-->0; ) {
			CertificateAndEfficiencyStatement statement = statements.get(i);
			if(resourceKey.equals(statement.getResourceKey())) {
				CertificateLight certificate = certificatesManager.getCertificateLightById(certificateKey);
				statement.setCertificate(certificate);
				break;
			}
		}
	}

	@Override
	public void setBreadcrumbPanel(BreadcrumbPanel stackPanel) {
		this.stackPanel = stackPanel;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(linkToCoachingTool) {
			coachingToolButton = uifactory.addFormLink("coaching.tool", formLayout, Link.BUTTON);
		}
		
		FlexiTableColumnModel tableColumnModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.displayName.i18n(), Cols.displayName.ordinal()));
		tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.score.i18n(), Cols.score.ordinal()));
		tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.passed.i18n(), Cols.passed.ordinal(),
				new PassedCellRenderer()));
		tableColumnModel.addFlexiColumnModel(new StaticFlexiColumnModel("table.header.show",
				translate("table.header.show"), CMD_SHOW));
		tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.lastModified.i18n(), Cols.lastModified.ordinal()));
		tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.certificate.i18n(), Cols.certificate.ordinal(),
				new DownloadCertificateCellRenderer(assessedIdentity)));
		tableColumnModel.addFlexiColumnModel(new StaticFlexiColumnModel("table.header.launchcourse",
				translate("table.header.launchcourse"), CMD_LAUNCH_COURSE));
		tableColumnModel.addFlexiColumnModel(new StaticFlexiColumnModel("table.header.delete",
				translate("table.action.delete"), CMD_DELETE));
		
		//delete
		EPArtefactHandler<?> artHandler = portfolioModule.getArtefactHandler(EfficiencyStatementArtefact.ARTEFACT_TYPE);
		if(portfolioModule.isEnabled() && artHandler != null && artHandler.isEnabled() && assessedIdentity.equals(getIdentity())) {
			tableColumnModel.addFlexiColumnModel(new StaticFlexiColumnModel("table.header.artefact",
					Cols.efficiencyStatement.ordinal(), CMD_ARTEFACT,
					new StaticFlexiCellRenderer(CMD_ARTEFACT, new AsArtefactCellRenderer())));
		}
		
		tableModel = new CertificateAndEfficiencyStatementListModel(tableColumnModel);
		loadModel();
		tableEl = uifactory.addTableElement(getWindowControl(), "certificates", tableModel, getTranslator(), formLayout);
		tableEl.setElementCssClass("o_sel_certificates_table");
	}
	
	private void loadModel() {
		Map<Long, CertificateAndEfficiencyStatement> resourceKeyToStatments = new HashMap<>();
		List<CertificateAndEfficiencyStatement> statments = new ArrayList<>();
		List<UserEfficiencyStatementLight> efficiencyStatementsList = esm.findEfficiencyStatementsLight(assessedIdentity);
		for(UserEfficiencyStatementLight efficiencyStatement:efficiencyStatementsList) {
			CertificateAndEfficiencyStatement wrapper = new CertificateAndEfficiencyStatement();
			wrapper.setDisplayName(efficiencyStatement.getShortTitle());
			wrapper.setPassed(efficiencyStatement.getPassed());
			wrapper.setScore(efficiencyStatement.getScore());
			wrapper.setEfficiencyStatementKey(efficiencyStatement.getKey());
			wrapper.setResourceKey(efficiencyStatement.getArchivedResourceKey());
			wrapper.setLastModified(efficiencyStatement.getLastModified());
			
			statments.add(wrapper);
			resourceKeyToStatments.put(efficiencyStatement.getArchivedResourceKey(), wrapper);
		}
		
		List<CertificateLight> certificates = certificatesManager.getLastCertificates(assessedIdentity);
		for(CertificateLight certificate:certificates) {
			Long resourceKey = certificate.getOlatResourceKey();
			CertificateAndEfficiencyStatement wrapper = resourceKeyToStatments.get(resourceKey);
			if(wrapper == null) {
				wrapper = new CertificateAndEfficiencyStatement();
				wrapper.setDisplayName(certificate.getCourseTitle());
				resourceKeyToStatments.put(resourceKey, wrapper);
				statments.add(wrapper);
			} else {
				wrapper.setResourceKey(resourceKey);
			}
			if(resourceKey != null && wrapper.getResourceKey() == null) {
				wrapper.setResourceKey(resourceKey);
			}
			wrapper.setCertificate(certificate);
		}
		
		tableModel.setObjects(statments);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}
	
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(tableEl == source) {
			if(event instanceof SelectionEvent) {
				SelectionEvent te = (SelectionEvent) event;
				String cmd = te.getCommand();
				CertificateAndEfficiencyStatement statement = tableModel.getObject(te.getIndex());
				if(CMD_LAUNCH_COURSE.equals(cmd)) {
					doLaunchCourse(ureq, statement.getResourceKey());
				} else if(CMD_DELETE.equals(cmd)) {
					doConfirmDelete(ureq, statement);
				} else if(CMD_SHOW.equals(cmd)) {
					doShowStatement(ureq, statement);
				} else if(CMD_ARTEFACT.equals(cmd)) {
					doCollectArtefact(ureq, statement.getDisplayName(), statement.getEfficiencyStatementKey());
				}
			}
		} else if(coachingToolButton == source) {
			doLaunchCoachingTool(ureq);
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		 if (source == confirmDeleteCtr) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				CertificateAndEfficiencyStatement statement = (CertificateAndEfficiencyStatement)confirmDeleteCtr.getUserObject();
				doDelete(statement.getEfficiencyStatementKey());
			}
		}
	}
	
	private void doShowStatement(UserRequest ureq, CertificateAndEfficiencyStatement statement) {
		RepositoryEntry entry = repositoryService.loadByResourceKey(statement.getResourceKey());
		EfficiencyStatement efficiencyStatment = esm.getUserEfficiencyStatementByKey(statement.getEfficiencyStatementKey());
		CertificateAndEfficiencyStatementController efficiencyCtrl = new CertificateAndEfficiencyStatementController(getWindowControl(), ureq,
				assessedIdentity, null, statement.getResourceKey(), entry, efficiencyStatment, false);
		listenTo(efficiencyCtrl);
		stackPanel.pushController(statement.getDisplayName(), efficiencyCtrl);
	}

	private void doConfirmDelete(UserRequest ureq, CertificateAndEfficiencyStatement statement) {
		RepositoryEntry re = repositoryService.loadByResourceKey(statement.getResourceKey());
		if(re == null) {
			String text = translate("efficiencyStatements.delete.confirm", statement.getDisplayName());
			confirmDeleteCtr = activateYesNoDialog(ureq, null, text, confirmDeleteCtr);
			confirmDeleteCtr.setUserObject(statement);
		} else {
			showWarning("efficiencyStatements.cannot.delete");
		}
	}
	
	private void doDelete(Long efficiencyStatementKey) {
		UserEfficiencyStatementLight efficiencyStatement = esm.getUserEfficiencyStatementLightByKey(efficiencyStatementKey);
		esm.deleteEfficiencyStatement(efficiencyStatement);

		loadModel();
		tableEl.reset();
		showInfo("info.efficiencyStatement.deleted");
	}
	
	private void doLaunchCoachingTool(UserRequest ureq) {
		String businessPath = "[CoachSite:0][search:0][Identity:" + assessedIdentity.getKey() + "]";
		NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
	}

	private void doLaunchCourse(UserRequest ureq, Long resourceKey) {
		RepositoryEntry entry = repositoryService.loadByResourceKey(resourceKey);
		if(entry == null) {
			showWarning("efficiencyStatements.course.noexists");
		} else if (!repositoryManager.isAllowedToLaunch(ureq, entry)) {
			showWarning("efficiencyStatements.course.noaccess");
		} else {
			try {
				String businessPath = "[RepositoryEntry:" + entry.getKey() + "]";
				NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
			} catch (CorruptedCourseException e) {
				logError("Course corrupted: " + entry.getKey() + " (" + entry.getResourceableId() + ")", e);
				showError("cif.error.corrupted");
			}
		}
	}
	
	private void doCollectArtefact(UserRequest ureq, String title, Long efficiencyStatementKey) {
		EPArtefactHandler<?> artHandler = portfolioModule.getArtefactHandler(EfficiencyStatementArtefact.ARTEFACT_TYPE);
		if(artHandler != null && artHandler.isEnabled() && assessedIdentity.equals(getIdentity())) {
			AbstractArtefact artefact = artHandler.createArtefact();
			artefact.setAuthor(getIdentity());//only author can create artefact
			//no business path becouse we cannot launch an efficiency statement
			artefact.setCollectionDate(new Date());
			artefact.setTitle(translate("artefact.title", new String[]{ title }));
			EfficiencyStatement fullStatement = EfficiencyStatementManager.getInstance().getUserEfficiencyStatementByKey(efficiencyStatementKey);
			artHandler.prefillArtefactAccordingToSource(artefact, fullStatement);
			ePFCollCtrl = new ArtefactWizzardStepsController(ureq, getWindowControl(), artefact, (VFSContainer)null);
			listenTo(ePFCollCtrl);
		}
	}

	public class AsArtefactCellRenderer implements FlexiCellRenderer {
		
		@Override
		public void render(Renderer renderer, StringOutput sb, Object cellValue, int row,
				FlexiTableComponent source, URLBuilder ubu, Translator translator) {
			sb.append("<i class='o_icon o_icon-lg o_icon_eportfolio_add'> </i> <span title=\"")
				.append(translate("table.add.as.artefact"))
				.append("\"> </span>");
		}
	}
}