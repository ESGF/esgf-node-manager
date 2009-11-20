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

       This is a very simple object to encapsulate information between
       remote client and service

*****************************************************************/
package esg.common.service;

public class ESGRemoteEvent implements java.io.Serializable {

    //Okay, I know ideally these should be ENUMS but I am not sure on
    //how enums serialize with hessian, so I am going to play it safe
    //before I start experimenting.  Just something to get this setup
    //rolling. Besides this is old school flava right here! :-)

    public static final int NOOP       = 1;
    public static final int REGISTER   = 2;
    public static final int UNREGISTER = 4;
    public static final int NOTIFY     = 8;
    public static final int HEALTH     = 16;
    public static final int STATUS     = 32;

    private String source  = null;
    private int    messageType = -1;
    private Object payload = null;

    public ESGRemoteEvent(String source, int messageType, Object payload) {
	this.source  = source;
	this.messageType = messageType;
	this.payload = payload;
    }

    public ESGRemoteEvent(String source, int messageType) {
	this(source,messageType,null);
    }

    public ESGRemoteEvent(String source) { this(source,NOOP,null); }


    public String getSource()  { return source;  }
    public int    getMessageType() { return messageType; }
    public Object getPayload() { return payload; }
    public void setPayload(Object obj) { this.payload = obj; }

    public String toString() { return "s:["+source+"] m:["+messageType+"] p:["+payload+"]"; }
    
}

