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

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.repository.PropPupForm;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.controllers.EntryChangedEvent;
import org.olat.resource.accesscontrol.ui.AccessConfigurationController;

/**
 * Configuration of the BARG and access control of a learn resource.
 * 
 * 
 * Initial date: 06.05.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AuthoringEditAccessController extends BasicController {
	
	private VelocityContainer editproptabpubVC;
	
	private PropPupForm propPupForm;
	private AccessConfigurationController acCtr;
	
	private RepositoryEntry entry;
	
	public AuthoringEditAccessController(UserRequest ureq, WindowControl wControl, RepositoryEntry entry) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(RepositoryService.class, getLocale(), getTranslator()));
		
		this.entry = entry;
		
		editproptabpubVC = createVelocityContainer("editproptabpub");
		propPupForm = new PropPupForm(ureq, wControl, entry);
		listenTo(propPupForm);
		editproptabpubVC.put("proppupform", propPupForm.getInitialComponent());

		boolean managedBookings = RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.bookings);
		acCtr = new AccessConfigurationController(ureq, getWindowControl(), entry.getOlatResource(), entry.getDisplayname(), true, !managedBookings);
		int access = propPupForm.getAccess();
		int numOfBookingConfigs = acCtr.getNumOfBookingConfigurations();
		if(access == RepositoryEntry.ACC_USERS || access == RepositoryEntry.ACC_USERS_GUESTS) {
			if(!managedBookings || numOfBookingConfigs > 0) {
				editproptabpubVC.put("accesscontrol", acCtr.getInitialComponent());
				editproptabpubVC.contextPut("isGuestAccess", Boolean.valueOf(access == RepositoryEntry.ACC_USERS_GUESTS));
			}
		}
		putInitialPanel(editproptabpubVC);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {	
		if (source == propPupForm) {
			if (event == Event.DONE_EVENT) {
				// inform user about inconsistent configuration: doesn't make sense to set a repositoryEntry canReference=true if it is only accessible to owners
				if (!entry.getCanReference() && propPupForm.canReference() && (propPupForm.getAccess() < RepositoryEntry.ACC_OWNERS_AUTHORS && !propPupForm.isMembersOnly())) {					
					showError("warn.config.reference.no.access");
				}
			
				int access = propPupForm.getAccess();
				int numOfBookingConfigs = acCtr.getNumOfBookingConfigurations();

				entry = RepositoryManager.getInstance().setProperties(entry, propPupForm.canCopy(), propPupForm.canReference(), propPupForm.canDownload());		
				entry = RepositoryManager.getInstance().setAccess(entry, access, propPupForm.isMembersOnly());		

				boolean managedBookings = RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.bookings);
				if(access == RepositoryEntry.ACC_USERS || access == RepositoryEntry.ACC_USERS_GUESTS) {
					if(!managedBookings || numOfBookingConfigs > 0) {
						editproptabpubVC.put("accesscontrol", acCtr.getInitialComponent());
						editproptabpubVC.contextPut("isGuestAccess", Boolean.valueOf(access == RepositoryEntry.ACC_USERS_GUESTS));
					}
				} else {
					editproptabpubVC.remove(acCtr.getInitialComponent());
				}
				
				// inform anybody interrested about this change
				MultiUserEvent modifiedEvent = new EntryChangedEvent(entry, EntryChangedEvent.MODIFIED);
				CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(modifiedEvent, entry);			
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
		}
	}
}