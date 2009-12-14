/***************************************************************************
*                                                                          *
*  Organization: Lawrence Livermore National Lab (LLNL)                    *
*   Directorate: Computation                                               *
*    Department: Computing Applications and Research                       *
*      Division: S&T Global Security                                       *
*        Matrix: Atmospheric, Earth and Energy Division                    *
*       Program: PCMDI                                                     *
*       Project: Earth Systems Grid (ESG) Data Node Software Stack         *
*  First Author: Gavin M. Bell (gavin@llnl.gov)                            *
*                                                                          *
****************************************************************************
*                                                                          *
*   Copyright (c) 2009, Lawrence Livermore National Security, LLC.         *
*   Produced at the Lawrence Livermore National Laboratory                 *
*   Written by: Gavin M. Bell (gavin@llnl.gov)                             *
*   LLNL-CODE-420962                                                       *
*                                                                          *
*   All rights reserved. This file is part of the:                         *
*   Earth System Grid (ESG) Data Node Software Stack, Version 1.0          *
*                                                                          *
*   For details, see http://esg-repo.llnl.gov/esg-node/                    *
*   Please also read this link                                             *
*    http://esg-repo.llnl.gov/LICENSE                                      *
*                                                                          *
*   * Redistribution and use in source and binary forms, with or           *
*   without modification, are permitted provided that the following        *
*   conditions are met:                                                    *
*                                                                          *
*   * Redistributions of source code must retain the above copyright       *
*   notice, this list of conditions and the disclaimer below.              *
*                                                                          *
*   * Redistributions in binary form must reproduce the above copyright    *
*   notice, this list of conditions and the disclaimer (as noted below)    *
*   in the documentation and/or other materials provided with the          *
*   distribution.                                                          *
*                                                                          *
*   Neither the name of the LLNS/LLNL nor the names of its contributors    *
*   may be used to endorse or promote products derived from this           *
*   software without specific prior written permission.                    *
*                                                                          *
*   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS    *
*   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT      *
*   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS      *
*   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL LAWRENCE    *
*   LIVERMORE NATIONAL SECURITY, LLC, THE U.S. DEPARTMENT OF ENERGY OR     *
*   CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,           *
*   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT       *
*   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF       *
*   USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND    *
*   ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,     *
*   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT     *
*   OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF     *
*   SUCH DAMAGE.                                                           *
*                                                                          *
***************************************************************************/

/**
   Description:

      This class also MANAGES gateway proxy object(s)
      (ex:BasicGateway) that communicate out to the gateway(s).

**/
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
import esg.node.core.ESGGatewayListener;
import esg.node.core.ESGDataNodeManager;
import esg.node.core.AbstractDataNodeComponent;
import esg.node.core.AbstractDataNodeManager;
import esg.node.core.ESGEvent;
import esg.node.core.ESGJoinEvent;
import esg.node.core.ESGGatewayEvent;
import esg.node.core.Gateway;


public class ESGConnectionManager extends AbstractDataNodeComponent implements ESGGatewayListener {

    private static final Log log = LogFactory.getLog(ESGConnectionManager.class);

    private Map<String,Gateway> gateways = null;
    private Map<String,Gateway> unavailableGateways = null;
    

    public ESGConnectionManager() {
	log.info("ESGConnectionManager instantiated...");
	setMyName("ESGConnectionManager");
	gateways = Collections.synchronizedMap(new HashMap<String,Gateway>());
	unavailableGateways = Collections.synchronizedMap(new HashMap<String,Gateway>());
	init();
    }

    //Bootstrap the rest of the subsystems... (ESGDataNodeServiceImpl really bootstraps)
    public void init() {
	//NOTE:
	//Just to make sure we have these guys if we decide to re-register.
	//since we did such a good job cleaning things out with we unregister.
	//Once could imagine wanting to re-establish the connection manager.
	if(gateways == null) gateways = Collections.synchronizedMap(new HashMap<String,Gateway>());
	if(unavailableGateways == null) unavailableGateways = Collections.synchronizedMap(new HashMap<String,Gateway>());
	
	periodicallyPingToGateways();
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
    
    private void periodicallyPingToGateways() {
	log.trace("Launching ping timer...");
	Timer timer = new Timer();
	timer.schedule(new TimerTask() { 
		public final void run() {
		    ESGConnectionManager.this.pingToGateways();
		}
	    },0,5*1000);
    }
    private void pingToGateways() {
	Collection<? extends Gateway> gateways_ = gateways.values();
	for(Gateway gateway: gateways_) {
	    gateway.ping();
	}
    }

    //----
    //Test code.
    //----
    private void periodicallyRegisterToGateways() {
	log.trace("Launching ping timer...");
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
    public boolean amAvailable() {
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
	super.unregister();
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

    //--------------------------------------------
    //Event handling... (for gateway events)
    //--------------------------------------------
    public void handleGatewayEvent(ESGGatewayEvent evt) {
	log.trace("Got Gateway Event: "+evt);

	//TODO: I know I know... use generics in the event!!! (todo)
	Gateway gway = (Gateway)evt.getSource();
	switch(evt.getEventType()) {
	case ESGGatewayEvent.CONNECTION_FAILED:
	case ESGGatewayEvent.CONNECTION_BUSY:
	    log.trace("Transfering from active -to-> inactive list");
	    if(gateways.remove(gway.getName()) != null) {
		unavailableGateways.put(gway.getName(),gway);
	    }
	    break;
	case ESGGatewayEvent.CONNECTION_AVAILABLE:
	    log.trace("Transfering from inactive -to-> active list");
	    if(unavailableGateways.remove(gway.getName()) != null) {
		gateways.put(gway.getName(),gway);
	    }
	    break;
	default:
	    break;
	}
	log.trace("Available Gateways: ["+gateways.size()+"] Unavailable: ["+unavailableGateways.size()+"]");	
    }
    
}