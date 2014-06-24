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
package org.olat.repository.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.TypedQuery;

import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityImpl;
import org.olat.catalog.CatalogEntryImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.commons.services.mark.impl.MarkImpl;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.course.assessment.EfficiencyStatementManager;
import org.olat.course.assessment.UserCourseInformations;
import org.olat.course.assessment.manager.UserCourseInformationsManager;
import org.olat.course.assessment.model.UserEfficiencyStatementImpl;
import org.olat.course.assessment.model.UserEfficiencyStatementLight;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryMyView;
import org.olat.repository.model.RepositoryEntryMyCourseImpl;
import org.olat.repository.model.SearchMyRepositoryEntryViewParams;
import org.olat.repository.model.SearchMyRepositoryEntryViewParams.Filter;
import org.olat.repository.model.SearchMyRepositoryEntryViewParams.OrderBy;
import org.olat.resource.OLATResource;
import org.olat.resource.accesscontrol.model.OfferImpl;
import org.olat.user.UserImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Queries for the view "RepositoryEntryMyCourseView" dedicated to the "My course" feature.
 * The identity is a mandatory parameter.
 * 
 * 
 * Initial date: 12.03.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class RepositoryEntryMyCourseQueries {
	
	private static final OLog log = Tracing.createLoggerFor(RepositoryEntryMyCourseQueries.class);
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private EfficiencyStatementManager efficiencyStatementManager;
	@Autowired
	private UserCourseInformationsManager userCourseInformationsManager;
	
	public int countViews(SearchMyRepositoryEntryViewParams params) {
		if(params.getIdentity() == null) {
			log.error("No identity defined for query");
			return 0;
		}
		
		TypedQuery<Number> query = creatMyViewQuery(params, Number.class);
		Number count = query.getSingleResult();
		return count == null ? 0 : count.intValue();
	}

	public List<RepositoryEntryMyView> searchViews(SearchMyRepositoryEntryViewParams params, int firstResult, int maxResults) {
		if(params.getIdentity() == null) {
			log.error("No identity defined for query");
			return Collections.emptyList();
		}

		TypedQuery<Object[]> query = creatMyViewQuery(params, Object[].class);
		query.setFirstResult(firstResult);
		if(maxResults > 0) {
			query.setMaxResults(maxResults);
		}
		
		List<Long> infosKeys = new ArrayList<>();
		List<Long> effKeys = new ArrayList<>();
		List<Object[]> objects = query.getResultList();
		List<RepositoryEntryMyView> views = new ArrayList<>(objects.size());
		Map<OLATResource,RepositoryEntryMyCourseImpl> viewsMap = new HashMap<>();
		for(Object[] object:objects) {
			RepositoryEntry re = (RepositoryEntry)object[0];
			Number numOfMarks = (Number)object[1];
			boolean hasMarks = numOfMarks == null ? false : numOfMarks.longValue() > 0;
			Number numOffers = (Number)object[2];
			long offers = numOffers == null ? 0l : numOffers.longValue();
			Integer myRating = (Integer)object[3];
			
			RepositoryEntryMyCourseImpl view = new RepositoryEntryMyCourseImpl(re, hasMarks, offers, myRating);
			views.add(view);
			viewsMap.put(re.getOlatResource(), view);
			
			Long infosKey = (Long)object[4];
			if(infosKey != null) {
				infosKeys.add(infosKey);
			}
			Long effKey = (Long)object[5];
			if(effKey != null) {
				effKeys.add(effKey);
			}
		}
		
		if(effKeys.size() > 0) {
			List<UserEfficiencyStatementLight> efficiencyStatements =
					efficiencyStatementManager.findEfficiencyStatementsLight(effKeys);
			for(UserEfficiencyStatementLight efficiencyStatement:efficiencyStatements) {
				if(viewsMap.containsKey(efficiencyStatement.getResource())) {
					viewsMap.get(efficiencyStatement.getResource()).setEfficiencyStatement(efficiencyStatement);
				}
			}
		}
		
		if(infosKeys.size() > 0) {
			List<UserCourseInformations> courseInfos = userCourseInformationsManager.getUserCourseInformations(infosKeys);
			for(UserCourseInformations courseInfo:courseInfos) {
				if(viewsMap.containsKey(courseInfo.getResource())) {
					viewsMap.get(courseInfo.getResource()).setCourseInfos(courseInfo);
				}
			}
		}
		
		return views;
	}

	protected <T> TypedQuery<T> creatMyViewQuery(SearchMyRepositoryEntryViewParams params,
			Class<T> type) {

		Roles roles = params.getRoles();
		Identity identity = params.getIdentity();
		List<String> resourceTypes = params.getResourceTypes();

		boolean count = Number.class.equals(type);
		StringBuilder sb = new StringBuilder();
		
		if(count) {
			sb.append("select count(v.key) ")
			  .append(" from repositoryentry as v, ").append(IdentityImpl.class.getName()).append(" as ident ")
			  .append(" inner join v.olatResource as res")
			  .append(" inner join v.statistics as stats")
			  .append(" left join v.lifecycle as lifecycle ");
		} else {
			sb.append("select v, ")
			  .append(" (select count(mark.key) from ").append(MarkImpl.class.getName()).append(" as mark ")
			  .append("   where mark.creator=ident and mark.resId=v.key and mark.resName='RepositoryEntry'")
			  .append(" ) as marks,")
			  .append(" (select count(offer.key) from ").append(OfferImpl.class.getName()).append(" as offer ")
			  .append("   where offer.resource=res and offer.valid=true")
			  //TODO validity
			  .append(" ) as offers, ")
			  .append(" (select rating.rating from userrating as rating")
			  .append("   where rating.resId=v.key and rating.creator=ident and rating.resName='RepositoryEntry'")
			  .append(" ) as myrating")
			  // user course informations
			  .append(" ,(select infos.key from usercourseinfos as infos")
			  .append("    where infos.resource=res and infos.identity=ident")
			  .append(" ) as infosKey")
			  //efficiency statements
			  .append(" ,(select eff.key from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as eff")
			  .append("    where eff.resource=res and eff.identity=ident")
			  .append(" ) as effKey");
			appendOrderByInSelect(params, sb);
			sb.append(" from repositoryentry as v, ").append(IdentityImpl.class.getName()).append(" as ident ")
			  .append(" inner join fetch v.olatResource as res")
			  .append(" inner join fetch v.statistics as stats")
			  .append(" left join fetch v.lifecycle as lifecycle ");
		}
		//user course informations
		//efficiency statements

		sb.append(" where ident.key=:identityKey and ");
		appendMyViewAccessSubSelect(sb, roles, params.getFilters(), params.isMembershipMandatory());
		if(params.getRepoEntryKeys() != null && params.getRepoEntryKeys().size() > 0) {
			sb.append(" and v.key in (:repoEntryKeys) ");
		}
		
		if(params.getFilters() != null) {
			for(Filter filter:params.getFilters()) {
				appendFiltersInWhereClause(filter, sb);
			}
		}
		
		if(params.getParentEntry() != null) {
			sb.append(" and exists (select cei.parent.key from ").append(CatalogEntryImpl.class.getName()).append(" as cei ")
			  .append("   where cei.parent.key=:parentCeiKey and cei.repositoryEntry.key=v.key")
			  .append(" )");
		}
		if (params.isResourceTypesDefined()) {
			sb.append(" and res.resName in (:resourcetypes)");
		}
		if(params.getMarked() != null && params.getMarked().booleanValue()) {
			sb.append(" and exists (select mark2.key from ").append(MarkImpl.class.getName()).append(" as mark2 ")
			  .append("   where mark2.creator=ident and mark2.resId=v.key and mark2.resName='RepositoryEntry'")
			  .append(" )");
		}
		
		String author = params.getAuthor();
		if (StringHelper.containsNonWhitespace(author)) { // fuzzy author search
			author = PersistenceHelper.makeFuzzyQueryString(author);

			sb.append(" and exists (select rel from repoentrytogroup as rel, bgroup as baseGroup, bgroupmember as membership, ")
			     .append(IdentityImpl.class.getName()).append(" as identity, ").append(UserImpl.class.getName()).append(" as user")
		         .append("    where rel.entry=v and rel.group=baseGroup and membership.group=baseGroup and membership.identity=identity and identity.user=user")
		         .append("      and membership.role='").append(GroupRoles.owner.name()).append("'")
		         .append("      and (");
			PersistenceHelper.appendFuzzyLike(sb, "user.userProperties['firstName']", "author", dbInstance.getDbVendor());
			sb.append(" or ");
			PersistenceHelper.appendFuzzyLike(sb, "user.userProperties['lastName']", "author", dbInstance.getDbVendor());
			sb.append(" or ");
			PersistenceHelper.appendFuzzyLike(sb, "identity.name", "author", dbInstance.getDbVendor());
			sb.append(" ))");
		}

		String text = params.getText();
		if (StringHelper.containsNonWhitespace(text)) {
			//displayName = '%' + displayName.replace('*', '%') + '%';
			//query.append(" and v.displayname like :displayname");
			text = PersistenceHelper.makeFuzzyQueryString(text);
			sb.append(" and (");
			PersistenceHelper.appendFuzzyLike(sb, "v.displayname", "displaytext", dbInstance.getDbVendor());
			sb.append(" or ");
			PersistenceHelper.appendFuzzyLike(sb, "v.description", "displaytext", dbInstance.getDbVendor());
			sb.append(" or ");
			PersistenceHelper.appendFuzzyLike(sb, "v.objectives", "displaytext", dbInstance.getDbVendor());
			sb.append(")");
		}
		
		Long id = null;
		String refs = null;
		if(StringHelper.containsNonWhitespace(params.getIdAndRefs())) {
			refs = params.getIdAndRefs();
			if(StringHelper.isLong(refs)) {
				try {
					id = Long.parseLong(refs);
				} catch (NumberFormatException e) {
					//
				}
			}
			sb.append(" and (v.externalId=:ref or v.externalRef=:ref or v.softkey=:ref");
			if(id != null) {
				sb.append(" or v.key=:vKey or res.resId=:vKey)");
			}
			sb.append(")");	
		}
		
		if(!count) {
			appendOrderBy(params.getOrderBy(), params.isOrderByAsc(), sb);
		}

		TypedQuery<T> dbQuery = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), type);
		if(params.getParentEntry() != null) {
			dbQuery.setParameter("parentCeiKey", params.getParentEntry().getKey());
		}
		if(params.getRepoEntryKeys() != null && params.getRepoEntryKeys().size() > 0) {
			dbQuery.setParameter("repoEntryKeys", params.getRepoEntryKeys());
		}
		if (params.isResourceTypesDefined()) {
			dbQuery.setParameter("resourcetypes", resourceTypes);
		}
		if(params.isLifecycleFilterDefined()) {
			dbQuery.setParameter("now", new Date());
		}
		if(id != null) {
			dbQuery.setParameter("vKey", id);
		}
		if(refs != null) {
			dbQuery.setParameter("ref", refs);
		}
		if(StringHelper.containsNonWhitespace(text)) {
			dbQuery.setParameter("displaytext", text);
		}
		if (StringHelper.containsNonWhitespace(author)) { // fuzzy author search
			dbQuery.setParameter("author", author);
		}
		dbQuery.setParameter("identityKey", identity.getKey());
		return dbQuery;
	}
	
	private void appendMyViewAccessSubSelect(StringBuilder sb, Roles roles, List<Filter> filters, boolean membershipMandatory) {
		sb.append("(v.access >= ");
		if (roles.isAuthor()) {
			sb.append(RepositoryEntry.ACC_OWNERS_AUTHORS);
		} else if (roles.isGuestOnly()) {
			sb.append(RepositoryEntry.ACC_USERS_GUESTS);
		} else {
			sb.append(RepositoryEntry.ACC_USERS);
		}

		StringBuilder inRoles = new StringBuilder();
		if(filters != null && filters.size() > 0) {
			for(Filter filter: filters) {
				if(Filter.asAuthor.equals(filter)) {
					if(inRoles.length() > 0) inRoles.append(",");
					inRoles.append("'").append(GroupRoles.owner.name()).append("'");
				} else if(Filter.asCoach.equals(filter)) {
					if(inRoles.length() > 0) inRoles.append(",");
					inRoles.append("'").append(GroupRoles.coach.name()).append("'");
				} else if (Filter.asParticipant.equals(filter)) {
					if(inRoles.length() > 0) inRoles.append(",");
					inRoles.append("'").append(GroupRoles.participant.name()).append("'");
				}
			}
		}

		//+ membership
		if(roles.isGuestOnly()) {
			sb.append(")");
		} else {
			if(inRoles.length() == 0 && !membershipMandatory) {
				//sub select are very quick
				sb.append(" or (")
				  .append("  v.access=").append(RepositoryEntry.ACC_OWNERS).append(" and v.membersOnly=true")
				  .append("  and exists (select rel from repoentrytogroup as rel, bgroup as baseGroup, bgroupmember as membership")
				  .append("    where rel.entry=v and rel.group=baseGroup and membership.group=baseGroup and membership.identity.key=ident.key")
				  .append("      and membership.role in ('").append(GroupRoles.owner.name()).append("','").append(GroupRoles.coach.name()).append("','").append(GroupRoles.participant.name()).append("')")
				  .append("  )")
				  .append(" )")
				  .append(")");
			} else {
				if(inRoles.length() == 0) {
					inRoles.append("'").append(GroupRoles.owner.name()).append("','").append(GroupRoles.coach.name()).append("','").append(GroupRoles.participant.name()).append("'");
				}
				//make sure that in all case the role is mandatory
				sb.append(" or (v.access=").append(RepositoryEntry.ACC_OWNERS).append(" and v.membersOnly=true))")
				  .append(" and exists (select rel from repoentrytogroup as rel, bgroup as baseGroup, bgroupmember as membership")
				  .append("    where rel.entry=v and rel.group=baseGroup and membership.group=baseGroup and membership.identity.key=ident.key")
				  .append("      and membership.role in (").append(inRoles).append(")")
				  .append(" )");
			}
		}
	}
	
	private void appendFiltersInWhereClause(Filter filter, StringBuilder sb) {
		switch(filter) {
			case currentCourses:
				sb.append(" and lifecycle.validFrom<=:now and lifecycle.validTo>=:now");
				break;
			case upcomingCourses:
				sb.append(" and lifecycle.validFrom>=:now");
				break;
			case oldCourses:
				sb.append(" and lifecycle.validTo<=:now");
				break;
			case passed:
				sb.append(" and exists (select eff2.key from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as eff2")
				  .append("    where eff2.resource=res and eff2.identity=ident and eff2.passed=true")
				  .append(" )");
				break;
			case notPassed:
				sb.append(" and exists (select eff3.key from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as eff3")
				  .append("    where eff3.resource=res and eff3.identity=ident and eff3.passed=false")
				  .append(" )");
				break;
			case withoutPassedInfos:
				sb.append(" and exists (select eff4.key from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as eff4")
				  .append("    where eff4.resource=res and eff4.identity=ident and eff4.passed is null")
				  .append(" )");
				break;
			default: //do nothing
		}
	}
	

	private void appendOrderByInSelect(SearchMyRepositoryEntryViewParams params, StringBuilder sb) {
		OrderBy orderBy = params.getOrderBy();
		if(orderBy != null) {
			switch(orderBy) {
				case lastVisited:
					sb.append(" ,(select infos2.recentLaunch from usercourseinfos as infos2")
					  .append("    where infos2.resource=res and infos2.identity=ident")
					  .append(" ) as recentLaunch");
					break;
				case passed:
					sb.append(" ,(select eff3.passed from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as eff3")
					  .append("    where eff3.resource=res and eff3.identity=ident")
					  .append(" ) as passed");
					break;
				case score:
					sb.append(" ,(select eff4.score from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as eff4")
					  .append("    where eff4.resource=res and eff4.identity=ident")
					  .append(" ) as score");
					break;
				default: //do nothing
			}
		}
	}
	
	private void appendOrderBy(OrderBy orderBy, boolean asc, StringBuilder sb) {
		if(orderBy != null) {
			switch(orderBy) {
				case automatic:
					sb.append(" order by lower(v.displayname)");
					appendAsc(sb, asc);
					break;
				case favorit:
					if(asc) {
						sb.append(" order by marks asc, lower(v.displayname) asc");
					} else {
						sb.append(" order by marks desc, lower(v.displayname) desc");
					}
					break;
				case lastVisited:
					sb.append(" order by recentLaunch ");
					appendAsc(sb, asc).append(" nulls last, lower(v.displayname) asc");
					break;
				case passed:
					sb.append(" order by passed ");
					appendAsc(sb, asc).append(" nulls last, lower(v.displayname) asc");
					break;
				case score:
					sb.append(" order by score ");
					appendAsc(sb, asc).append(" nulls last, lower(v.displayname) asc");
					break;
				case title:
					sb.append(" order by lower(v.displayname)");
					appendAsc(sb, asc);
					break;
				case lifecycle:
					sb.append(" order by lifecycle.validFrom ");
					appendAsc(sb, asc).append(" nulls last, lower(v.displayname) asc");
					break;
				case author:
					sb.append(" order by lower(v.authors)");
					appendAsc(sb, asc).append(" nulls last");
					break;
				case creationDate:
					sb.append(" order by v.creationDate ");
					appendAsc(sb, asc).append(", lower(v.displayname) asc");
					break;
				case lastModified:
					sb.append(" order by v.lastModified ");
					appendAsc(sb, asc).append(", lower(v.displayname) asc");
					break;
				case rating:
					sb.append(" order by v.statistics.rating ");
					appendAsc(sb, asc).append(" nulls last, lower(v.displayname) asc");
					break;	
				default:
					sb.append(" order by lower(v.displayname)");
					appendAsc(sb, asc);
					break;
			}
		}
	}
	
	private final StringBuilder appendAsc(StringBuilder sb, boolean asc) {
		if(asc) {
			sb.append(" asc");
		} else {
			sb.append(" desc");
		}
		return sb;
	}
	
	/*
	protected <T> TypedQuery<T> creatMyViewQuery(SearchMyRepositoryEntryViewParams params,
			Class<T> type) {

		Roles roles = params.getRoles();
		Identity identity = params.getIdentity();
		List<String> resourceTypes = params.getResourceTypes();

		boolean count = Number.class.equals(type);
		StringBuilder sb = new StringBuilder();
		
		String entity = "repositoryentrymy";
		if(params.getMarked() != null && params.getMarked().booleanValue()) {
			entity = "repositoryentrymy";
		}
		
		if(count) {
			sb.append("select count(v.key) from ").append(entity).append(" as v ")
			  .append(" inner join v.olatResource as res")
			  .append(" left join v.lifecycle as lifecycle");
		} else {
			sb.append("select v from ").append(entity).append(" as v ")
			  .append(" inner join fetch v.olatResource as res")
			  .append(" left join fetch v.lifecycle as lifecycle");
		}

		sb.append(" where v.identityKey=:identityKey and ");
		appendMyViewAccessSubSelect(sb, roles, params.getFilters(), params.isMembershipMandatory());
		if(params.getRepoEntryKeys() != null && params.getRepoEntryKeys().size() > 0) {
			sb.append(" and v.key in (:repoEntryKeys) ");
		}
		
		if(params.getFilters() != null) {
			for(Filter filter:params.getFilters()) {
				appendMyViewFilters(filter, sb);
			}
		}
		
		if(params.getParentEntry() != null) {
			sb.append(" and exists (select cei.parent.key from ").append(CatalogEntryImpl.class.getName()).append(" as cei ")
			  .append("   where cei.parent.key=:parentCeiKey and cei.repositoryEntry.key=v.key")
			  .append(" )");
		}
		if (params.isResourceTypesDefined()) {
			sb.append(" and res.resName in (:resourcetypes)");
		}
		if(params.getMarked() != null) {
			sb.append(" and v.markKey ").append(params.getMarked().booleanValue() ? " is not null " : " is null ");
		}
		
		if(!count) {
			appendMyViewOrderBy(params.getOrderBy(), params.isOrderByAsc(), sb);
		}

		TypedQuery<T> dbQuery = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), type);
		if(params.getParentEntry() != null) {
			dbQuery.setParameter("parentCeiKey", params.getParentEntry().getKey());
		}
		if(params.getRepoEntryKeys() != null && params.getRepoEntryKeys().size() > 0) {
			dbQuery.setParameter("repoEntryKeys", params.getRepoEntryKeys());
		}
		if (params.isResourceTypesDefined()) {
			dbQuery.setParameter("resourcetypes", resourceTypes);
		}
		if(params.isLifecycleFilterDefined()) {
			dbQuery.setParameter("now", new Date());
		}
		dbQuery.setParameter("identityKey", identity.getKey());
		return dbQuery;
	}*/
}