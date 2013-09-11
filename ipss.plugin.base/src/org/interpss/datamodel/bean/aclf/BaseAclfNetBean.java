/*
 * @(#)AclfNetBean.java   
 *
 * Copyright (C) 2008-2013 www.interpss.org
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
 * @Date 01/10/2013
 * 
 *   Revision History
 *   ================
 *
 */

package org.interpss.datamodel.bean.aclf;

import java.util.ArrayList;
import java.util.List;

import org.interpss.datamodel.bean.BaseJSONBean;
import org.interpss.datamodel.bean.BaseNetBean;

public class BaseAclfNetBean<TBus extends AclfBusBean, TBra extends AclfBranchBean> extends BaseNetBean {
	
	public List<TBus> 
		bus_list;					// bus result bean list
	public List<TBra> 
		branch_list;                // branch result bean list
	
	public BaseAclfNetBean() { bus_list = new ArrayList<TBus>(); branch_list = new ArrayList<TBra>(); }
	
	@Override public int compareTo(BaseJSONBean b) {
		int eql = super.compareTo(b);
		
		BaseAclfNetBean<TBus,TBra> bean = (BaseAclfNetBean<TBus,TBra>)b;

		// do nothing
		
		return eql;
	}	
	
	public boolean validate(List<String> msgList) {
		boolean noErr = super.validate(msgList);
		
		for (TBus bean : this.bus_list) 
			if (!bean.validate(msgList))
				noErr = false;
		for (TBra bean : this.branch_list) 
			if (!bean.validate(msgList))
				noErr = false;
		return noErr; 
	}	
}