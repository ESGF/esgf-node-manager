/*****************************************************************
 Copyright (c) 2009  Larence Livermore National Security (LLNS)
 All rights reserved.
*****************************************************************

       Project: Earth Systems Grid
   Description: Test Code

*****************************************************************/
package esg.node.components.monitoring;

import esg.common.Utils;
import esg.node.core.*;

//esg-node-test esg.node.components.monitoring.MonitorInfoTest

public class MonitorInfoTest {
    public static void main(String[] args) {
	MonitorDAO mon = new MonitorDAO(null,Utils.getNodeID(),new java.util.Properties());
	mon.getMonitorInfo();
    }
}