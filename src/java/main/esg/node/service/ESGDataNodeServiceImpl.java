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

import esg.common.service.ESGRemoteEvent;
import esg.node.core.ESGDataNodeManager;

public class ESGDataNodeServiceImpl implements ESGDataNodeService {

    private static final Log log = LogFactory.getLog(ESGDataNodeServiceImpl.class);

    private ESGDataNodeManager mgr = null;

    public ESGDataNodeServiceImpl() {
	log.info("ESGDataNodeServiceImpl instantiated...");
	mgr.init();
    }

    public boolean ping() { 
	log.trace("DataNode service got \"ping\"");
	return true; 
    }

    //TODO: Think about the performance implications of having a synchronious return value
    public boolean notify(ESGRemoteEvent evt) {
	log.trace("DataNode service got \"notify\" call with event: ["+evt+"]");
	return true;
    }
    
    
}
