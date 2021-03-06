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

package org.olat.resource.accesscontrol.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.group.manager.BusinessGroupDAO;
import org.olat.group.manager.BusinessGroupRelationDAO;
import org.olat.group.model.EnrollState;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryShort;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.manager.RepositoryEntryRelationDAO;
import org.olat.repository.model.RepositoryEntryShortImpl;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.AccessControlModule;
import org.olat.resource.accesscontrol.AccessResult;
import org.olat.resource.accesscontrol.method.AccessMethodHandler;
import org.olat.resource.accesscontrol.model.ACResourceInfo;
import org.olat.resource.accesscontrol.model.ACResourceInfoImpl;
import org.olat.resource.accesscontrol.model.AccessMethod;
import org.olat.resource.accesscontrol.model.AccessTransaction;
import org.olat.resource.accesscontrol.model.BusinessGroupAccess;
import org.olat.resource.accesscontrol.model.OLATResourceAccess;
import org.olat.resource.accesscontrol.model.Offer;
import org.olat.resource.accesscontrol.model.OfferAccess;
import org.olat.resource.accesscontrol.model.Order;
import org.olat.resource.accesscontrol.model.OrderStatus;
import org.olat.resource.accesscontrol.model.PSPTransaction;
import org.olat.resource.accesscontrol.model.ResourceReservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Description:<br>
 * The access control is not intend for security check.
 * 
 * <P>
 * Initial Date:  14 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
@Service("acService")
public class ACFrontendManager implements ACService {
	
	private static final OLog log = Tracing.createLoggerFor(ACFrontendManager.class);
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private AccessControlModule accessModule;
	@Autowired
	private ACOfferDAO accessManager;
	@Autowired
	private ACMethodDAO methodManager;
	@Autowired
	private ACOrderDAO orderManager;
	@Autowired
	private ACReservationDAO reservationDao;
	@Autowired
	private ACTransactionDAO transactionManager;
	@Autowired
	private BusinessGroupDAO businessGroupDao;
	@Autowired
	private BusinessGroupRelationDAO businessGroupRelationDao;
	@Autowired
	private RepositoryEntryRelationDAO repositoryEntryRelationDao;
	@Autowired
	private BusinessGroupService businessGroupService;

	/**
	 * The rule to access the repository entry:<br/>
	 * -No offer, access is free<br/>
	 * -Owners have always access to the resource<br/>
	 * -Tutors have access to the resource<br/>
	 * -Participants have access to the resource<br/>
	 * @param entry
	 * @param forId
	 * @param knowMember give it if already know as a member
	 * @return
	 */
	@Override
	public AccessResult isAccessible(RepositoryEntry entry, Identity forId, Boolean knowMember, boolean allowNonInteractiveAccess) {
		if(!accessModule.isEnabled()) {
			return new AccessResult(true);
		}
		
		boolean member;
		if(knowMember == null) {
			member = repositoryService.isMember(forId, entry);
		} else {
			member = knowMember.booleanValue();
		}
		if(member) {
			return new AccessResult(true);
		}
		
		List<Offer> offers = accessManager.findOfferByResource(entry.getOlatResource(), true, new Date());
		if(offers.isEmpty()) {
			if(methodManager.isValidMethodAvailable(entry.getOlatResource(), null)) {
				//not open for the moment: no valid offer at this date but some methods are defined
				return new AccessResult(false);
			} else {
				return new AccessResult(true);
			}	
		}
		return isAccessible(forId, offers, allowNonInteractiveAccess);
	}
	
	@Override
	public AccessResult isAccessible(RepositoryEntry entry, Identity forId, boolean allowNonInteractiveAccess) {
		if(!accessModule.isEnabled()) {
			return new AccessResult(true);
		}
		
		boolean member = repositoryService.isMember(forId, entry);
		return isAccessible(entry, forId, new Boolean(member), allowNonInteractiveAccess);
	}
	
	/**
	 * 
	 * @param resource
	 * @param atDate 
	 * @return
	 */
	@Override
	public boolean isResourceAccessControled(OLATResource resource, Date atDate) {
		return methodManager.isValidMethodAvailable(resource, atDate);
	}
	
