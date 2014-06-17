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
package org.olat.repository.ui.author;

import java.io.File;

import org.olat.NewControllerFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SpacerElement;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.logging.activity.LearningResourceLoggingAction;
import org.olat.core.logging.activity.OlatResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.fileresource.types.ResourceEvaluation;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 30.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ImportRepositoryEntryController extends FormBasicController {
	
	private RepositoryEntry importedEntry;
	private RepositoryHandler handlerForUploadedResource;
	
	private SpacerElement spacerEl;
	private FormSubmit importButton;
	private FileElement uploadFileEl;
	private StaticTextElement typeEl;
	private TextElement displaynameEl;
	private MultipleSelectionElement referencesEl;
	
	private final static String[] refKeys = new String[]{ "checked" };
	
	@Autowired
	private RepositoryHandlerFactory repositoryHandlerFactory;
	
	public ImportRepositoryEntryController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(RepositoryManager.class, getLocale(), getTranslator()));
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		uploadFileEl = uifactory.addFileElement("upload", "import.file", formLayout);
		uploadFileEl.addActionListener(FormEvent.ONCHANGE);
		
		spacerEl = uifactory.addSpacerElement("spacer1", formLayout, false);
		spacerEl.setVisible(false);
		
		typeEl = uifactory.addStaticTextElement("cif.type", "cif.type", "", formLayout);
		typeEl.setVisible(false);
		
		displaynameEl = uifactory.addTextElement("cif.displayname", "cif.displayname", 100, "", formLayout);
		displaynameEl.setDisplaySize(30);
		displaynameEl.setMandatory(true);
		displaynameEl.setVisible(false);
		
		String[] refValues = new String[]{ "" };
		referencesEl = uifactory.addCheckboxesHorizontal("references", "references", formLayout, refKeys, refValues);
		referencesEl.setVisible(false);
		
		FormLayoutContainer buttonContainer = FormLayoutContainer.createButtonLayout("buttonContainer", getTranslator());
		formLayout.add("buttonContainer", buttonContainer);
		buttonContainer.setElementCssClass("o_sel_repo_save_details");
		importButton = uifactory.addFormSubmitButton("cmd.import.ressource", buttonContainer);
		importButton.setEnabled(false);

		uifactory.addFormCancelButton("cancel", buttonContainer, ureq, getWindowControl());
	}
	
	@Override
	protected void doDispose() {
		//
	}
	
	public RepositoryEntry getImportedEntry() {
		return importedEntry;
	}

	public RepositoryHandler getHandler() {
		return handlerForUploadedResource;
	}
	
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(uploadFileEl == source) {
			doAnalyseUpload();
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		if(handlerForUploadedResource != null) {
			doImport();
			fireEvent(ureq, Event.DONE_EVENT);
		}
	}
	
	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		if (!StringHelper.containsNonWhitespace(displaynameEl.getValue())) {
			displaynameEl.setErrorKey("cif.error.displayname.empty", new String[] {});
			allOk = false;
		} else if (displaynameEl.hasError()) {
			allOk = false;
		} else {
			displaynameEl.clearError();
		}

		return allOk & handlerForUploadedResource != null & super.validateFormLogic(ureq);
	}
	
	private void doImport() {
		if(handlerForUploadedResource == null) return;
		
		String displayname = displaynameEl.getValue();
		File uploadedFile = uploadFileEl.getUploadFile();
		String uploadedFilename = uploadFileEl.getUploadFileName();
		boolean withReferences = referencesEl.isAtLeastSelected(1);
		
		importedEntry = handlerForUploadedResource.importResource(getIdentity(), null, displayname,
				"", withReferences, getLocale(), uploadedFile, uploadedFilename);

		ThreadLocalUserActivityLogger.log(LearningResourceLoggingAction.LEARNING_RESOURCE_CREATE, getClass(),
				LoggingResourceable.wrap(importedEntry, OlatResourceableType.genRepoEntry));
	}

	private void doAnalyseUpload() {
		File uploadedFile = uploadFileEl.getUploadFile();
		String uploadedFilename = uploadFileEl.getUploadFileName();
		
		for(String type:repositoryHandlerFactory.getSupportedTypes()) {
			RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(type);
			ResourceEvaluation eval = handler.acceptImport(uploadedFile, uploadedFilename);
			if(eval != null && eval.isValid()) {
				updateResourceInfos(eval, handler);
				break;
			}
		}
	}
	
	private void updateResourceInfos(ResourceEvaluation eval, RepositoryHandler handler) {
		handlerForUploadedResource = handler;
		typeEl.setVisible(true);
		if (handler != null) { // add image and typename code
			String tName = NewControllerFactory.translateResourceableTypeName(handler.getSupportedType(), getLocale());
			typeEl.setValue(tName);
		} else {
			typeEl.setValue(translate("cif.type.na"));
		}
		displaynameEl.setVisible(true);
		displaynameEl.setValue(eval.getDisplayname());
		referencesEl.setVisible(eval.isReferences());
		if(eval.isReferences()) {
			referencesEl.select(refKeys[0], true);
		}
		importButton.setEnabled(handler != null);
	}
}