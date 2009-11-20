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
package esg.node.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;

public class FauxDataNodeRemoteClient {
    private static final Log log = LogFactory.getLog(FauxDataNodeRemoteClient.class);

    public static void main(String[] args) throws Exception {
	String serviceURL;
	if (args.length != 1) {
	    System.out.println(" \nError: Must provide service endpoint url!");
	    System.out.println(" usage: % java esg.node.service.FauxDataNodeRemoteClient http://rainbow.llnl.gov/esg-node/datanode\n");
	    System.exit(1);
	}
	serviceURL = args[0];
	HessianProxyFactory factory = new HessianProxyFactory();
	factory.setReadTimeout(1000L);
	ESGDataNodeService datanode = (ESGDataNodeService)factory.create(ESGDataNodeService.class, serviceURL);

	System.out.print("Ping: ");
	try {
	    System.out.println((datanode.ping() ? "[OK]" : "[FAIL]"));
	}catch (HessianRuntimeException ex) {
	    System.out.println("[FAIL]");
	    log.error("Problem calling Ping on ["+serviceURL+"] "+ex.getMessage());
	}
    
    }
}
