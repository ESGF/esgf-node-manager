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

      These are methods that will be exposed, for calling by remote
      callers, namely the GATEWAY!

*****************************************************************/
package esg.node.service;

import esg.common.service.ESGRemoteEvent;

public interface ESGDataNodeService {

    public boolean ping();
    public boolean notify(ESGRemoteEvent evt);

}