	/**
	 * The rule to access a business group:<br/>
	 * -No offer, access is free<br/>
	 * -Owners have always access to the resource<br/>
	 * -Tutors have access to the resource<br/>
	 * -Participants have access to the resource<br/>
	 * @param group
	 * @param forId
	 * @return
	 */
	@Override
	public AccessResult isAccessible(BusinessGroup group, Identity forId, boolean allowNonInteractiveAccess) {
		if(!accessModule.isEnabled()) {
			return new AccessResult(true);
		}

		List<String> roles = businessGroupRelationDao.getRoles(forId, group);
		if(roles.contains(GroupRoles.coach.name())) {
			return new AccessResult(true);
		}
		if(roles.contains(GroupRoles.participant.name())) {
			return new AccessResult(true);
		}
		
		OLATResource resource = OLATResourceManager.getInstance().findResourceable(group);
		List<Offer> offers = accessManager.findOfferByResource(resource, true, new Date());
		if(offers.isEmpty()) {
			if(methodManager.isValidMethodAvailable(resource, null)) {
				//not open for the moment: no valid offer at this date but some methods are defined
				return new AccessResult(false);
			} else {
				return new AccessResult(true);
			}	
		}
		
		return isAccessible(forId, offers, allowNonInteractiveAccess);
	}
	
	protected AccessResult isAccessible(Identity identity, List<Offer> offers, boolean allowNonInteractiveAccess) {
		List<OfferAccess> offerAccess = methodManager.getOfferAccess(offers, true);
		if(offerAccess.isEmpty()) {
			return new AccessResult(false);
		}
		if(allowNonInteractiveAccess && offerAccess.size() == 1) {
			//is it a method without user interaction as the free access?
			OfferAccess link = offerAccess.get(0);
			if(!link.getMethod().isNeedUserInteraction()) {
				return accessResource(identity, link, null);
			}
		}
		return new AccessResult(false, offerAccess);
	}

	@Override
	public Offer createOffer(OLATResource resource, String resourceName) {
		return accessManager.createOffer(resource, resourceName);
	}

	@Override
	public void deleteOffer(Offer offer) {
		accessManager.deleteOffer(offer);
	}

	@Override
	public List<OLATResourceAccess> filterRepositoryEntriesWithAC(List<RepositoryEntry> repoEntries) {
		if(repoEntries == null || repoEntries.isEmpty()) {
			return Collections.emptyList();
		}
		Set<String> resourceTypes = new HashSet<String>();
		List<Long> resourceKeys = new ArrayList<Long>();
		for(RepositoryEntry entry:repoEntries) {
			OLATResource ores = entry.getOlatResource();
			resourceKeys.add(ores.getKey());
			resourceTypes.add(ores.getResourceableTypeName());
		}
		
		String resourceType = null;
		if(resourceTypes.size() == 1) {
			resourceType = resourceTypes.iterator().next();
		}
		List<OLATResourceAccess> resourceWithOffers = methodManager.getAccessMethodForResources(resourceKeys, resourceType, "BusinessGroup", true, new Date());
		return resourceWithOffers;
	}

	@Override
	public List<OLATResourceAccess> filterResourceWithAC(List<OLATResource> resources) {
		if(resources == null || resources.isEmpty()) {
			return Collections.emptyList();
		}
		Set<String> resourceTypes = new HashSet<String>();
		List<Long> resourceKeys = new ArrayList<Long>();
		for(OLATResource resource:resources) {
			resourceKeys.add(resource.getKey());
			resourceTypes.add(resource.getResourceableTypeName());
		}
		
		String resourceType = null;
		if(resourceTypes.size() == 1) {
			resourceType = resourceTypes.iterator().next();
		}
		List<OLATResourceAccess> resourceWithOffers = methodManager.getAccessMethodForResources(resourceKeys, resourceType, "BusinessGroup", true, new Date());
		return resourceWithOffers;
	}

	@Override
	public Set<Long> filterResourcesWithAC(Collection<Long> resourceKeys) {
		Set<Long> resourceWithOffers = accessManager.filterResourceWithOffer(resourceKeys);
		return resourceWithOffers;
	}

	@Override
	public List<Offer> findOfferByResource(OLATResource resource, boolean valid, Date atDate) {
		return accessManager.findOfferByResource(resource, valid, atDate);
	}

