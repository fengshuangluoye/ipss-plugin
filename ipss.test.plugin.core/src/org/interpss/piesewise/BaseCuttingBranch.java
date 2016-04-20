 /*
  * @(#)BaseCuttingBranch.java   
  *
  * Copyright (C) 2006-2016 www.interpss.org
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE
  * as published by the Free Software Foundation; either version 2.1
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * @Author Mike Zhou
  * @Version 1.0
  * @Date 01/15/2016
  * 
  *   Revision History
  *   ================
  *
  */

package org.interpss.piesewise;


/**
 * 
 * @author Mike
 *
 */
public class BaseCuttingBranch<TCur> {
	// branch AclfNetwork id
	String branchId;
	// branch from/to bus SubArea flag
	int fromSubAreaFlag, toSubAreaFlag;
	// cutting branch Norton equivalent current
	TCur cur;
	
	public BaseCuttingBranch() {}
	
	public BaseCuttingBranch(String id, int fromFlag, int toFlag) {
		this.branchId = id;
		this.fromSubAreaFlag = fromFlag;
		this.toSubAreaFlag = toFlag;
	}
	
	public String toString() {
		String str =  "Branch Id: " + this.branchId + "\n"
				+ "From, To side SubArea Flag: " + this.fromSubAreaFlag + ", " + this.toSubAreaFlag + "\n";
		return str;
	}
}
