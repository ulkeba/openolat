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

package org.olat.resource.references;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.persistence.DB;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceImpl;
import org.olat.resource.OLATResourceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Initial Date:  May 27, 2004
 *
 * @author Mike Stock
 * 
 * Comment:  
 * 
 */
@Service("referenceManager")
public class ReferenceManager {
	
	private static final OLog log = Tracing.createLoggerFor(ReferenceManager.class);
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private OLATResourceManager olatResourceManager;

	/**
	 * Add a new reference. The meaning of source and target is
	 * such as the source references the target.
	 * 
	 * @param source
	 * @param target
	 * @param userdata
	 */
	public void addReference(OLATResourceable source, OLATResourceable target, String userdata) {
		OLATResourceImpl sourceImpl = (OLATResourceImpl)olatResourceManager.findResourceable(source);
		OLATResourceImpl targetImpl = (OLATResourceImpl)olatResourceManager.findResourceable(target);
		ReferenceImpl ref = new ReferenceImpl();
		ref.setSource(sourceImpl);
		ref.setTarget(targetImpl);
		ref.setUserdata(userdata);
		ref.setCreationDate(new Date());
		dbInstance.getCurrentEntityManager().persist(ref);
	}

	/**
	 * List all references the source holds.
	 * 
	 * @param source
	 * @return List of renerences.
	 */
	public List<Reference> getReferences(OLATResourceable source) {
		Long sourceKey = getResourceKey(source);
		if (sourceKey == null) {
			return new ArrayList<Reference> (0);
		}
		return dbInstance.getCurrentEntityManager()
				.createNamedQuery("referencesBySourceId", Reference.class)
				.setParameter("sourceKey", sourceKey)
				.getResultList();
	}
	
	/**
	 * List all sources which hold references to the target.
	 * 
	 * @param target
	 * @return List of references.
	 */
	public List<Reference> getReferencesTo(OLATResourceable target) {
		Long targetKey = getResourceKey(target);
		if (targetKey == null) {
			return new ArrayList<Reference>(0);
		}
		return dbInstance.getCurrentEntityManager()
				.createNamedQuery("referencesByTargetId", Reference.class)
				.setParameter("targetKey", targetKey)
				.getResultList();
	}
	