	@Override
	public List<BusinessGroupAccess> getOfferAccessForBusinessGroup(boolean valid, Date atDate) {
		return methodManager.getAccessMethodForBusinessGroup(valid, atDate);
	}
	
	/**
	 * 
	 * @param resourceKeys This parameter is mandatory and must not be empty!
	 */
	@Override
	public List<OLATResourceAccess> getAccessMethodForResources(Collection<Long> resourceKeys, String resourceType, boolean valid, Date atDate) {
		if(resourceKeys == null || resourceKeys.isEmpty()) {
			return new ArrayList<OLATResourceAccess>();
		}
		return methodManager.getAccessMethodForResources(resourceKeys, resourceType, null, valid, atDate);
	}

	/**
	 * Get the list of access methods for a business group that are currently available
	 * @param group 
	 * @param valid 
	 * @param atDate
	 * @return The list of OfferAccess objects that represent available access methods
	 */
	@Override
	public List<OfferAccess> getAccessMethodForBusinessGroup(BusinessGroup group, boolean valid, Date atDate) {
		List<Offer> offers = accessManager.findOfferByResource(group.getResource(), valid, atDate);
		if(offers.isEmpty()) {
			return Collections.<OfferAccess>emptyList();
		}
		List<OfferAccess> offerAccess = methodManager.getOfferAccess(offers, valid);
		if(offerAccess.isEmpty()) {
			return Collections.<OfferAccess>emptyList();
		}
		return offerAccess;
	}

	@Override
	public List<OfferAccess> getOfferAccessByResource(Collection<Long> resourceKeys, boolean valid, Date atDate) {
		return methodManager.getOfferAccessByResource(resourceKeys, valid, atDate);
	}

	@Override
	public void save(Offer offer) {
		accessManager.saveOffer(offer);
	}

	@Override
	public OfferAccess saveOfferAccess(OfferAccess link) {
		accessManager.saveOffer(link.getOffer());
		methodManager.save(link);
		return link;
	}
	
	@Override
	public AccessResult accessResource(Identity identity, OfferAccess link, Object argument) {
		if(link == null || link.getOffer() == null || link.getMethod() == null) {
			log.audit("Access refused (no offer) to: " + link + " for " + identity);
			return new AccessResult(false);
		}
		
		AccessMethodHandler handler = accessModule.getAccessMethodHandler(link.getMethod().getType());
		if(handler == null) {
			log.audit("Access refused (no handler method) to: " + link + " for " + identity);
			return new AccessResult(false);
		}
		
		if(handler.checkArgument(link, argument)) {
			if(allowAccesToResource(identity, link.getOffer())) {
				Order order = orderManager.saveOneClick(identity, link);
				AccessTransaction transaction = transactionManager.createTransaction(order, order.getParts().get(0), link.getMethod());
				transactionManager.save(transaction);
				log.audit("Access granted to: " + link + " for " + identity);
				return new AccessResult(true);
			} else {
				log.audit("Access error to: " + link + " for " + identity);
			}
		} else {
			log.audit("Access refused to: " + link + " for " + identity);
		}
		return new AccessResult(false);
	}
	
	@Override
	public void acceptReservationToResource(Identity identity, ResourceReservation reservation) {
		OLATResource resource = reservation.getResource();
		if("BusinessGroup".equals(resource.getResourceableTypeName())) {
			//it's a reservation for a group
			businessGroupService.acceptPendingParticipation(identity, identity, resource);
		} else {
			repositoryManager.acceptPendingParticipation(identity, identity, resource, reservation);
		}
	}

	@Override
	public void removeReservation(Identity ureqIdentity, Identity identity, ResourceReservation reservation) {
		OLATResource resource = reservation.getResource();
		reservationDao.deleteReservation(reservation);
		if("BusinessGroup".equals(resource.getResourceableTypeName())) {
			dbInstance.commit();//needed to have the right number of participants to calculate upgrade from waiting list
			businessGroupService.cancelPendingParticipation(ureqIdentity, reservation);
		}
	}

	@Override
	public ResourceReservation getReservation(Identity identity, OLATResource resource) {
		return reservationDao.loadReservation(identity, resource);
	}
	
	@Override
	public List<ResourceReservation> getReservations(List<OLATResource> resources) {
		return reservationDao.loadReservations(resources);
	}

