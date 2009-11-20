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

public class ESGJoinEvent extends ESGEvent {

    public static final boolean JOIN   = true;
    public static final boolean UNJOIN = false;

    private DataNodeComponent joiner = null;
    private boolean direction = false;

    public ESGJoinEvent(Object source, 
			String message, 
			DataNodeComponent joiner, boolean direction) { 
	super(source,message); 
	this.joiner = joiner;
	this.direction = direction;
    }

    public DataNodeComponent getJoiner() { return joiner; }
    public boolean hasJoined() { return direction == JOIN;   }
    public boolean hasLeft()   { return direction == UNJOIN; }

    public String toString() {
	return super.toString()+" joiner:["+joiner.getClass().getName()+"] action:["+(direction ? "JOIN" : "UNJOIN")+"]";

    }

    
}
