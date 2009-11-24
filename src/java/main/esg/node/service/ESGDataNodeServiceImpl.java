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

        ----------------------------------------------------
      THIS CLASS RECEIVES ALL *INGRESS* CALLS *FROM* GATEWAY(s)!!
        ----------------------------------------------------
           (I don't think I can make this any clearer)


*****************************************************************/
package esg.node.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.core.ESGDataNodeManager;
import esg.node.core.AbstractDataNodeComponent;
import esg.node.connection.ESGConnectionManager;
import esg.common.service.ESGRemoteEvent;

public class ESGDataNodeServiceImpl extends AbstractDataNodeComponent 
    implements ESGDataNodeService {

    private static final Log log = LogFactory.getLog(ESGDataNodeServiceImpl.class);
    private ESGDataNodeManager datanodeMgr = null;
    private ESGConnectionManager connMgr = null;
    

    public ESGDataNodeServiceImpl() {
	log.info("ESGDataNodeServiceImpl instantiated...");
	setMyName("DataNodeService");
	init();
    }

    //Bootstrap the entire system...
    public void init() {
	datanodeMgr = new ESGDataNodeManager();
	connMgr = new ESGConnectionManager();
	
	datanodeMgr.registerComponent(this);
	datanodeMgr.registerComponent(connMgr);

	datanodeMgr.init();
    }

    //Remote (ingress) calls to method...
    public boolean ping() { 
	log.trace("DataNode service got \"ping\"");
	return amAvailable(); 
    }

    //TODO: Think about the performance implications of having a synchronious return value
    //Remote (ingress) calls to method...
    public boolean notify(ESGRemoteEvent evt) {
	log.trace("DataNode service got \"notify\" call with event: ["+evt+"]");
	if(!amAvailable()) {
	    log.warn("Dropping ingress notification event on the floor, I am NOT available. ["+evt+"]");
	    return false;
	}
	
	//TODO: Grab this remote event, inspect the payload and stuff
	//it into an esg event and fire it off to listeners to handle.
	//Example... if the payload was the Catalog xml then JAXB it
	//and send the object form as the payload for others to use
	//(for example)

	return true;
    }


    //We will consider this object not valid if there are no gateways
    //to communicate with. That would be because:
    //1) There are no gateway proxy objects available for us to use
    //2) If the gateway proxy objects we DO have are no longer valid
    //(we just need one to be valid for us to be available) 

    //NOTE: review this policy! *for now* good enough as we are only
    //planning on having a 1:1 between data node and gateways... but
    //we could imagine having just one gateway that is valid holding
    //open the door for us to be DOS-ed by folks maliciously sending
    //us huge events that flood our system.
    private boolean amAvailable() { 
	boolean ret = false;
	ret = connMgr.amAvailable(); 
	log.trace("amAvailable() -> "+ret);
	return ret;
    }
    
    public void unregister() {
	//Intentionally a NO-OP
	log.warn("Balking... What does it mean to unregister the service itself?... exactly... no can do ;-)");
    }

}