	@Override
	public List<ResourceReservation> getReservations(Identity identity) {
		return reservationDao.loadReservations(identity);
	}

	@Override
	public int countReservations(OLATResource resource) {
		return reservationDao.countReservations(resource);
	}

	@Override
	public boolean reserveAccessToResource(final Identity identity, final OfferAccess offer) {
		final OLATResource resource = offer.getOffer().getResource();
		String resourceType = resource.getResourceableTypeName();
		if("BusinessGroup".equals(resourceType)) {
			boolean reserved = false;
			final BusinessGroup group = businessGroupDao.loadForUpdate(resource.getResourceableId());
			if(group.getMaxParticipants() == null && group.getMaxParticipants() <= 0) {
				reserved =  true;//don't need reservation
			} else {
				BusinessGroup reloadedGroup = businessGroupService.loadBusinessGroup(resource);
				ResourceReservation reservation = reservationDao.loadReservation(identity, resource);
				if(reservation != null) {
					reserved = true;
				}
				
				int currentCount = businessGroupService.countMembers(reloadedGroup, GroupRoles.participant.name());
				int reservations = reservationDao.countReservations(resource);
				if(currentCount + reservations < reloadedGroup.getMaxParticipants().intValue()) {
					reservationDao.createReservation(identity, offer.getMethod().getType(), null, resource);
					reserved = true;
				}
			}
			return reserved;
		}
		return true;
	}