	public List<ReferenceInfos> getReferencesInfos(List<RepositoryEntry> res, Identity identity, Roles roles) {
		if(res == null || res.isEmpty()) return Collections.emptyList();
		
		List<Long> sourceKeys = new ArrayList<>();
		for(RepositoryEntry re:res) {
			sourceKeys.add(re.getOlatResource().getKey());
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("select ref from references ref")
		  .append(" where ref.target.key in (select orig.target.key from references orig where orig.source.key in (:sourceKeys))");
		List<Reference> references = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Reference.class)
				.setParameter("sourceKeys", sourceKeys)
				.getResultList();
		
		Set<Long> targetResourceKeys = new HashSet<>();
		Set<Long> notOrphansResourceKeys = new HashSet<>();
		for(Iterator<Reference> itRef = references.iterator(); itRef.hasNext(); ) {
			Reference reference = itRef.next();
			OLATResource source = reference.getSource();
			OLATResource target = reference.getTarget();
			targetResourceKeys.add(target.getKey());
			if(!sourceKeys.contains(source.getKey())) {
				notOrphansResourceKeys.add(target.getKey());
			}
		}
		
		boolean isOlatAdmin = roles.isOLATAdmin();
		
		List<RepositoryEntry> entries = repositoryService.loadByResourceKeys(targetResourceKeys);
		List<ReferenceInfos> infos = new ArrayList<>(entries.size());
		for(RepositoryEntry entry:entries) {
			Long resourceKey = entry.getOlatResource().getKey();
			boolean notOrphan = notOrphansResourceKeys.contains(resourceKey);
			
			boolean deleteManaged = RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.delete);

			boolean isInstitutionalResourceManager = !roles.isGuestOnly()
						&& repositoryManager.isInstitutionalRessourceManagerFor(identity, roles, entry);
			boolean isOwner = isOlatAdmin || repositoryService.hasRole(identity, entry, GroupRoles.owner.name())
						|| isInstitutionalResourceManager;

			ReferenceInfos refInfos = new ReferenceInfos(entry, !notOrphan, isOwner, deleteManaged);
			infos.add(refInfos);
		}
		return infos;
	}
	
	/**
	 * find the references source -> target -> source
	 * 
	 * 
	 * @param source
	 * @return
	 */
	public List<Reference> getReferencesOfReferences(OLATResource source) {
		StringBuilder sb = new StringBuilder();
		sb.append("select ref from references ref")
		  .append(" where ref.source.key != :sourceKey and ref.target.key in (select orig.target.key from references orig where orig.source.key=:sourceKey)");
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Reference.class)
				.setParameter("sourceKey", source.getKey())
				.getResultList();
	}
	
	private Long getResourceKey(OLATResourceable resource) {
		Long sourceKey;
		if(resource instanceof OLATResource) {
			sourceKey = ((OLATResource)resource).getKey();
		} else {
			OLATResource sourceImpl = olatResourceManager.findResourceable(resource);
			if (sourceImpl == null) {
				sourceKey =  null;
			} else {
				sourceKey = sourceImpl.getKey();
			}
		}
		return sourceKey;
	}
	
	/**
	 * Get an HTML summary of existing references or null if no references exist.
	 * @param target
	 * @param locale
	 * @return HTML fragment or null if no references exist.
	 */
	public String getReferencesToSummary(OLATResourceable target, Locale locale) {
		Translator translator = Util.createPackageTranslator(this.getClass(), locale);
		StringBuilder result = new StringBuilder(100);
		List<Reference> refs = getReferencesTo(target);
		if (refs.size() == 0) return null;
		for (Reference ref:refs) {
			OLATResource source = ref.getSource();
			// special treatment for referenced courses: find out the course title
			if (source.getResourceableTypeName().equals(CourseModule.getCourseTypeName())) {
				try {
					ICourse course = CourseFactory.loadCourse(source);
					result.append(translator.translate("ref.course", new String[] { StringHelper.escapeHtml(course.getCourseTitle()) }));
				} catch (Exception e) {
					log.error("", e);
					result.append(translator.translate("ref.course", new String[] { "<strike>" + source.getKey().toString() + "</strike>" }));
				}
			} else {
				result.append(translator.translate("ref.generic", new String[] { source.getKey().toString() }));
			}
			result.append("<br />");
		}
		return result.toString();
	}
	
	public List<String> getReferencesToSummary(OLATResourceable target) {
		List<Reference> refs = getReferencesTo(target);
		List<String> refNames = new ArrayList<>(refs.size());
		if (refs.size() > 0) {
			for (Reference ref:refs) {
				OLATResource source = ref.getSource();
				// special treatment for referenced courses: find out the course title
				if (source.getResourceableTypeName().equals(CourseModule.getCourseTypeName())) {
					try {
						ICourse course = CourseFactory.loadCourse(source);
						refNames.add(StringHelper.escapeHtml(course.getCourseTitle()));
					} catch (Exception e) {
						log.error("", e);
						refNames.add("<strike>" + source.getKey().toString() + "</strike>");
					}
				} else {
					refNames.add(source.getKey().toString());
				}
			}
		}
		return refNames;
		
	}
	
	/**
	 * @param ref
	 */
	public void delete(Reference ref) {
		ReferenceImpl reloadedRef = dbInstance.getCurrentEntityManager()
				.getReference(ReferenceImpl.class, ref.getKey());
		dbInstance.getCurrentEntityManager().remove(reloadedRef);
	}

	/**
	 * Only for cleanup : Delete all references of an OLAT-resource.
	 * @param olatResource  an OLAT-Resource
	 */
	public void deleteAllReferencesOf(OLATResource olatResource) {
		List<Reference> references = getReferences(olatResource);
		for (Reference ref:references) {
			delete(ref);
		}
	}
}
