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
package esg.node.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.components.replication.ReplicationAgent;
import esg.node.components.replication.BasicReplicationAgent;

public class FauxSystemTest {

    private static Log log = LogFactory.getLog(FauxSystemTest.class.getName());

    private static DataNodeManager dnmgr;

    public FauxSystemTest() {
	log.info("Instantiating FauxSystemTest...");
	dnmgr = new ESGDataNodeManager();
    }

    public void doIt() {
	ReplicationAgent ra0  = new BasicReplicationAgent("Faux Replication Agent 0");
	ra0.init();
	Gateway gateway0      = new BasicGateway("Faux Gateway 0");
	gateway0.init();
	dnmgr.registerGateway(gateway0);
	dnmgr.registerComponent(ra0);
	
	System.out.println("1) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0 and gateway 0]\n----\n");

	ReplicationAgent ra1 = new BasicReplicationAgent("Faux Replication Agent 1");
	ReplicationAgent ra2 = new BasicReplicationAgent("Faux Replication Agent 2");
	Gateway gateway1     = new BasicGateway("Faux Gateway 1");
	dnmgr.registerComponent(ra1);
	dnmgr.registerGateway(gateway1);
	dnmgr.registerComponent(ra2);

	System.out.println("2) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0,1,2 and gateway 0,1]\n----\n");

	dnmgr.removeComponent(ra2);
	dnmgr.removeGateway(gateway1);

	System.out.println("3) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0,1 and gateway 0]\n----\n");

	ra2.unregister(); //This should do nothing as it was already removed from dnmgr
	ra1.unregister();

	System.out.println("4) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0 and gateway 0]\n----\n");

	ra1.publishDataSet();
	ra0.publishDataSet();
	
	ReplicationAgent ra3 = new BasicReplicationAgent("Faux Replication Agent 3");
	dnmgr.registerComponent(ra3);
	Gateway gateway2     = new BasicGateway("Faux Gateway 2");
	dnmgr.registerComponent(gateway2); //this will be ignored and a warning issued.
	System.out.println("\n5) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0,3 and gateway 0]\n----\n");

	System.out.println("---");
    }

    public static void main(String[] args) {
	FauxSystemTest fst = new FauxSystemTest();
	fst.doIt();
	System.gc();
	//at this point ra3 should have fallen out of scope and the
	//finalize should have cleaned it up and unregistererd it from
	//the data node manager... do we see it?
	System.out.println(dnmgr+"\n---");
    }
}