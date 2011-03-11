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
* <p>
*/ 

package org.olat.course;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.Formatter;
import org.olat.core.util.servlets.WebDAVProvider;
import org.olat.core.util.vfs.MergeSource;
import org.olat.core.util.vfs.NamedContainerImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.groupsandrights.CourseRights;
import org.olat.group.context.BGContext;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.group.right.BGRightManager;
import org.olat.group.right.BGRightManagerImpl;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
/**
 * 
 * Description:<br>
 * TODO: guido Class Description for CoursefolderWebDAVProvider
 */
public class CoursefolderWebDAVProvider implements WebDAVProvider {

	private static final String MOUNTPOINT = "coursefolders";

	public String getMountPoint() { return MOUNTPOINT; }

	public VFSContainer getContainer(Identity identity) {
		MergeSource cfRoot = new MergeSource(null, null);
		RepositoryManager rm = RepositoryManager.getInstance();
		List<RepositoryEntry> courseEntries = rm.queryByOwner(identity, CourseModule.getCourseTypeName());
		
		Set<Long> smashDuplicates = new HashSet<Long>();
		for(RepositoryEntry courseEntry:courseEntries) {
			smashDuplicates.add(courseEntry.getKey());
		}
		
		BGContextManager bgContextManager = BGContextManagerImpl.getInstance();
		BGRightManager bgRightManager = BGRightManagerImpl.getInstance();
		List<BGContext> contexts = bgContextManager.findBGContextsForIdentity(identity, true, true);
		for(BGContext context:contexts) {
			if(bgRightManager.hasBGRight(CourseRights.RIGHT_COURSEEDITOR, identity, context)) {
				List<RepositoryEntry> entries = bgContextManager.findRepositoryEntriesForBGContext(context);
				for(RepositoryEntry entry:entries) {
					if(!smashDuplicates.contains(entry.getKey())) {
						courseEntries.add(entry);
						smashDuplicates.add(entry.getKey());
					}
				}
			}
		}
		
		for (RepositoryEntry re:courseEntries) {
			OLATResourceable res = re.getOlatResource();
			ICourse course = CourseFactory.loadCourse(res.getResourceableId());
			VFSContainer courseFolder = course.getCourseFolderContainer();
			String courseTitle = Formatter.makeStringFilesystemSave(course.getCourseTitle());
			NamedContainerImpl cfContainer = new NamedContainerImpl(courseTitle, courseFolder);
			cfRoot.addContainer(cfContainer);
		}
		return cfRoot;
	}

}