	@Override
	public void cleanupReservations() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, -1);
		Date oneHourTimeout = cal.getTime();
		List<ResourceReservation> oldReservations = reservationDao.loadExpiredReservation(oneHourTimeout);
		for(ResourceReservation reservation:oldReservations) {
			log.audit("Remove reservation:" + reservation);
			reservationDao.deleteReservation(reservation);
		}
	}

	@Override
	public boolean allowAccesToResource(final Identity identity, final Offer offer) {
		//check if offer is ok: key is stupid but further check as date, validity...
		if(offer.getKey() == null) {
			return false;
		}
		
		//check the resource
		OLATResource resource = offer.getResource();
		if(resource == null || resource.getKey() == null || resource.getResourceableId() == null || resource.getResourceableTypeName() == null) {
			return false;
		}
		
		String resourceType = resource.getResourceableTypeName();
		if("BusinessGroup".equals(resourceType)) {
			BusinessGroup group = businessGroupService.loadBusinessGroup(resource);
			if(group != null) {
				EnrollState result = businessGroupService.enroll(identity, null, identity, group, null);
				return result.isFailed() ? Boolean.FALSE : Boolean.TRUE;
			}
		} else {
			RepositoryEntry entry = repositoryManager.lookupRepositoryEntry(resource, false);
			if(entry != null) {
				if(!repositoryEntryRelationDao.hasRole(identity, entry, GroupRoles.participant.name())) {
					repositoryEntryRelationDao.addRole(identity, entry, GroupRoles.participant.name());
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean denyAccesToResource(Identity identity, Offer offer) {
		//check if offer is ok: key is stupid but further check as date, validity...
		if(offer.getKey() == null) {
			return false;
		}
		
		//check the resource
		OLATResource resource = offer.getResource();
		if(resource == null || resource.getKey() == null || resource.getResourceableId() == null || resource.getResourceableTypeName() == null) {
			return false;
		}
		
		String resourceType = resource.getResourceableTypeName();
		if("BusinessGroup".equals(resourceType)) {
			BusinessGroup group = businessGroupService.loadBusinessGroup(resource);
			if(group != null) {
				if(businessGroupService.hasRoles(identity, group, GroupRoles.participant.name())) {
					businessGroupRelationDao.removeRole(identity, group, GroupRoles.participant.name());
				}
				return true;
			}
		} else {
			RepositoryEntry entry = repositoryManager.lookupRepositoryEntry(resource, false);
			if(entry != null) {
				if(repositoryEntryRelationDao.hasRole(identity, entry, GroupRoles.participant.name())) {
					repositoryEntryRelationDao.removeRole(identity, entry, GroupRoles.participant.name());
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public String resolveDisplayName(OLATResource resource) {
		String resourceType = resource.getResourceableTypeName();
		if("BusinessGroup".equals(resourceType)) {
			BusinessGroup group = businessGroupService.loadBusinessGroup(resource);
			if(group != null) {
				return group.getName();
			}
		} else {
			RepositoryEntry entry = repositoryManager.lookupRepositoryEntry(resource, false);
			if(entry != null) {
				return entry.getDisplayname();
			}
		}
		return null;
	}
	
	@Override
	public List<ACResourceInfo> getResourceInfos(List<OLATResource> resources) {
		if(resources == null || resources.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<OLATResource> groupResources = new ArrayList<OLATResource>(resources.size());
		List<OLATResource> repositoryResources = new ArrayList<OLATResource>(resources.size());
		for(OLATResource resource:resources) {
			String resourceType = resource.getResourceableTypeName();
			if("BusinessGroup".equals(resourceType)) {
				groupResources.add(resource);
			} else {
				repositoryResources.add(resource);
			}
		}

		List<ACResourceInfo> resourceInfos = new ArrayList<ACResourceInfo>(resources.size());
		if(!groupResources.isEmpty()) {
			List<Long> groupKeys = new ArrayList<Long>(groupResources.size());
			Map<Long, OLATResource> groupMapKeys = new HashMap<Long, OLATResource>(groupResources.size() * 2 + 1);
			for(OLATResource groupResource:groupResources) {
				groupKeys.add(groupResource.getResourceableId());
			}
			List<BusinessGroup> groups = businessGroupService.loadBusinessGroups(groupKeys);
			for(BusinessGroup group:groups) {
				ACResourceInfoImpl info = new ACResourceInfoImpl();
				info.setResource(groupMapKeys.get(group.getKey()));
				info.setName(group.getName());
				info.setDescription(group.getDescription());
				info.setResource(group.getResource());
				resourceInfos.add(info);
			}
		}
		if(!repositoryResources.isEmpty()) {
			List<RepositoryEntryShort> repoEntries = repositoryManager.loadRepositoryEntryShorts(repositoryResources);
			for(RepositoryEntryShort repoEntry:repoEntries) {
				ACResourceInfoImpl info = new ACResourceInfoImpl();
				info.setName(repoEntry.getDisplayname());
				info.setDescription(((RepositoryEntryShortImpl)repoEntry).getDescription());
				info.setResource(((RepositoryEntryShortImpl)repoEntry).getOlatResource());
				resourceInfos.add(info);
			}
		}
		return resourceInfos;
	}

	@Override
	public void enableMethod(Class<? extends AccessMethod> type, boolean enable) {
		methodManager.enableMethod(type, enable);
	}

	@Override
	public List<AccessMethod> getAvailableMethods(Identity identity, Roles roles) {
		return methodManager.getAvailableMethods(identity, roles);
	}

	@Override
	public OfferAccess createOfferAccess(Offer offer, AccessMethod method) {
		return methodManager.createOfferAccess(offer, method);
	}

	@Override
	public void deletedLinkToMethod(OfferAccess link) {
		methodManager.delete(link);
	}

	@Override
	public List<OfferAccess> getOfferAccess(Offer offer, boolean valid) {
		return methodManager.getOfferAccess(offer, valid);
	}

	@Override
	public List<Order> findOrders(Identity delivery, OrderStatus... status) {
		return orderManager.findOrdersByDelivery(delivery, status);
	}

	@Override
	public List<AccessTransaction> findAccessTransactions(List<Order> orders) {
		return transactionManager.loadTransactionsForOrders(orders);
	}

	@Override
	public List<PSPTransaction> findPSPTransactions(List<Order> orders) {
		List<AccessMethodHandler> handlers = accessModule.getMethodHandlers();
		List<PSPTransaction> transactions = new ArrayList<PSPTransaction>();
		for(AccessMethodHandler handler:handlers) {
			transactions.addAll(handler.getPSPTransactions(orders));
		}
		return transactions;
	}

	@Override
	public List<Order> findOrders(OLATResource resource, Identity delivery, Long orderNr, Date from, Date to, OrderStatus... status) {
		return orderManager.findOrders(resource, delivery, orderNr, from, to, status);
	}

	@Override
	public List<Order> findOrders(OLATResource resource, OrderStatus... status) {
		return orderManager.findOrdersByResource(resource, status);
	}
}
