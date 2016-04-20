 /*
  * @(#)SubAreaProcessor.java   
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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.interpss.numeric.datatype.ComplexFunc;
import org.interpss.numeric.sparse.ISparseEqnComplex;

import com.interpss.common.exp.InterpssException;
import com.interpss.common.util.IpssLogger;
import com.interpss.core.aclf.AclfBranch;
import com.interpss.core.aclf.AclfNetwork;
import com.interpss.core.net.Bus;

/**
 * Class for SubArea processing. It begins by defining a set of cutting branches.
 * It finds SubAreas in the network and SubArea interface buses.
 * 
 * @author Mike
 *
 */
		
public class SubAreaProcessor {
	private static final int DefaultFlag = -1;
	
	/**
	 * Class holding a pair of buses for SubArea processing. The two buses are stored
	 * so that bus1.IntFlag <= bus2.InfFlag. 
	 */
	private static class BusPair {
		Bus bus1, bus2;
		int subAreaFlag = DefaultFlag;
		
		public BusPair(Bus bus) {
			this.bus1 = bus;
			this.bus2 = bus;
			this.subAreaFlag = bus.getIntFlag();
		}
		
		public BusPair(Bus bus1, Bus bus2) {
			// make sure that bus1.IntFlag <= bus2.InfFlag.
			if (bus1.getIntFlag() < bus2.getIntFlag()) {
				this.bus1 = bus1;
				this.bus2 = bus2;
			}
			else {
				this.bus1 = bus2;
				this.bus2 = bus1;
			}
		}
		
		public String getKey() {
			return createKey(bus1.getIntFlag(), bus2.getIntFlag());
		};
		
		public static String createKey(int flag1, int flag2) {
			return flag1 + "_" + flag2;
		};
		
		public static String createKey(int flag1) {
			return flag1 + "_" + flag1;
		};
		
		public String toString() {
			return "[" + this.bus1.getIntFlag() + " \"" + this.bus1.getId() + "\",  "
					+ this.bus2.getIntFlag()  + " \""  + this.bus2.getId() + "\",  "
					+ this.subAreaFlag + "]";
		}
	}
	
	/**
	 * Class for modeling the cutting branch concept
	 */
	public static class CuttingBranch extends BaseCuttingBranch<Complex> {
		public CuttingBranch(String id, int fromFlag, int toFlag) {
			super(id, fromFlag, toFlag);
		}
		
		public CuttingBranch(String id) {
			super(id, DefaultFlag, DefaultFlag);
		}
		
		public String toString() {
			String str =  super.toString();
			if (this.cur != null)
				str += "Branch current (from->to): " + ComplexFunc.toStr(this.cur) + "\n";
			return str;
		}
	}
	
	/**
	 * Class for modeling the SubArea concept
	 */
	public static class SubArea {
		// SubArea flag, which should be unique
		int flag;
		// interface bus ID array
		List<String> interfaceBusIdList;
		// SubArea Y-matrix sparse eqn 
		ISparseEqnComplex ySparseEqn;
		// SubArea Norton equivalent Z-matrix
		Complex[][] zMatrix;
		
		public SubArea(int flag) {
			this.flag = flag;
			this.interfaceBusIdList = new ArrayList<>();
		}
		
		public SubArea(int flag, String[] ids) {
			this.flag = flag;
			this.interfaceBusIdList = new ArrayList<>();
			for (String id : ids)
				this.interfaceBusIdList.add(id);
		}
		
		public String toString() {
			return "SubArea flag: " + this.flag + "\n"
					+ "Interface bus id set: " + this.interfaceBusIdList + "\n";
		}
	}	
	
	// AclfNetwork object
	private AclfNetwork net;
	
	// Cutting branch set
	private CuttingBranch[] cuttingBranches;
	
	// Sub-area list 
	private List<SubArea> subareaList;
	
	/**
	 * Constructor
	 * 
	 * @param net AclfNetwork object
	 */
	public SubAreaProcessor(AclfNetwork net) {
		this.net = net;
		this.subareaList = new ArrayList<>();
	}

