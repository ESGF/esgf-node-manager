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

import java.util.EventObject;

public class ESGEvent extends EventObject {

    private String message="";

    public ESGEvent(Object source) { super(source); }
    public ESGEvent(Object source, String message) {
	super(source);
	this.message = message;
    }

    public String getMessage() { return message; }

    public String toString() {
        return "Event:["+this.getClass().getName()+"] s:["+source+"] msg:["+message+"]";
    }

    
}
