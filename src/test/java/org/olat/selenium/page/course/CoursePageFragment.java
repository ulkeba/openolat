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
package org.olat.selenium.page.course;

import java.net.URL;
import java.util.List;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.Graphene;
import org.junit.Assert;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.selenium.page.core.MenuTreePageFragment;
import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * Initial date: 20.06.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CoursePageFragment {
	
	public static final By courseRun = By.className("o_course_run");
	public static final By toolsMenu = By.cssSelector("ul.o_sel_course_tools");
	public static final By toolsMenuCaret = By.cssSelector("a.o_sel_course_tools");
	public static final By editCourseBy = By.className("o_sel_course_editor");
	public static final By treeContainerBy = By.id("o_main_left_content");
	
	@Drone
	private WebDriver browser;
	
	public CoursePageFragment() {
		//
	}
	
	public CoursePageFragment(WebDriver browser) {
		this.browser = browser;
	}
	
	
	public static CoursePageFragment getCourse(WebDriver browser, URL deploymentUrl, CourseVO course) {
		browser.navigate().to(deploymentUrl.toExternalForm() + "url/RepositoryEntry/" + course.getRepoEntryKey());
		OOGraphene.waitElement(courseRun);
		return new CoursePageFragment(browser);
	}
	
	public static CoursePageFragment getCourse(WebDriver browser) {
		OOGraphene.waitElement(courseRun);
		return new CoursePageFragment(browser);
	}
	
	public CoursePageFragment assertOnCoursePage() {
		WebElement treeContainer = browser.findElement(treeContainerBy);
		Assert.assertTrue(treeContainer.isDisplayed());
		return this;
	}
	
	public CoursePageFragment assertOnTitle(String displayName) {
		List<WebElement> titleList = browser.findElements(By.tagName("h2"));
		Assert.assertNotNull(titleList);
		Assert.assertEquals(1, titleList.size());
		
		WebElement title = titleList.get(0);
		Assert.assertTrue(title.isDisplayed());
		Assert.assertTrue(title.getText().contains(displayName));
		return this;
	}
	
	/**
	 * Click the first element of the menu tree
	 * @return
	 */
	public MenuTreePageFragment clickTree() {
		MenuTreePageFragment menuTree = new MenuTreePageFragment(browser);
		menuTree.selectRoot();
		return menuTree;
	}
	
	/**
	 * Open the tools drop-down
	 * @return
	 */
	public CoursePageFragment openToolsMenu() {
		browser.findElement(toolsMenuCaret).click();
		OOGraphene.waitElement(toolsMenu);
		return this;
	}
	
	/**
	 * Click the editor link in the tools drop-down
	 * @return
	 */
	public CourseEditorPageFragment edit() {
		if(!browser.findElement(toolsMenu).isDisplayed()) {
			openToolsMenu();
		}
		browser.findElement(editCourseBy).click();
		OOGraphene.waitBusy();
		OOGraphene.closeBlueMessageWindow(browser);

		WebElement main = browser.findElement(By.id("o_main"));
		return Graphene.createPageFragment(CourseEditorPageFragment.class, main);
	}
}