	/**
	 * Constructor
	 * 
	 * @param net AclfNetwork object
	 * @param cuttingBranches cutting branch set
	 */
	public SubAreaProcessor(AclfNetwork net, CuttingBranch[] cuttingBranches) {
		this(net);
		this.cuttingBranches = cuttingBranches;
	}
	
	/**
	 * return the cutting branch set
	 * 
	 * @return the cuttingBranches
	 */
	public CuttingBranch[] getCuttingBranches() {
		return cuttingBranches;
	}

	/**
	 * set the cutting branch set
	 * 
	 * @param cuttingBranches the cuttingBranches to set
	 */
	public void setCuttingBranches(CuttingBranch[] cuttingBranches) {
		this.cuttingBranches = cuttingBranches;
	}
	
	/**
	 * get the subarea list
	 * 
	 * @return the netVoltage
	 */
	public List<SubAreaProcessor.SubArea> getSubAreaList() {
		return this.subareaList;
	}

	/**
	 * get SubArea by the area flag
	 * 
	 * @param flag the area flag
	 * @return the subarea object
	 */
	public SubAreaProcessor.SubArea getSubArea(int flag) {
		for (SubAreaProcessor.SubArea subarea: this.subareaList) {
			if (subarea.flag == flag)
				return subarea;
		}
		return null;
	}	

	/**
	 * Initialization for SubArea processing 
	 * 
	 */
	private void initialization() {
		// init bus IntFlag
		net.getBusList().forEach(bus -> { bus.setIntFlag(DefaultFlag);});
		
		// init cutting branch set
  		int flag = 0;
  		for (CuttingBranch cbra : cuttingBranches) {
  			AclfBranch branch = net.getBranch(cbra.branchId);
  			branch.setStatus(false);
  			
  			if (branch.getFromBus().getIntFlag() == DefaultFlag) {
  				cbra.fromSubAreaFlag = ++flag;
  				branch.getFromBus().setIntFlag(cbra.fromSubAreaFlag);
  				//System.out.println("Bus " + branch.getFromBus().getId() + " assigned Bus Flag: " + flag);
  			}

  			if (branch.getToBus().getIntFlag() == DefaultFlag) {
  				cbra.toSubAreaFlag = ++flag;
  				branch.getToBus().setIntFlag(cbra.toSubAreaFlag);
  				//System.out.println("Bus " + branch.getToBus().getId() + " assigned Bus Flag: " + flag);
  			}
  		}		
	}
	
