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
package org.olat.course.assessment.manager;

import static org.olat.core.commons.persistence.PersistenceHelper.appendAnd;
import static org.olat.core.commons.persistence.PersistenceHelper.appendFuzzyLike;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityRef;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.util.StringHelper;
import org.olat.course.assessment.AssessmentMode;
import org.olat.course.assessment.AssessmentMode.Status;
import org.olat.course.assessment.model.AssessmentModeImpl;
import org.olat.course.assessment.model.SearchAssessmentModeParams;
import org.olat.group.BusinessGroupImpl;
import org.olat.group.BusinessGroupRef;
import org.olat.group.area.BGtoAreaRelationImpl;
import org.olat.repository.RepositoryEntryRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 20.04.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class AssessmentModeDAO {
	
	@Autowired
	private DB dbInstance;
	
	public AssessmentMode getAssessmentModeById(Long key) {
		List<AssessmentMode> modes = dbInstance.getCurrentEntityManager()
			.createNamedQuery("assessmentModeById", AssessmentMode.class)
			.setParameter("modeKey", key)
			.getResultList();
		
		return modes == null || modes.isEmpty() ? null : modes.get(0);
	}
	
	public List<AssessmentMode> findAssessmentMode(SearchAssessmentModeParams params) {
		StringBuilder sb = new StringBuilder();
		sb.append("select mode from courseassessmentmode mode")
		  .append(" inner join fetch mode.repositoryEntry v")
		  .append(" inner join fetch v.olatResource res");
		
		boolean where = false;
		
		Date date = params.getDate();
		if(date != null) {
			where = appendAnd(sb, where);
			sb.append(":date between mode.beginWithLeadTime and mode.endWithFollowupTime");
		}
		
		String name = params.getName();
		if(StringHelper.containsNonWhitespace(name)) {
			name = PersistenceHelper.makeFuzzyQueryString(name);
			where = appendAnd(sb, where);
			sb.append("(");
			appendFuzzyLike(sb, "v.displayname", "name", dbInstance.getDbVendor());
			sb.append(" or ");
			appendFuzzyLike(sb, "mode.name", "name", dbInstance.getDbVendor());
			sb.append(")");
		}
		
		Long id = null;
		String refs = null;
		String fuzzyRefs = null;
		if(StringHelper.containsNonWhitespace(params.getIdAndRefs())) {
			refs = params.getIdAndRefs();
			fuzzyRefs = PersistenceHelper.makeFuzzyQueryString(refs);
			where = appendAnd(sb, where);
			sb.append(" (v.externalId=:ref or ");
			PersistenceHelper.appendFuzzyLike(sb, "v.externalRef", "fuzzyRefs", dbInstance.getDbVendor());
			sb.append(" or v.softkey=:ref");
			if(StringHelper.isLong(refs)) {
				try {
					id = Long.parseLong(refs);
					sb.append(" or v.key=:vKey or res.resId=:vKey");
				} catch (NumberFormatException e) {
					//
				}
			}
			sb.append(")");	
		}
		
		sb.append(" order by mode.beginWithLeadTime desc ");

		TypedQuery<AssessmentMode> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), AssessmentMode.class);
		if(StringHelper.containsNonWhitespace(params.getName())) {
			query.setParameter("name", name);
		}
		if(id != null) {
			query.setParameter("vKey", id);
		}
		if(refs != null) {
			query.setParameter("ref", refs);
		}
		if(fuzzyRefs != null) {
			query.setParameter("fuzzyRefs", fuzzyRefs);
		}
		if(date != null) {
			query.setParameter("date", date, TemporalType.TIMESTAMP);
		}
		return query.getResultList();
	}
	
	public List<AssessmentMode> getAssessmentModeFor(RepositoryEntryRef entry) {
		return dbInstance.getCurrentEntityManager()
				.createNamedQuery("assessmentModeByRepoEntry", AssessmentMode.class)
				.setParameter("entryKey", entry.getKey())
				.getResultList();
	}
	
	public List<AssessmentMode> getAssessmentModes(Date now) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		
		StringBuilder sb = new StringBuilder();
		sb.append("select mode from courseassessmentmode mode where ")
		  .append(" (mode.beginWithLeadTime<=:now and mode.endWithFollowupTime>=:now and mode.manualBeginEnd=false)")
		  .append(" or mode.statusString in ('").append(Status.leadtime.name()).append("','")
		  .append(Status.assessment.name()).append("','").append(Status.followup.name()).append("')");

		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), AssessmentMode.class)
				.setParameter("now", now)
				.getResultList();
	}
	
	public boolean isInAssessmentMode(RepositoryEntryRef entry, Date date) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		
		StringBuilder sb = new StringBuilder();
		sb.append("select count(mode) from courseassessmentmode mode where ")
		  .append(" mode.repositoryEntry.key=:repoKey and (")
		  .append(" (mode.beginWithLeadTime<=:now and mode.endWithFollowupTime>=:now and mode.manualBeginEnd=false)")
		  .append(" or mode.statusString in ('").append(Status.leadtime.name()).append("','")
		  .append(Status.assessment.name()).append("','").append(Status.followup.name()).append("'))");

		List<Number> count = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("now", date)
				.setParameter("repoKey", entry.getKey())
				.getResultList();
		return count != null && count.size() > 0 && count.get(0).intValue() > 0;
	}
	
	protected List<AssessmentMode> loadAssessmentModeFor(IdentityRef identity, List<AssessmentMode> currentModes) {
		StringBuilder sb = new StringBuilder(1500);
		sb.append("select mode from courseassessmentmode mode ")
		  .append(" inner join fetch mode.repositoryEntry entry")
		  .append(" left join mode.groups as modeToGroup")
		  .append(" left join mode.areas as modeToArea")
		  .append(" where mode.key in (:modeKeys)")
		  .append("  and ((mode.targetAudienceString in ('").append(AssessmentMode.Target.courseAndGroups.name()).append("','").append(AssessmentMode.Target.groups.name()).append("')")
		  .append("   and (exists (select businessGroup from ").append(BusinessGroupImpl.class.getName()).append(" as businessGroup, bgroupmember as membership")
		  .append("     where modeToGroup.businessGroup=businessGroup and membership.group=businessGroup.baseGroup and membership.identity.key=:identityKey")
		  .append("     and (membership.role='").append(GroupRoles.participant.name()).append("' or ")
		  .append("       (mode.applySettingsForCoach=true and membership.role='").append(GroupRoles.coach.name()).append("'))")
		  .append("   ) or exists (select areaToGroup from ").append(BGtoAreaRelationImpl.class.getName()).append(" as areaToGroup,").append(BusinessGroupImpl.class.getName()).append(" as businessGroupArea, bgroupmember as membership")
		  .append("     where modeToArea.area=areaToGroup.groupArea and areaToGroup.businessGroup=businessGroupArea and membership.group=businessGroupArea.baseGroup and membership.identity.key=:identityKey")
		  .append("     and (membership.role='").append(GroupRoles.participant.name()).append("' or ")
		  .append("       (mode.applySettingsForCoach=true and membership.role='").append(GroupRoles.coach.name()).append("'))")
		  .append("  ))) or (mode.targetAudienceString in ('").append(AssessmentMode.Target.courseAndGroups.name()).append("','").append(AssessmentMode.Target.course.name()).append("')")
		  .append("   and exists (select rel from repoentrytogroup as rel,  bgroupmember as membership ")
		  .append("     where mode.repositoryEntry=rel.entry and membership.group=rel.group and rel.defaultGroup=true and membership.identity.key=:identityKey")
		  .append("     and (membership.role='").append(GroupRoles.participant.name()).append("' or ")
		  .append("       (mode.applySettingsForCoach=true and membership.role='").append(GroupRoles.coach.name()).append("'))")
		  .append("  ))")
		  .append(" )");

		List<Long> modeKeys = new ArrayList<>(currentModes.size());
		for(AssessmentMode mode:currentModes) {
			modeKeys.add(mode.getKey());
		}
		List<AssessmentMode> modeList = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), AssessmentMode.class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("modeKeys", modeKeys)
				.getResultList();
		//quicker than distinct
		return new ArrayList<AssessmentMode>(new HashSet<AssessmentMode>(modeList));
	}
	
	public void delete(AssessmentMode assessmentMode) {
		AssessmentModeImpl refMode = dbInstance.getCurrentEntityManager()
				.getReference(AssessmentModeImpl.class, assessmentMode.getKey());
		dbInstance.getCurrentEntityManager().remove(refMode);
	}
	
	/**
	 * Delete all assessment modes of a course.
	 * 
	 * @param entry The course
	 */
	public void delete(RepositoryEntryRef entry) {
		for(AssessmentMode mode: getAssessmentModeFor(entry)) {
			delete(mode);
		}
	}
	
	public void deleteAssessmentModesToGroup(BusinessGroupRef businessGroup) {
		String q = "delete from courseassessmentmodetogroup as modegrrel where modegrrel.businessGroup.key=:groupKey";
		dbInstance.getCurrentEntityManager().createQuery(q)
			.setParameter("groupKey", businessGroup.getKey())
			.executeUpdate();
	}
	
	/**
	 * Delete the relations between assessment mode and group for the specified business group and course.
	 * @param businessGroup
	 * @param entry
	 */
	public void delete(BusinessGroupRef businessGroup, RepositoryEntryRef entry) {
		String q = "delete from courseassessmentmodetogroup as modegrrel where modegrrel.businessGroup.key=:groupKey and modegrrel.assessmentMode.key in (select amode.key from courseassessmentmode amode where amode.repositoryEntry.key=:repoKey)";
		dbInstance.getCurrentEntityManager().createQuery(q)
			.setParameter("groupKey", businessGroup.getKey())
			.setParameter("repoKey", entry.getKey())
			.executeUpdate();
	}

}