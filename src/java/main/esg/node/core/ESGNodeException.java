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

import esg.common.ESGException;

public class ESGNodeException extends ESGException {
    public ESGNodeException() { }
    public ESGNodeException(String message) { super(message); }
    public ESGNodeException(String message, Throwable cause) { super(message,cause); }
    public ESGNodeException(Throwable cause) { super(cause); }
}