	/**
	 * Process SubArea
	 * 
	 */
	public void processSubArea() {
		initialization();
		
		Hashtable<String, BusPair> busPairSet = new Hashtable<>();
			// There are two types of BusPair record in the busPairSet Hashtable
			// Type1 ["10",  "9",  -1] - indicating Bus "10" and Bus "9" are in the same SubArea 
			// Type2 ["61",  "61",  5] - the interface Bus current SubArea flag is 5, which needs to be 
			//                           consolidated to the smallest Bus.IntFlag in the SubAre
		
  		for (CuttingBranch cbra : cuttingBranches) {
  			AclfBranch branch = net.getBranch(cbra.branchId);

  			// starting from the fromBus, recursively set the branch opposite 
  			// bus IntFlag for SubArea processing 
  			BusPair pair = new BusPair(branch.getFromBus());
  			if (busPairSet.get(pair.getKey()) == null) {
					busPairSet.put(pair.getKey(), pair);
				}
  			setConnectedBusFlag(branch.getFromBus(), busPairSet);
  			
  			// starting from the toBus, recursively set the branch opposite 
  			// bus IntFlag for SubArea processing 
  			pair = new BusPair(branch.getToBus());
  			if (busPairSet.get(pair.getKey()) == null) {
					busPairSet.put(pair.getKey(), pair);
				}
  			setConnectedBusFlag(branch.getToBus(), busPairSet);
  		}	
		//System.out.println(this.busPairSet);
		
  		// consolidate bus.IntFlag and create SubArea flag
		busPairSet.forEach((key, pair) -> {
			// bus1 and bus2 are in the same SubArea and make sure only process Type1 record 
			if (!pair.bus1.getId().equals(pair.bus2.getId())) {
				// get Type2 records corresponding to bus1 and bus2
				BusPair p1 = busPairSet.get(BusPair.createKey(pair.bus1.getIntFlag()));
				BusPair p2 = busPairSet.get(BusPair.createKey(pair.bus2.getIntFlag()));
				
				// SubArea Flag is the smallest Bus.IntFlag in a SubArea. Bus.IntFlag has been initialized at
				// the interface bus in the initialization() method
				if (p1.subAreaFlag > p2.subAreaFlag) {
					// recursively set p1.subAreaFlag to p2.subAreaFlag
					setSubAreaFlag(p1, p2.subAreaFlag, busPairSet);
				}
				else if (p1.subAreaFlag < p2.subAreaFlag) {
					// recursively set p2.subAreaFlag to p1.subAreaFlag
					setSubAreaFlag(p2, p1.subAreaFlag, busPairSet);
				}
			}
		});
		//System.out.println(this.busPairSet);
		
		// create SubArea list and the subArea.interfaceBusIdList
		busPairSet.forEach((key, pair) -> {
			// make sure only Type2 records are processed
			if (pair.bus1.getIntFlag() == pair.bus2.getIntFlag()) {
				//System.out.println(pair);
				SubArea subarea = getSubArea(pair.subAreaFlag);
				if (subarea == null) {
					// create SubArea object and add to the SubArea list
					subarea = new SubArea(pair.subAreaFlag);
					getSubAreaList().add(subarea);
				}
				// add the interface bus ID to the list
				subarea.interfaceBusIdList.add(pair.bus1.getId());
			}
		});		
		
		// update network bus SubArea flag
		net.getBusList().forEach(bus -> {
			BusPair p = busPairSet.get(BusPair.createKey(bus.getIntFlag()));
			bus.setIntFlag(p.subAreaFlag);
		});
	}
	
	/**
	 * Recursively set Bus SubArea flag
	 * 
	 * @param startPair the starting Bus (Type2 record)
	 * @param flag SubArea flag
	 */
	private void setSubAreaFlag(BusPair startPair, int flag, Hashtable<String, BusPair> busPairSet) {
		// 5_5=["61",  "61",  5], 6_6=["9",  "9",  5], 2_2=["71",  "71",  2]
		// if "71" and "9" are connected, when set 6_6 flag to 2, 5_5 flag should be set also (recursively)
		if (startPair.bus1.getIntFlag() != startPair.subAreaFlag) {
			BusPair nextPair = busPairSet.get(BusPair.createKey(startPair.subAreaFlag));
			setSubAreaFlag(nextPair, flag, busPairSet);
		}
		startPair.subAreaFlag = flag;
	}

	/**
	 * recursive function, recursively visit the opposite bus of the bus connected branch
	 * to define SubArea in the network 
	 * 
	 * @param bus
	 */
	private void setConnectedBusFlag(Bus bus, Hashtable<String, BusPair> busPairSet) {
		bus.getBranchList().forEach(branch -> {
  			try {
  				if (branch.isActive()) {
  					Bus optBus = branch.getOppositeBus(bus);
  					if (optBus.getIntFlag() == DefaultFlag) {
  						optBus.setIntFlag(bus.getIntFlag());
  						setConnectedBusFlag(optBus, busPairSet);
  					}
  					else {
  						// the optBus has been already visited with a non-DefaultFlag
  						//System.out.println("Bus " + optBus.getId() + " marked");
  						if (bus.getIntFlag() != optBus.getIntFlag()) {
  							// make sure that the bus and optBus are visited starting from different 
  							// interface bus
  							// store the bus and optBus pair to indicate that they belong to the same SubArea
  							String key = BusPair.createKey(bus.getIntFlag(), optBus.getIntFlag());
  							if (busPairSet.get(key) == null) {
  	  							busPairSet.put(key, new BusPair(bus, optBus));
  								//System.out.println(x + " belong to the same SubArea" );
  							}
  						}
  					}
  				}
			} catch (InterpssException e) {
				IpssLogger.logErr(e);
			}
  		});
	}	
}
