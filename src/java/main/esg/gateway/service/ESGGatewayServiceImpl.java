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

import esg.common.service.ESGRemoteEvent;

public class ESGGatewayServiceImpl implements ESGGatewayService {

    private static final Log log = LogFactory.getLog(ESGGatewayServiceImpl.class);
    
    public boolean ping() {
	log.trace("Gateway service got \"ping\"");
	return true;
    }
    
    public void register(ESGRemoteEvent evt) {
	log.trace("Gateway service got \"register\" call with event: ["+evt+"]");
    }

}
