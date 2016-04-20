 /*
  * @(#)IEEE14TestSubAreaSearch.java   
  *
  * Copyright (C) 2006 www.interpss.org
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
  * @Date 09/15/2006
  * 
  *   Revision History
  *   ================
  *
  */

package org.interpss.piesewise;

import static org.junit.Assert.assertTrue;

import org.interpss.CorePluginObjFactory;
import org.interpss.CorePluginTestSetup;
import org.interpss.fadapter.IpssFileAdapter;
import org.junit.Test;

import com.interpss.core.aclf.AclfNetwork;


public class IEEE14TestSubAreaSearch extends CorePluginTestSetup {
	private static final int DefaultFlag = -1;
	
	@Test
	public void testCase1() throws Exception {
		AclfNetwork net = getTestNet();
		
		
		SubAreaProcessor proc = new SubAreaProcessor(net, new SubAreaProcessor.CuttingBranch[] { 
					new SubAreaProcessor.CuttingBranch("4->71(1)"),
					new SubAreaProcessor.CuttingBranch("4->91(1)"),
					new SubAreaProcessor.CuttingBranch("5->61(1)")});	
  		
  		proc.processSubArea();
  		
  		proc.getSubAreaList().forEach(subarea -> {
  			//System.out.println(subarea);
  		});
  		
  		assertTrue(proc.getSubAreaList().size() == 2);
  		assertTrue(proc.getSubArea(1).interfaceBusIdList.size() == 2);
  		assertTrue(proc.getSubArea(2).interfaceBusIdList.size() == 3);
  		
  		net.getBusList().forEach(bus -> {
  			assertTrue(bus.getIntFlag() != DefaultFlag);
  			//System.out.println(bus.getId() + "," + bus.getIntFlag());
  			if (bus.getId().equals("2")) assertTrue(bus.getIntFlag() == 1);
  			if (bus.getId().equals("13")) assertTrue(bus.getIntFlag() == 2);
  		});
	}

	@Test
	public void testCase2() throws Exception {
		AclfNetwork net = getTestNet();
		
		
		SubAreaProcessor proc = new SubAreaProcessor(net, new SubAreaProcessor.CuttingBranch[] { 
					new SubAreaProcessor.CuttingBranch("4->71(1)"),
					new SubAreaProcessor.CuttingBranch("4->91(1)"),
					new SubAreaProcessor.CuttingBranch("5->61(1)"),
					new SubAreaProcessor.CuttingBranch("9->14(1)"),
					new SubAreaProcessor.CuttingBranch("14->13(1)")});	
  		
  		proc.processSubArea();
  		
  		proc.getSubAreaList().forEach(subarea -> {
  			//System.out.println(subarea);
  		});
  		
  		assertTrue(proc.getSubAreaList().size() == 3);
  		assertTrue(proc.getSubArea(1).interfaceBusIdList.size() == 2);
  		assertTrue(proc.getSubArea(2).interfaceBusIdList.size() == 5);
  		assertTrue(proc.getSubArea(7).interfaceBusIdList.size() == 1);  		
  		
  		net.getBusList().forEach(bus -> {
  			assertTrue(bus.getIntFlag() != DefaultFlag);
  			//System.out.println(bus.getId() + "," + bus.getIntFlag());
  			if (bus.getId().equals("2")) assertTrue(bus.getIntFlag() == 1);
  			if (bus.getId().equals("61")) assertTrue(bus.getIntFlag() == 2);
  			if (bus.getId().equals("14")) assertTrue(bus.getIntFlag() == 7);  			
  		});
	}
	
	private AclfNetwork getTestNet() throws Exception {
		/*
		 * Load the network and run Loadflow
		 */
		AclfNetwork net = CorePluginObjFactory
					.getFileAdapter(IpssFileAdapter.FileFormat.IpssInternal)
					.load("testData/ipssdata/ieee14piecewise.ipssdat")
					.getAclfNet();	
		
  		//System.out.println(net.net2String());
  		assertTrue((net.getBusList().size() == 17 && net.getBranchList().size() == 23));

  		return net;
	}
}
