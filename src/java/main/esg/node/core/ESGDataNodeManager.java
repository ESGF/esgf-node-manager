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

public class ESGDataNodeManager extends AbstractDataNodeManager {

    private static Log log = LogFactory.getLog(ESGDataNodeManager.class);
    //TODO: the logic of the system, pulling in the components and
    //managing them.
    public ESGDataNodeManager() { }

    public void init() {
	log.info("Initializing Data Node Manager...");
    }

}