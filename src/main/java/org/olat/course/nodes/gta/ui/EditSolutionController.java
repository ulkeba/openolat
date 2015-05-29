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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.course.nodes.gta.model.Solution;


/**
 * 
 * Initial date: 24.02.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class EditSolutionController extends FormBasicController {
	
	private TextElement titleEl;
	private FileElement fileEl;
	
	private final boolean replaceFile;
	private final Solution solution;
	private final File solutionContainer;
	
	public EditSolutionController(UserRequest ureq, WindowControl wControl, File solutionContainer) {
		this(ureq, wControl, new Solution(), solutionContainer, true);
	}
	
	public EditSolutionController(UserRequest ureq, WindowControl wControl, Solution solution, File solutionContainer) {
		this(ureq, wControl, solution, solutionContainer, false);
	}
	
	public EditSolutionController(UserRequest ureq, WindowControl wControl,
			Solution solution, File solutionContainer, boolean replaceFile) {
		super(ureq, wControl);
		this.replaceFile = replaceFile;
		this.solution = solution;
		this.solutionContainer = solutionContainer;
		initForm(ureq);
	}
	
	public Solution getSolution() {
		return solution;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		formLayout.setElementCssClass("o_sel_course_gta_upload_solution_form");
		
		String title = solution.getTitle() == null ? "" : solution.getTitle();
		titleEl = uifactory.addTextElement("title", "solution.title", 128, title, formLayout);
		titleEl.setElementCssClass("o_sel_course_gta_upload_solution_title");
		titleEl.setMandatory(true);

		fileEl = uifactory.addFileElement("file", "solution.file", formLayout);
		fileEl.setMandatory(true);
		fileEl.addActionListener(FormEvent.ONCHANGE);
		if(StringHelper.containsNonWhitespace(solution.getFilename())) {
			File currentFile = new File(solutionContainer, solution.getFilename());
			if(currentFile.exists()) {
				fileEl.setInitialFile(currentFile);
			}
		}
		
		FormLayoutContainer buttonCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		buttonCont.setRootForm(mainForm);
		formLayout.add(buttonCont);
		uifactory.addFormSubmitButton("save", buttonCont);
		uifactory.addFormCancelButton("cancel", buttonCont, ureq, getWindowControl());
	}

	@Override
	protected void doDispose() {
		//
	}
	
	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		titleEl.clearError();
		if(!StringHelper.containsNonWhitespace(titleEl.getValue())) {
			titleEl.setErrorKey("form.mandatory.hover", null);
			allOk &= false;
		}
		
		fileEl.clearError();
		if(fileEl.getInitialFile() == null && fileEl.getUploadFile() == null) {
			fileEl.setErrorKey("form.mandatory.hover", null);
			allOk &= false;
		}
		
		return allOk & super.validateFormLogic(ureq);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		solution.setTitle(titleEl.getValue());
		
		if(fileEl.getUploadFile() != null) {
			if(replaceFile && StringHelper.containsNonWhitespace(solution.getFilename())) {
				File currentFile = new File(solutionContainer, solution.getFilename());
				if(currentFile.exists()) {
					currentFile.delete();
				}
			}
			
			String filename = fileEl.getUploadFileName();
			solution.setFilename(filename);
			
			try {
				Path upload = fileEl.getUploadFile().toPath();
				File newFile = new File(solutionContainer, filename);
				Files.move(upload, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch(Exception ex) {
				logError("", ex);
			}
		}
		
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
}