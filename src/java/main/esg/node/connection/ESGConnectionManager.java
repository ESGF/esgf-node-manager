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

      This class also MANAGES peer proxy object(s)
      (ex:BasicPeer) that communicate OUT (egress) to the
      peer(s).

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
import esg.node.core.ESGPeerListener;
import esg.node.core.ESGDataNodeManager;
import esg.node.core.AbstractDataNodeComponent;
import esg.node.core.AbstractDataNodeManager;
import esg.node.core.ESGEvent;
import esg.node.core.ESGEventHelper;
import esg.node.core.ESGJoinEvent;
import esg.node.core.ESGPeerEvent;
import esg.node.core.ESGPeer;


public class ESGConnectionManager extends AbstractDataNodeComponent implements ESGPeerListener {

    private static final Log log = LogFactory.getLog(ESGConnectionManager.class);
    
    private Map<String,ESGPeer> peers = null;
    private Map<String,ESGPeer> unavailablePeers = null;
    

    public ESGConnectionManager(String name) {
	super(name);
	log.info("ESGConnectionManager instantiated...");
	peers = Collections.synchronizedMap(new HashMap<String,ESGPeer>());
	unavailablePeers = Collections.synchronizedMap(new HashMap<String,ESGPeer>());
	init();
    }
    
    //Bootstrap the rest of the subsystems... (ESGDataNodeServiceImpl really bootstraps)
    public void init() {
	//NOTE:
	//Just to make sure we have these guys if we decide to re-register.
	//since we did such a good job cleaning things out with we unregister.
	//Once could imagine wanting to re-establish the connection manager.
	if(peers == null) peers = Collections.synchronizedMap(new HashMap<String,ESGPeer>());
	if(unavailablePeers == null) unavailablePeers = Collections.synchronizedMap(new HashMap<String,ESGPeer>());
	
	//periodicallyPingToPeers();
	//periodicallyRegisterToPeers(); //zoiks: test method (not permanent)
    }

    //--------------------------------------------
    // Status Methods
    //--------------------------------------------
    
    public int numAvailablePeers() { return peers.size(); }
    public int numUnavailablePeers() { return unavailablePeers.size(); }

    //--
    //Communication maintenance... (this could become arbitrarily
    //complex... leasing et. al. but for now we will just do periodic
    //pings, and simple registration)
    //--
    
    private void periodicallyPingToPeers() {
	log.trace("Launching ping timer...");
	Timer timer = new Timer();
	timer.schedule(new TimerTask() { 
		public final void run() {
		    ESGConnectionManager.this.pingToPeers();
		}
	    },0,5*1000);
    }
    private void pingToPeers() {
	Collection<? extends ESGPeer> peers_ = peers.values();
	for(ESGPeer peer: peers_) {
	    peer.ping();
	}
    }

    //----
    //Test code.
    //----
    private void periodicallyRegisterToPeers() {
	log.trace("Launching registration timer...");
	Timer timer = new Timer();

	//This will transition from active map to inactive map
	timer.schedule(new TimerTask() { 
		public final void run() {
		    ESGConnectionManager.this.registerToPeers();
		}
	    },0,10*1000);
    }
    private void registerToPeers() {
	Collection<? extends ESGPeer> peers_ = peers.values();
	for(ESGPeer peer: peers_) {
	    peer.registerToPeer();
	}
    }


    //We will consider this communications closed (essentially making
    //this unavailable for ingress communication) if there are no
    //peers to communicate with. That would be because:
    //1) There are no peer proxy objects available for us to use
    //2) If the peer proxy objects we DO have are no longer valid
    //(we just need one to be valid for us to be available) 

    //NOTE (TODO): review this policy! *for now* good enough as we are
    //only planning on having a 1:1 between data node and peers... but
    //we could imagine that it would only take just having one peer
    //that is valid holding open the door for us to be DOS-ed by folks
    //maliciously sending us huge events that flood our system.
    public boolean amAvailable() {
	boolean amAvailable = false;
	boolean haveValidPeerProxies = false;
	if (peers.isEmpty()) { amAvailable = false; return false; }
	Collection<? extends ESGPeer> peers_ = peers.values();
	for(ESGPeer peer: peers_) {
	    haveValidPeerProxies |= peer.isAvailable();
	}
	amAvailable = (amAvailable || haveValidPeerProxies );
	return amAvailable;
    }
    
