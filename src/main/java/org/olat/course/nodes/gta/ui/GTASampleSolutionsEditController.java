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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.olat.core.commons.editor.htmleditor.HTMLEditorController;
import org.olat.core.commons.editor.htmleditor.WysiwygFactory;
import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.tagged.MetaTagged;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.BooleanCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiColumnModel;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSManager;
import org.olat.course.nodes.GTACourseNode;
import org.olat.course.nodes.gta.model.Solution;
import org.olat.course.nodes.gta.model.SolutionList;
import org.olat.course.nodes.gta.ui.SolutionTableModel.SolCols;
import org.olat.modules.ModuleConfiguration;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 25.02.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class GTASampleSolutionsEditController extends FormBasicController {
	
	private FormLink addSolutionLink, createSolutionLink;
	private SolutionTableModel solutionModel;
	private FlexiTableElement solutionTable;
	
	private CloseableModalController cmc;
	private EditSolutionController addSolutionCtrl;
	private EditSolutionController editSolutionCtrl;
	private NewSolutionController newSolutionCtrl;
	private HTMLEditorController newSolutionEditorCtrl, editSolutionEditorCtrl;
	
	private final SolutionList solutions;
	private final File solutionDir;
	private final VFSContainer solutionContainer;
	
	@Autowired
	private UserManager userManager;
	
	public GTASampleSolutionsEditController(UserRequest ureq, WindowControl wControl,
			ModuleConfiguration config, File solutionDir, VFSContainer solutionContainer) {
		super(ureq, wControl, "edit_solution_list");
		this.solutionDir = solutionDir;
		this.solutionContainer = solutionContainer;
		if(config.get(GTACourseNode.GTASK_SOLUTIONS) == null) {
			solutions = new SolutionList();
			config.set(GTACourseNode.GTASK_SOLUTIONS, solutions);
		} else {
			solutions = (SolutionList)config.get(GTACourseNode.GTASK_SOLUTIONS);
		}

		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {

		addSolutionLink = uifactory.addFormLink("add.solution", formLayout, Link.BUTTON);
		addSolutionLink.setElementCssClass("o_sel_course_gta_add_solution");
		addSolutionLink.setIconLeftCSS("o_icon o_icon_upload");
		createSolutionLink = uifactory.addFormLink("create.solution", formLayout, Link.BUTTON);
		createSolutionLink.setElementCssClass("o_sel_course_gta_create_solution");
		createSolutionLink.setIconLeftCSS("o_icon o_icon_edit");

		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(SolCols.title.i18nKey(), SolCols.title.ordinal()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(SolCols.file.i18nKey(), SolCols.file.ordinal()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(SolCols.author.i18nKey(), SolCols.author.ordinal()));
		columnsModel.addFlexiColumnModel(new StaticFlexiColumnModel("table.header.edit", SolCols.edit.ordinal(), "edit",
				new BooleanCellRenderer(
						new StaticFlexiCellRenderer(translate("edit"), "edit"),
						new StaticFlexiCellRenderer(translate("replace"), "edit"))));

		solutionModel = new SolutionTableModel(columnsModel);
		solutionTable = uifactory.addTableElement(getWindowControl(), "table", solutionModel, getTranslator(), formLayout);
		solutionTable.setExportEnabled(true);
		updateModel();
	}
	
	private void updateModel() {
		List<Solution> solutionList = solutions.getSolutions();
		List<SolutionRow> rows = new ArrayList<>(solutionList.size());
		for(Solution solution:solutionList) {
			String filename = solution.getFilename();
			String author = null;
			
			VFSItem item = solutionContainer.resolve(filename);
			if(item instanceof MetaTagged) {
				MetaInfo metaInfo = ((MetaTagged)item).getMetaInfo();
				if(metaInfo != null && metaInfo.getAuthorIdentityKey() != null) {
					author = userManager.getUserDisplayName(metaInfo.getAuthorIdentityKey());
				}
			}

			rows.add(new SolutionRow(solution, author));
		}
		solutionModel.setObjects(rows);
		solutionTable.reset();
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(addSolutionCtrl == source) {
			if(event == Event.DONE_EVENT) {
				Solution newSolution = addSolutionCtrl.getSolution();
				solutions.getSolutions().add(newSolution);
				fireEvent(ureq, Event.DONE_EVENT);
				updateModel();
			}
			cmc.deactivate();
			cleanUp();
		} else if(editSolutionCtrl == source) {
			if(event == Event.DONE_EVENT) {
				fireEvent(ureq, Event.DONE_EVENT);
				updateModel();
			}
			cmc.deactivate();
			cleanUp();
		} else if(newSolutionCtrl == source) {
			Solution newSolution = newSolutionCtrl.getSolution();
			cmc.deactivate();
			cleanUp();
			
			if(event == Event.DONE_EVENT) {
				solutions.getSolutions().add(newSolution);
				doCreateSolutionEditor(ureq, newSolution);
				updateModel();
			}
		} else if(newSolutionEditorCtrl == source) {
			if(event == Event.DONE_EVENT) {
				updateModel();
				fireEvent(ureq, Event.DONE_EVENT);
			}
			cmc.deactivate();
			cleanUp();
		} else if(editSolutionEditorCtrl == source) {
			cmc.deactivate();
			cleanUp();
		} else if(cmc == source) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(editSolutionCtrl);
		removeAsListenerAndDispose(addSolutionCtrl);
		removeAsListenerAndDispose(cmc);
		editSolutionCtrl = null;
		addSolutionCtrl = null;
		cmc = null;
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(addSolutionLink == source) {
			doAddSolution(ureq);
		} else if(createSolutionLink == source) {
			doCreateSolution(ureq);
		} else if(solutionTable == source) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				SolutionRow row = solutionModel.getObject(se.getIndex());
				if("edit".equals(se.getCommand())) {
					doEdit(ureq, row.getSolution());
				}
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}
	
	private void doEdit(UserRequest ureq, Solution solution) {
		if(solution.getFilename().endsWith(".html")) {
			doEditSolutionEditor(ureq, solution);
		} else {
			doReplace(ureq, solution);
		}
		
	}

	private void doAddSolution(UserRequest ureq) {
		addSolutionCtrl = new EditSolutionController(ureq, getWindowControl(), solutionDir, solutionContainer);
		listenTo(addSolutionCtrl);

		String title = translate("add.solution");
		cmc = new CloseableModalController(getWindowControl(), null, addSolutionCtrl.getInitialComponent(), true, title, false);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doReplace(UserRequest ureq, Solution solution) {
		editSolutionCtrl = new EditSolutionController(ureq, getWindowControl(), solution, solutionDir, solutionContainer);
		listenTo(editSolutionCtrl);

		String title = translate("add.solution");
		cmc = new CloseableModalController(getWindowControl(), null, editSolutionCtrl.getInitialComponent(), true, title, false);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doCreateSolution(UserRequest ureq) {
		newSolutionCtrl = new NewSolutionController(ureq, getWindowControl(), solutionContainer);
		listenTo(newSolutionCtrl);
		
		cmc = new CloseableModalController(getWindowControl(), "close", newSolutionCtrl.getInitialComponent());
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doCreateSolutionEditor(UserRequest ureq, Solution solution) {
		String documentName = solution.getFilename();
		VFSItem item = solutionContainer.resolve(documentName);
		if(item == null) {
			solutionContainer.createChildLeaf(documentName);
		} else {
			documentName = VFSManager.rename(solutionContainer, documentName);
			solutionContainer.createChildLeaf(documentName);
		}

		newSolutionEditorCtrl = WysiwygFactory.createWysiwygController(ureq, getWindowControl(),
				solutionContainer, documentName, "media", true, true);
		newSolutionEditorCtrl.setNewFile(true);
		newSolutionEditorCtrl.setUserObject(solution);
		listenTo(newSolutionEditorCtrl);
		
		cmc = new CloseableModalController(getWindowControl(), "close", newSolutionEditorCtrl.getInitialComponent());
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doEditSolutionEditor(UserRequest ureq, Solution solution) {
		String documentName = solution.getFilename();

		editSolutionEditorCtrl = WysiwygFactory.createWysiwygController(ureq, getWindowControl(),
				solutionContainer, documentName, "media", true, true);
		listenTo(editSolutionEditorCtrl);
		
		cmc = new CloseableModalController(getWindowControl(), "close", editSolutionEditorCtrl.getInitialComponent());
		listenTo(cmc);
		cmc.activate();
	}
}
