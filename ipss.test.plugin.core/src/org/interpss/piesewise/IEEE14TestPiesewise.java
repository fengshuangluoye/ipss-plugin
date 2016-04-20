 /*
  * @(#)IEEE14TestPiesewise.java   
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

import java.util.function.Function;

import org.apache.commons.math3.complex.Complex;
import org.interpss.CorePluginObjFactory;
import org.interpss.CorePluginTestSetup;
import org.interpss.fadapter.IpssFileAdapter;
import org.interpss.numeric.datatype.ComplexFunc;
import org.interpss.numeric.sparse.ISparseEqnComplex;
import org.interpss.numeric.util.NumericUtil;
import org.junit.Test;

import com.interpss.CoreObjectFactory;
import com.interpss.core.aclf.AclfBranch;
import com.interpss.core.aclf.AclfBus;
import com.interpss.core.aclf.AclfLoadCode;
import com.interpss.core.aclf.AclfNetwork;
import com.interpss.core.algo.LoadflowAlgorithm;


public class IEEE14TestPiesewise extends CorePluginTestSetup {
	/*
	 * Function to compute bus injection current
	 */
	Function<AclfBus, Complex> injCurFunc = bus -> {
			// The bus injection current is based on gen bus load flow results.
		if (bus.isGen()) {
			//System.out.println("Inj cur -- id, sortNumber, cur: " + bus.getId() + ", " + bus.getSortNumber() + ", " + ComplexFunc.toStr(i));
			return bus.getNetGenResults().divide(bus.getVoltage());
		}
		else 
			return new Complex(0.0, 0.0);
	};
	
	/*
	 * Solve the network Y-matrix using the full matrix approach
	 */
	@Test
	public void testCase1() throws Exception {
		AclfNetwork net = getTestNet();
  		
  		/*
  		 * Solve [Y][I] = [V] using the full network Y matrix	
  		 */
  		ISparseEqnComplex y = net.formYMatrix();
  		//System.out.println(y.toString());
  		
  		net.getBusList().forEach(bus -> {
				if (bus.isGen()) {
					Complex i = injCurFunc.apply(bus);
					y.setBi(i, bus.getSortNumber());
			  		//System.out.println("id, sortNumber, cur: " + bus.getId() + ", " + bus.getSortNumber() + ", " + ComplexFunc.toStr(i));
				}
			});
  		//System.out.println(y.toString());
  		
  		y.luMatrixAndSolveEqn(1.0e-10);
  		//System.out.println(y.toString());
  		
  		double[] results = {
  				1.1279777567087341, // 1
  				1.0464860447326718, // 2
  				0.9528686959960438, // 3
  				0.9799153321639716, // 4
  				0.9950266473976869, // 5
  				0.9671557817564894, // 6
  				0.9907677847192378, // 61
  				0.9499208713758617, // 7
  				0.9774389620016228, // 71
  				0.9231214900170436, // 8
  				0.9416955884946859, // 9
  				0.9785928456286968, // 91
  				0.9388713309613421, // 10
  				0.9493664990106351, // 11
  				0.9515407836174639, // 12
  				0.9461953528220368, // 13
  				0.9264016515631729, // 14				
  		};

  		for (int i = 0; i < y.getDimension(); i++) {
  			//System.out.println(y.getX(i).abs() + ", // " + y.getBusId(i));
  			assertTrue(NumericUtil.equals(y.getX(i).abs(), results[i], 1.0e-10));
  		}
  		
  		// turn off the cutting branches
  		for (AclfBranch branch : net.getBranchList()) {
  			Complex cur = y.getX(branch.getFromBus().getSortNumber())
  					          .subtract(y.getX(branch.getToBus().getSortNumber()))
  					          .multiply(branch.yft()).multiply(-1.0);
  			//System.out.println(branch.getId() + ": " + ComplexFunc.toStr(cur));
  		}
	}
	
	/*
	 * Break the network into two SubAreas
	 */
	@Test
	public void testCase2() throws Exception {
		AclfNetwork net = getTestNet();
  		
  		/*
  		 * Solve [Y][I] = [V] using the piecewise method
  		 * =============================================
  		 */
  		
  		int areaFlag1 = 1, areaFlag2 = 2;
		
  		PiecewiseAlgorithm pieceWiseAlgo = new PiecewiseAlgorithm(net);
  		SubAreaProcessor.SubArea[] subareas = {
  					new SubAreaProcessor.SubArea(areaFlag1, new String[] {"4", "5"}), 
  					new SubAreaProcessor.SubArea(areaFlag2, new String[] {"71", "91", "61"})};
  		
  		for( SubAreaProcessor.SubArea area : subareas)
  			pieceWiseAlgo.getSubAreaList().add(area); 

  		SubAreaProcessor.CuttingBranch[] cuttingBranches = { 
  							new SubAreaProcessor.CuttingBranch("4->71(1)", areaFlag1, areaFlag2),
  							new SubAreaProcessor.CuttingBranch("4->91(1)", areaFlag1, areaFlag2),
  							new SubAreaProcessor.CuttingBranch("5->61(1)", areaFlag1, areaFlag2)};
  		
  		String[][] subAreaBusSet = { {"1", "2", "3", "4", "5"},
                 {"61", "71", "91",  "6", "7", "8", "9", "10", "11", "12", "13", "14"}};
  		for(String s : subAreaBusSet[0]) {
  			net.getBus(s).setIntFlag(areaFlag1);
  		}

  		for(String s : subAreaBusSet[1]) {
  			net.getBus(s).setIntFlag(areaFlag2);
  		}
  		
  		// turn off the cutting branches
  		for (SubAreaProcessor.CuttingBranch cbra : cuttingBranches) {
  			AclfBranch branch = net.getBranch(cbra.branchId);
  			branch.setStatus(false);
  		}
  		
  		/*
  		 * 	4->71(1): 0.31752 + j-0.09918
			5->61(1): 0.54067 + j-0.16539
			4->91(1): 0.18765 + j-0.04213
  		 */
  		
  		/*//////////////////////////////////
  		 * Step-1: Solve for the open-circuit voltage
  		 *//////////////////////////////////
  		
  		pieceWiseAlgo.calculateOpenCircuitVoltage(this.injCurFunc);

  		//System.out.println("\n" + netVoltage.toString());
  		/*
  		 *  91=(-0.18659013832240784, 0.21419273612333975), 
  		 *  61=(-0.18163291956694141, 0.20707711498654005), 
  		 *  71=(-0.20477166939561042, 0.21222458286328053), 
  		 *  1=(1.5318884131122048, 0.5321466929013828)
  		 */
  		/*
  		 * Check open circuit voltage results
  		 */
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("91"), new Complex(-0.18659013832240784, 0.21419273612333975), 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("61"), new Complex(-0.18163291956694141, 0.20707711498654005), 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("71"), new Complex(-0.20477166939561042, 0.21222458286328053), 1.0e-10));
  		
  		
  		/*/////////////////////////////
  		 * Step-2: calculate cutting branch current
  		 */////////////////////////////
 
    	pieceWiseAlgo.calculateCuttingBranchCurrent(cuttingBranches);
    	/*
			Banch cur: 4->71(1) 0.31752 + j-0.09918
			Banch cur: 4->91(1) 0.18765 + j-0.04213
			Banch cur: 5->61(1) 0.54067 + j-0.16539
    	 */
    	/*
    	 * Check cutting branch currents
    	 */
		assertTrue(NumericUtil.equals(cuttingBranches[0].cur, new Complex(0.31752,-0.09918), 1.0e-4));
		
  		/*//////////////////////////////////////////
  		 * Step-3
  		 *//////////////////////////////////////////
		
		pieceWiseAlgo.calcuateNetVoltage(cuttingBranches);  		
 		
		pieceWiseAlgo.getNetVoltage().forEach((id, v) -> {
  			//System.out.println(v.abs() + ",   //  " + id);
  		});
  		/*
0.9388713309613442,   //  10
1.127977756708736,   //  1
0.9493664990106371,   //  11
1.0464860447326738,   //  2
0.9515407836174656,   //  12
0.9528686959960464,   //  3
0.9461953528220387,   //  13
0.9774389620016243,   //  71
0.9799153321639745,   //  4
0.926401651563175,   //  14
0.9950266473976895,   //  5
0.9671557817564914,   //  6
0.9499208713758636,   //  7
0.9231214900170452,   //  8
0.9416955884946879,   //  9
0.9907677847192397,   //  61
0.9785928456286984,   //  91
  		 */
		/*
		 * Checking bus voltage results
		 */
  		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("10").abs(), 0.9388713309613442, 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("1").abs(), 1.127977756708736, 1.0e-10));
	}

	@Test
	public void testCase2_1() throws Exception {
		AclfNetwork net = getTestNet();
  		
		SubAreaProcessor proc = new SubAreaProcessor(net, new SubAreaProcessor.CuttingBranch[] { 
				new SubAreaProcessor.CuttingBranch("4->71(1)"),
  				new SubAreaProcessor.CuttingBranch("4->91(1)"),
  				new SubAreaProcessor.CuttingBranch("5->61(1)")});	
		
		proc.processSubArea();
  		/*
  		 * Solve [Y][I] = [V] using the piecewise method
  		 * =============================================
  		 */
  		PiecewiseAlgorithm pieceWiseAlgo = new PiecewiseAlgorithm(net, proc.getSubAreaList());
  		
  		/*//////////////////////////////////
  		 * Step-1: Solve for the open-circuit voltage
  		 *//////////////////////////////////
  		
  		pieceWiseAlgo.calculateOpenCircuitVoltage(this.injCurFunc);

  		//System.out.println("\n" + netVoltage.toString());
  		
  		/*/////////////////////////////
  		 * Step-2: calculate cutting branch current
  		 */////////////////////////////
 
    	pieceWiseAlgo.calculateCuttingBranchCurrent(proc.getCuttingBranches());

		
  		/*//////////////////////////////////////////
  		 * Step-3
  		 *//////////////////////////////////////////
		
		pieceWiseAlgo.calcuateNetVoltage(proc.getCuttingBranches());  		
 		
		/*
		 * Checking bus voltage results
		 */
  		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("10").abs(), 0.9388713309613442, 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("1").abs(), 1.127977756708736, 1.0e-10));
	}

	/*
	 * Break the network into three SubAreas
	 */
	@Test
	public void testCase4() throws Exception {
		AclfNetwork net = getTestNet();
  		
  		/*
  		 * Solve [Y][I] = [V] using the piecewise method
  		 * =============================================
  		 */
  		
  		int areaFlag1 = 1, areaFlag2 = 2, areaFlag3 = 3;

  		PiecewiseAlgorithm pieceWiseAlgo = new PiecewiseAlgorithm(net);
  		
  		SubAreaProcessor.SubArea[] subareas = {
					new SubAreaProcessor.SubArea(areaFlag1, new String[] {"4", "5"}), 
					new SubAreaProcessor.SubArea(areaFlag2, new String[] {"71", "91", "61", "9", "13"}),
					new SubAreaProcessor.SubArea(areaFlag3, new String[] {"14"})};
		
		for( SubAreaProcessor.SubArea area : subareas)
			pieceWiseAlgo.getSubAreaList().add(area); 

		SubAreaProcessor.CuttingBranch[] cuttingBranches = { 
					new SubAreaProcessor.CuttingBranch("4->71(1)", areaFlag1, areaFlag2),
					new SubAreaProcessor.CuttingBranch("4->91(1)", areaFlag1, areaFlag2),
					new SubAreaProcessor.CuttingBranch("5->61(1)", areaFlag1, areaFlag2),
					new SubAreaProcessor.CuttingBranch("9->14(1)", areaFlag2, areaFlag3),
					new SubAreaProcessor.CuttingBranch("14->13(1)", areaFlag3, areaFlag2) };
  		
  		String[][] subAreaBusSet = { 
					 {"1", "2", "3", "4", "5"},
					 {"61", "71", "91",  "6", "7", "8", "9", "10", "11", "12", "13"}, 
					 { "14"}};
  		
  		for(String s : subAreaBusSet[0]) {
  			net.getBus(s).setIntFlag(areaFlag1);
  		}

  		for(String s : subAreaBusSet[1]) {
  			net.getBus(s).setIntFlag(areaFlag2);
  		}
  		
  		for(String s : subAreaBusSet[2]) {
  			net.getBus(s).setIntFlag(areaFlag3);
  		}  		
  		
  		// turn off the cutting branches
  		for (SubAreaProcessor.CuttingBranch cbra : cuttingBranches) {
  			AclfBranch branch = net.getBranch(cbra.branchId);
  			assertTrue(branch.getFromBus().getIntFlag() != branch.getToBus().getIntFlag());
  			branch.setStatus(false);
  		}
  		
  		/*//////////////////////////////////
  		 * Step-1: Solve for the open-circuit voltage
  		 *//////////////////////////////////
  		
  		pieceWiseAlgo.calculateOpenCircuitVoltage(this.injCurFunc);

  		//System.out.println("\n" + pieceWiseAlgo.getNetVoltage().toString());
  		/*
  		 *  91=91=(-0.2285660705729364, 0.248387737183099)
  		 *  1=(1.5318884131122048, 0.5321466929013828)} 
  		 *  14=(0.0, 0.0), 

  		 */
  		/*
  		 * Check open circuit voltage results
  		 */
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("91"), new Complex(-0.2285660705729364, 0.248387737183099), 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("14"), new Complex(0.0, 0.0), 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("1"), new Complex(1.5318884131122048, 0.5321466929013828), 1.0e-10));
		
		
  		/*/////////////////////////////
  		 * Step-2: calculate cutting branch current
  		 */////////////////////////////
  		
    	pieceWiseAlgo.calculateCuttingBranchCurrent(cuttingBranches);
    	for (SubAreaProcessor.CuttingBranch branch: cuttingBranches) {
    		//System.out.println("Branch cur: " + branch.branchId + "  " + ComplexFunc.toStr(branch.cur));
    	}	
    	/*
4->71(1): 0.31752 + j-0.09918
4->91(1): 0.18765 + j-0.04213
5->61(1): 0.54067 + j-0.16539
9->14(1): 0.08501 + j0.02301
13->14(1): 0.05971 + j-0.00704
    	 */    	
    	
    	
  		/*//////////////////////////////////////////
  		 * Step-3
  		 *//////////////////////////////////////////
    	
		pieceWiseAlgo.calcuateNetVoltage(cuttingBranches);  		
 		
		pieceWiseAlgo.getNetVoltage().forEach((id, v) -> {
  			//System.out.println(v.abs() + ",   //  " + id);
  		});	    
		
  		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("10").abs(), 0.9388713309613453, 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("1").abs(), 1.1279777567087346, 1.0e-10));
	}

	/*
	 * Break the network into three SubAreas
	 */
	@Test
	public void testCase4_1() throws Exception {
		AclfNetwork net = getTestNet();
		
		SubAreaProcessor proc = new SubAreaProcessor(net, new SubAreaProcessor.CuttingBranch[] { 
				new SubAreaProcessor.CuttingBranch("4->71(1)"),
				new SubAreaProcessor.CuttingBranch("4->91(1)"),
				new SubAreaProcessor.CuttingBranch("5->61(1)"),
				new SubAreaProcessor.CuttingBranch("9->14(1)"),
				new SubAreaProcessor.CuttingBranch("14->13(1)")});	
		
		proc.processSubArea();
  		
  		/*
  		 * Solve [Y][I] = [V] using the piecewise method
  		 * =============================================
  		 */
  		
  		PiecewiseAlgorithm pieceWiseAlgo = new PiecewiseAlgorithm(net, proc.getSubAreaList());
  		
  		
  		/*//////////////////////////////////
  		 * Step-1: Solve for the open-circuit voltage
  		 *//////////////////////////////////
  		
  		pieceWiseAlgo.calculateOpenCircuitVoltage(this.injCurFunc);

  		//System.out.println("\n" + pieceWiseAlgo.getNetVoltage().toString());
		
  		/*/////////////////////////////
  		 * Step-2: calculate cutting branch current
  		 */////////////////////////////
  		
    	pieceWiseAlgo.calculateCuttingBranchCurrent(proc.getCuttingBranches());

    	
  		/*//////////////////////////////////////////
  		 * Step-3
  		 *//////////////////////////////////////////
    	
		pieceWiseAlgo.calcuateNetVoltage(proc.getCuttingBranches());  		
 		
  		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("10").abs(), 0.9388713309613453, 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("1").abs(), 1.1279777567087346, 1.0e-10));
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

  		/*
  		 * Get the default loadflow algorithm and Run loadflow analysis. By default, it uses
  		 * NR method with convergence error tolerance 0.0001 pu
  		 */
	  	LoadflowAlgorithm algo = CoreObjectFactory.createLoadflowAlgorithm(net);
	  	algo.loadflow();
  		//System.out.println(net.net2String());
	  	
	  	/*
	  	 * Check if loadflow has converged
	  	 */
  		assertTrue(net.isLfConverged());
  		
  		/*
  		 * Turn all loads to Constant-Z load
  		 */
  		net.getBusList().forEach(bus -> {
  				if (bus.isLoad()) 
  					bus.setLoadCode(AclfLoadCode.CONST_Z);
  			}); 		
  		
  		return net;
	}
	
	/*
	 * This test has issues. It might caused by the condition of the 2nd SubArea Y-matrix
	 */
	//@Test
	public void testCase3() throws Exception {
		AclfNetwork net = getTestNet();
  		
  		/*
  		 * Solve [Y][I] = [V] using the piecewise method
  		 * =============================================
  		 */
  		
  		int areaFlag1 = 1, areaFlag2 = 2, areaFlag3 = 3;

  		PiecewiseAlgorithm pieceWiseAlgo = new PiecewiseAlgorithm(net);
  		
  		SubAreaProcessor.SubArea[] subareas = {
					new SubAreaProcessor.SubArea(areaFlag1, new String[] {"4", "5"}), 
					new SubAreaProcessor.SubArea(areaFlag2, new String[] {"71", "91", "61", "9", "6", "12"}),
					new SubAreaProcessor.SubArea(areaFlag3, new String[] {"13", "14"})};
		
		for( SubAreaProcessor.SubArea area : subareas)
			pieceWiseAlgo.getSubAreaList().add(area); 

		SubAreaProcessor.CuttingBranch[] cuttingBranches = { 
					new SubAreaProcessor.CuttingBranch("4->71(1)", areaFlag1, areaFlag2),
					new SubAreaProcessor.CuttingBranch("4->91(1)", areaFlag1, areaFlag2),
					new SubAreaProcessor.CuttingBranch("5->61(1)", areaFlag1, areaFlag2),
					new SubAreaProcessor.CuttingBranch("9->14(1)", areaFlag2, areaFlag3),
					new SubAreaProcessor.CuttingBranch("6->13(1)", areaFlag2, areaFlag3),
					new SubAreaProcessor.CuttingBranch("12->13(1)", areaFlag2, areaFlag3)};
  		
  		String[][] subAreaBusSet = { 
					 {"1", "2", "3", "4", "5"},
					 {"61", "71", "91",  "6", "7", "8", "9", "10", "11", "12"}, 
					 { "13", "14"}};
  		
  		for(String s : subAreaBusSet[0]) {
  			net.getBus(s).setIntFlag(areaFlag1);
  		}

  		for(String s : subAreaBusSet[1]) {
  			net.getBus(s).setIntFlag(areaFlag2);
  		}
  		
  		for(String s : subAreaBusSet[2]) {
  			net.getBus(s).setIntFlag(areaFlag3);
  		}  		
  		
  		// turn off the cutting branches
  		for (SubAreaProcessor.CuttingBranch cbra : cuttingBranches) {
  			AclfBranch branch = net.getBranch(cbra.branchId);
  			assertTrue(branch.getFromBus().getIntFlag() != branch.getToBus().getIntFlag());
  			branch.setStatus(false);
  		}
  		
  		/*//////////////////////////////////
  		 * Step-1: Solve for the open-circuit voltage
  		 *//////////////////////////////////
  		
  		pieceWiseAlgo.calculateOpenCircuitVoltage(this.injCurFunc);

  		//System.out.println("\n" + pieceWiseAlgo.getNetVoltage().toString());
  		/*
  		 *  91=(-0.21704264054476824, 0.22847433520573676), 
  		 *  14=(0.0, 0.0), 
  		 *  1=(1.5318884131122048, 0.5321466929013828)}

  		 */
  		/*
  		 * Check open circuit voltage results
  		 */
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("91"), new Complex(-0.21704264054476824, 0.22847433520573676), 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("14"), new Complex(0.0, 0.0), 1.0e-10));
		assertTrue(NumericUtil.equals(pieceWiseAlgo.getNetVoltage().get("1"), new Complex(1.5318884131122048, 0.5321466929013828), 1.0e-10));
		
  		/*/////////////////////////////
  		 * Step-2: calculate cutting branch current
  		 */////////////////////////////
    	pieceWiseAlgo.calculateCuttingBranchCurrent(cuttingBranches);
    	for (SubAreaProcessor.CuttingBranch branch: cuttingBranches) {
    		System.out.println("Branch cur: " + branch.branchId + "  " + ComplexFunc.toStr(branch.cur));
    	}

    	/*
4->71(1): 0.31752 + j-0.09918
4->91(1): 0.18765 + j-0.04213
5->61(1): 0.54067 + j-0.16539
9->14(1): 0.08501 + j0.02301
6->13(1): 0.18075 + j0.00149
12->13(1): 0.01788 + j-0.00283
    	 */
    	/*
    	 * Check cutting branch currents
    	 */
		//assertTrue(NumericUtil.equals(cuttingBranches[0].cur, new Complex(0.31752,-0.09918), 1.0e-4));

    	/*//////////////////////////////////////////
  		 * Step-3
  		 *//////////////////////////////////////////
		
		pieceWiseAlgo.calcuateNetVoltage(cuttingBranches);  		
 		
		pieceWiseAlgo.getNetVoltage().forEach((id, v) -> {
  			System.out.println(v.abs() + ",   //  " + id);
  		});		
	}	
}
