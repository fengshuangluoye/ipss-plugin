package org.interpss.datamodel.bean;

import java.util.List;

import org.interpss.numeric.datatype.Unit.UnitType;

import com.interpss.common.util.IpssLogger;

public abstract class BaseNetBean extends BaseJSONBean {
	public double base_kva; // network base kva

	/*public List<BaseBusBean> bus_list; // bus result bean list
	public List<BaseBranchBean> branch_list; // branch result bean list
*/
	/**
	 * units
	 */
	public UnitType unit_ang = UnitType.Deg, // angle unit for voltage, PsXfr
												// shifting angle
			unit_bus_v = UnitType.PU, // bus voltage unit
			unit_bus_p = UnitType.PU, // bus power (gen/load) unit
			unit_branch_z = UnitType.PU, // branch impedance unit
			unit_branch_cur = UnitType.Amp, // branch current unit
			unit_branch_b = UnitType.PU; // branch shunt Y unit

	
	
	public BaseNetBean() {  }
	
	@Override public int compareTo(BaseJSONBean b) {
		int eql = super.compareTo(b);
		
		BaseNetBean bean = (BaseNetBean)b;

		if (this.unit_ang != bean.unit_ang) {
			IpssLogger.ipssLogger.warning("BaseNetBean.unit_ang is not equal");
			eql = 1;
		}

		return eql;
	}	

	public boolean validate(List<String> msgList) {
		boolean noErr = true;
		if (this.base_kva == 0.0) {
			msgList.add("NetBean data error: baseKva not defined");
			noErr = false;
		}
		return noErr;
	}

}
