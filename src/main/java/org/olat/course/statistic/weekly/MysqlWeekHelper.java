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
package org.olat.course.statistic.weekly;

import java.util.Calendar;
import java.util.Locale;

/** helper class used to calculate the mysql week mode based on a locale **/
public class MysqlWeekHelper {

	public static int getMysqlWeekMode(Locale locale) {
		/*
		 * From http://dev.mysql.com/doc/refman/5.1/en/date-and-time-functions.html#function_week
		 * 
Mode 	First day of week 	Range 	Week 1 is the first week …
0 	Sunday 	0-53 	with a Sunday in this year
1 	Monday 	0-53 	with more than 3 days this year
2 	Sunday 	1-53 	with a Sunday in this year
3 	Monday 	1-53 	with more than 3 days this year
4 	Sunday 	0-53 	with more than 3 days this year
5 	Monday 	0-53 	with a Monday in this year
6 	Sunday 	1-53 	with more than 3 days this year
7 	Monday 	1-53 	with a Monday in this year
		 */

		Calendar c = Calendar.getInstance(locale);
		int firstDayOfWeek = c.getFirstDayOfWeek();
		int minimalDaysInFirstWeek = c.getMinimalDaysInFirstWeek();
		
		if (firstDayOfWeek==Calendar.SUNDAY) {
			// that could be mode 0,2,4,6
			
			//Mode 	First day of week 	Range 	Week 1 is the first week …
			// 0 	Sunday 	0-53 	with a Sunday in this year
			// 4 	Sunday 	0-53 	with more than 3 days this year

			// 2 	Sunday 	1-53 	with a Sunday in this year
			// 6 	Sunday 	1-53 	with more than 3 days this year

			if (minimalDaysInFirstWeek==7) {
				return 2;
			} else if (minimalDaysInFirstWeek>3) {
				return 6;
			} else {
				return 4;
			}
		} else {
			// otherwise it must be MONDAY
			
			// that could be mode 1,3,5,7
			
			// Mode 	First day of week 	Range 	Week 1 is the first week …
			// 1 	Monday 	0-53 	with more than 3 days this year
			// 3 	Monday 	1-53 	with more than 3 days this year
			// 5 	Monday 	0-53 	with a Monday in this year
			// 7 	Monday 	1-53 	with a Monday in this year
			
			if (minimalDaysInFirstWeek==7) {
				return 7;
			} else if (minimalDaysInFirstWeek>3) {
				return 3;
			} else {
				return 1;
			}
		}
	}
}
