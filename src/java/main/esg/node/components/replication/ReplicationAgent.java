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
package esg.node.components.replication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.core.*;

public abstract class ReplicationAgent extends AbstractDataNodeComponent {
    
    private static Log log = LogFactory.getLog(ReplicationAgent.class);

    public ReplicationAgent(String name) { super(name); }

    public abstract void publishDataSet();
    public abstract void replicateDataSet();
    public abstract void updateDataSet();
    public abstract void listDataSets();

    //Optional Methods
    public abstract void replicateAllDataSets();
    public abstract void listDataSetUpdates();
    public abstract void isUpdatedDataSet();
}
