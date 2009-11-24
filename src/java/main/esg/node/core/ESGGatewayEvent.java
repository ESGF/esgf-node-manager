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

public class ESGGatewayEvent extends ESGEvent{

    //TODO: YES YES YES... I KNOW... I'll make these enums later! ;-)
    public static final int CONNECTION_FAILED = 2;
    public static final int CONNECTION_BUSY = 4;
    public static final int CONNECTION_AVAILABLE = 8;

    private int eventType = -1;
    
    public ESGGatewayEvent(Object source, String message, int eventType) { 
	super(source,message); 
	this.eventType = eventType;
    }
    public ESGGatewayEvent(Object source,int eventType) { 
	this(source,null,eventType); 
    }
    
    public int getEventType() { return eventType; }
    public String toString() { return "s:["+source+"] t:["+eventType+"]"; }
}