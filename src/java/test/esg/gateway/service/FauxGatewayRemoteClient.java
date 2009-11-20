/*****************************************************************

  Organization: Lawrence Livermore National Lab (LLNL)
   Directorate: Computation
    Department: Computing Applications and Research
      Division: S&T Global Security
        Matrix: Atmospheric, Earth and Energy Division
       Program: PCMDI
       Project: Earth Systems Grid
  First Author: Gavin M. Bell (gavin@llnl.gov)

   Description:

*****************************************************************/
package esg.gateway.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;

import esg.node.core.BasicGateway;

public class FauxGatewayRemoteClient {
    private static final Log log = LogFactory.getLog(FauxGatewayRemoteClient.class);

    public static void main(String[] args) throws Exception {
	String serviceURL;
	if (args.length != 1) {
	    System.out.println(" \nError: Must provide service endpoint url!");
	    System.out.println(" usage: % java esg.node.service.FauxGatewayRemoteClient http://rainbow.llnl.gov/esg-node/gateway\n");
	    System.exit(1);
	}
	serviceURL = args[0];

	BasicGateway gway = new BasicGateway("TEST_GWAY_CLIENT",serviceURL);
	gway.init();
	System.out.println("Calling Ping: "+(gway.ping() ? "[OK]" : "[FAIL]") );
	System.out.println("Calling RegisterToGateway: "+(gway.registerToGateway() ? "[OK]" : "[FAIL]") );

    }
}
