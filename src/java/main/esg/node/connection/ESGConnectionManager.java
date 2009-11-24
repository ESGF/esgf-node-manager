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
package esg.node.connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;

import esg.common.service.ESGRemoteEvent;
import esg.node.core.ESGDataNodeManager;
import esg.node.core.AbstractDataNodeComponent;
import esg.node.core.AbstractDataNodeManager;
import esg.node.core.ESGEvent;
import esg.node.core.ESGJoinEvent;
import esg.node.core.Gateway;


public class ESGConnectionManager extends AbstractDataNodeComponent {

    private static final Log log = LogFactory.getLog(ESGConnectionManager.class);

    //----
    private Map<String,Gateway> gateways = null;
    private Map<String,Gateway> unavailableGateways = null;
    

    public ESGConnectionManager() {
	log.info("ESGConnectionManager instantiated...");
	setMyName("ESGConnectionManager");
	gateways = Collections.synchronizedMap(new HashMap<String,Gateway>());
	unavailableGateways = Collections.synchronizedMap(new HashMap<String,Gateway>());
    }

    //Bootstrap the entire system...
    public void init() {
	//NOTE:
	//Just to make sure we have these guys if we decide to re-register.
	//since we did such a good job cleaning things out with we unregister.
	//Once could imagine wanting to re-establish the connection manager.
	if(gateways == null) gateways = Collections.synchronizedMap(new HashMap<String,Gateway>());
	if(unavailableGateways == null) unavailableGateways = Collections.synchronizedMap(new HashMap<String,Gateway>());
	
	periodicallyPingGateways();
	periodicallyRegisterToGateways(); //zoiks: test method (not permanent)
    }

    //--------------------------------------------
    // Status Methods
    //--------------------------------------------
    
    public int numAvailableGateways() { return gateways.size(); }
    public int numUnavailableGateways() { return unavailableGateways.size(); }

    //--
    //Communication maintenance... (this could become arbitrarily
    //complex... leasing et. al. but for now we will just do periodic
    //pings, and simple registration)
    //--
    
    private void periodicallyPingGateways() {
	log.trace("launching ping timer...");
	Timer timer = new Timer();

	//This will transition from active map to inactive map
	timer.schedule(new TimerTask() { 
		public final void run() {
		    ESGConnectionManager.this.pingGateways(ESGConnectionManager.this.gateways,
							     ESGConnectionManager.this.unavailableGateways);

		}
	    },0,5*1000);
	
	//This will transition from inactive map to active map
	timer.schedule(new TimerTask() { 
		public final void run() {
		    //only try to transfer nodes if there are nodes that are unavailable :-)
		    if(!ESGConnectionManager.this.unavailableGateways.isEmpty()) {
			log.trace("Noticed that there are some unavailable gateways... checking on them ["+unavailableGateways.size()+"]");
			ESGConnectionManager.this.pingGateways(ESGConnectionManager.this.unavailableGateways,
								 ESGConnectionManager.this.gateways);
			log.trace("Available Gateways: ["+gateways.size()+"] Unavailable: ["+unavailableGateways.size()+"]");
			
			//Okay, I have no connections!? Time to poll till I get at least one!
			if ((gateways.size() == 0) && (unavailableGateways.size() == 0)) {
			    List<Gateway> gatewaysList = ((AbstractDataNodeManager)ESGConnectionManager.this.getDataNodeManager()).getGateways();
			    if(!gatewaysList.isEmpty()) {
				for(Gateway gateway : gatewaysList) {
				    ESGConnectionManager.this.gateways.put(gateway.getName(),gateway);
				}
			    }
			}
		    }
		}
		
	    },0,10*1000);
    }
    
    //Tries to contact gateways... 
    //if not moves gateways proxies from map A --to--> B
    private void pingGateways(Map<String,Gateway> gatewaysA, 
			      Map<String,Gateway> gatewaysB) {
	boolean pingstat = false;
	Collection<? extends Gateway> gateways = gatewaysA.values();
	for(Gateway gateway: gateways) {
	    pingstat = gateway.ping();
	    log.trace("Available stat ?= "+gateway.isAvailable());
	    if(!gateway.isAvailable()) {
		log.trace("moving from one list to other");
		gatewaysA.remove(gateway.getName());
		gatewaysB.put(gateway.getName(),gateway);
	    }
	}
	log.trace("Ping return value = "+pingstat);
	log.trace("Available Gateways: ["+gateways.size()+"] Unavailable: ["+unavailableGateways.size()+"]");	
    }

    //----
    //Test code.
    //----
    private void periodicallyRegisterToGateways() {
	log.trace("launching ping timer...");
	Timer timer = new Timer();

	//This will transition from active map to inactive map
	timer.schedule(new TimerTask() { 
		public final void run() {
		    ESGConnectionManager.this.registerToGateways();
		}
	    },0,10*1000);
    }
    private void registerToGateways() {
	Collection<? extends Gateway> gateways_ = gateways.values();
	for(Gateway gateway: gateways_) {
	    gateway.registerToGateway();
	}
    }


    //We will consider this communications closed (essentially making
    //this unavailable for ingress communication) if there are no
    //gateways to communicate with. That would be because:
    //1) There are no gateway proxy objects available for us to use
    //2) If the gateway proxy objects we DO have are no longer valid
    //(we just need one to be valid for us to be available) 

    //NOTE: review this policy! *for now* good enough as we are only
    //planning on having a 1:1 between data node and gateways... but
    //we could imagine that it would only take just having one gateway
    //that is valid holding open the door for us to be DOS-ed by folks
    //maliciously sending us huge events that flood our system.
    private boolean amAvailable() {
	boolean amAvailable = false;
	boolean haveValidGatewayProxies = false;
	if (gateways.isEmpty()) { amAvailable = false; return false; }
	Collection<? extends Gateway> gateways_ = gateways.values();
	for(Gateway gateway: gateways_) {
	    haveValidGatewayProxies |= gateway.isAvailable();
	}
	amAvailable = (amAvailable || haveValidGatewayProxies );
	return amAvailable;
    }
    
    public void unregister() {
	//clear out my datastrutures of node proxies
	gateways.clear(); gateways = null;
	unavailableGateways.clear(); unavailableGateways = null;
    }

    //--------------------------------------------
    //Event handling... (for join events)
    //--------------------------------------------
    public void esgActionPerformed(ESGEvent esgEvent) {
	//we only care about join events
	if(!(esgEvent instanceof ESGJoinEvent)) return;

	ESGJoinEvent event = (ESGJoinEvent)esgEvent;
	
	//we only care bout Gateways joining
	if(!(event.getJoiner() instanceof Gateway)) return;

	//manage the data structure for gateway 'stubs' locally while
	//object is a participating managed component.
	if(event.hasJoined()) {
	    log.trace("Detected That A Gateway Component Has Joined: "+event.getJoiner().getName());
	    gateways.put(event.getJoiner().getName(),(Gateway)event.getJoiner());
	}else {
	    log.trace("Detected That A Gateway Component Has Left: "+event.getJoiner().getName());
	    gateways.remove(event.getJoiner().getName());
	    unavailableGateways.remove(event.getJoiner().getName());
	}
	log.trace("Number of active service managed gateways = "+gateways.size());
    }
    

    //----

}