    public void unregister() {
	//TODO: Be nice and send all the peers termination events
	//clear out my datastrutures of node proxies
	peers.clear(); peers = null; //gc niceness
	unavailablePeers.clear(); unavailablePeers = null; //gc niceness
	super.unregister();
    }

    //--------------------------------------------
    //Event handling...
    //--------------------------------------------

    public boolean handleESGQueuedEvent(ESGEvent event) {
	log.trace("["+getName()+"]:["+this.getClass().getName()+"]: Got A QueuedEvent!!!!: "+event);

	ESGRemoteEvent rEvent=null;
	String targetAddress = null;
	ESGPeer targetPeer = null;
	
	//TODO: pick it up from here to launch egress / return calls...
	if((rEvent = event.getRemoteEvent()) == null) {
	    log.warn("The encountered event does not contain a remote event, which is needed for egress routing [event dropped]");
	    event = null; //gc hint!
	    return false;
	}
	
	if((targetPeer = peers.get(targetAddress=rEvent.getSource())) == null) {
	    targetPeer = unavailablePeers.get(targetAddress);
	    log.error("Specified peer named by ["+targetAddress+"] is "+
		      ((targetPeer == null) ? "unknown " : "unavailable ")+"[event dropped]");
	    event = null; //gc hint!
	    return false;
	}


	//TODO: have the ESGEventHelper create the remote event properly (TTL, etc...)
	targetPeer.handleESGRemoteEvent(ESGEventHelper.createOutboundEvent(event));
	event = null; //gc hint!
	
	return true;
    }


    //(for JOIN events that happens in the ESGDataNodeManager via it's superclass AbstractDataNodeManager)
    public void handleESGEvent(ESGEvent esgEvent) {
	//we only care about join events
	if(!(esgEvent instanceof ESGJoinEvent)) return;

	ESGJoinEvent event = (ESGJoinEvent)esgEvent;
	
	//we only care bout ESGPeers joining
	if(!(event.getJoiner() instanceof ESGPeer)) return;

	//manage the data structure for peer 'stubs' locally while
	//object is a participating managed component.
	if(event.hasJoined()) {
	    log.trace("Detected That A Peer Component Has Joined: "+event.getJoiner().getName());
	    ESGPeer peer = (ESGPeer)event.getJoiner();
	    String peerURL = peer.getServiceURL();
	    if(peerURL != null) {
		peers.put(peerURL,peer);
		peer.notifyToPeer();
	    }else{
		log.warn("Dropping "+peer+"... (no null service urls accepted)");
	    }
	}else {
	    log.trace("Detected That A Peer Component Has Left: "+event.getJoiner().getName());
	    peers.remove(event.getJoiner().getName());
	    unavailablePeers.remove(event.getJoiner().getName());
	}
	log.trace("Number of active service managed peers = "+peers.size());
    }

    //--------------------------------------------
    //Special Event handling channel... 
    //(for peer events directly from managed peer stub objects)
    //--------------------------------------------
    public void handlePeerEvent(ESGPeerEvent evt) {
	log.trace("Got Peer Event: "+evt);

	//TODO: I know I know... use generics in the event!!! (todo)
	ESGPeer peer = (ESGPeer)evt.getSource();
	switch(evt.getEventType()) {
	case ESGPeerEvent.CONNECTION_FAILED:
	case ESGPeerEvent.CONNECTION_BUSY:
	    log.trace("Transfering from active -to-> inactive list");
	    if(peers.remove(peer.getName()) != null) {
		unavailablePeers.put(peer.getName(),peer);
	    }
	    break;
	case ESGPeerEvent.CONNECTION_AVAILABLE:
	    log.trace("Transfering from inactive -to-> active list");
	    if(unavailablePeers.remove(peer.getName()) != null) {
		peers.put(peer.getName(),peer);
	    }
	    break;
	default:
	    break;
	}
	log.trace("Available Peers: ["+peers.size()+"] Unavailable: ["+unavailablePeers.size()+"]");	
    }
    
}