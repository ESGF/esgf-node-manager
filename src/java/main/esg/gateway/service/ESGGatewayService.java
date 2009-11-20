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

import esg.common.service.ESGRemoteEvent;

public interface ESGGatewayService {
    
    public boolean ping();
    public void register(ESGRemoteEvent evt);

}
