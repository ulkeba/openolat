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

package org.olat.ldap;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.resource.OresHelper;

public interface LDAPLoginManager {

	public static final OLATResourceable ldapSyncLockOres = OresHelper.createOLATResourceableInstance(LDAPLoginManager.class, 0l);

	public LdapContext bindSystem();

	public Attributes bindUser(String uid, String pwd, LDAPError errors);

	public boolean changePassword(Identity identity, String pwd, LDAPError errors);
	
	public Identity createAndPersistUser(Attributes userAttributes);
	
	public Map<String,String> prepareUserPropertyForSync(Attributes attributes, Identity identity);
	
	public List<Identity> getIdentitysDeletedInLdap(LdapContext ctx);
	
	public Identity findIdentyByLdapAuthentication(String uid, LDAPError errors);
	
	public void syncUser(Map<String,String> olatPropertyMap, Identity identity);
	
	public void deletIdentities(List<Identity> identityList);

	public boolean doBatchSync(LDAPError errors, boolean full);
	
	public Date getLastSyncDate();
	
	public boolean acquireSyncLock();
	
	public void freeSyncLock();
	
	public void doSyncSingleUser(Identity ident);

	public void removeFallBackAuthentications();

	/**
	 * returns true, if the given identity is member of the LDAP-securitygroup
	 * 
	 * @param ident
	 * @return
	 */
	public boolean isIdentityInLDAPSecGroup(Identity ident);
}
