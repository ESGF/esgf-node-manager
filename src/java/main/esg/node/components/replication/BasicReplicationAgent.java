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

   NO-OP place holder class.... to be filled in by Ann and company. :-)

*****************************************************************/
package esg.node.components.replication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.core.*;

public class BasicReplicationAgent extends ReplicationAgent {

    private static Log log = LogFactory.getLog(BasicReplicationAgent.class);

    public BasicReplicationAgent(String name) { super(name); }
    
    public void init() { System.out.println(getName()+".init() no-op"); }
    public void publishDataSet() { 
	System.out.println("publishDataSet no-op"); 
	ESGEvent publishEvent = new ESGEvent(this,"Publish Event from "+getName());
	fireESGEvent(publishEvent);
    }
    public void replicateDataSet() { System.out.println("replicateDataSet no-op"); }
    public void updateDataSet() { System.out.println("updateDataSet no-op"); }
    public void listDataSets() { System.out.println("listDataSets no-op"); }

    //Optional Methods
    public void replicateAllDataSets() { System.out.println("replicateAllDataSets no-op"); }
    public void listDataSetUpdates() { System.out.println("listAllDataSet no-op"); }
    public void isUpdatedDataSet() { System.out.println("isUpdatedDataSet no-op"); }

}