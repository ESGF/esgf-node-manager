/***************************************************************************
*                                                                          *
*  Organization: Lawrence Livermore National Lab (LLNL)                    *
*   Directorate: Computation                                               *
*    Department: Computing Applications and Research                       *
*      Division: S&T Global Security                                       *
*        Matrix: Atmospheric, Earth and Energy Division                    *
*       Program: PCMDI                                                     *
*       Project: Earth Systems Grid Federation (ESGF) Data Node Software   *
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
*   Earth System Grid Federation (ESGF) Data Node Software Stack           *
*                                                                          *
*   For details, see http://esgf.org/esg-node/                             *
*   Please also read this link                                             *
*    http://esgf.org/LICENSE                                               *
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
import java.util.Properties;

import esg.common.Utils;
import esg.common.util.ESGFProperties;
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
import esg.node.core.BasicPeer;

import esg.common.generated.registration.*;
import esg.node.components.registry.RegistryUpdateDigest;

public class ESGConnectionManager extends AbstractDataNodeComponent implements ESGPeerListener {

    private static final Log log = LogFactory.getLog(ESGConnectionManager.class);

    private Properties props = null;

    private Map<String,ESGPeer> peers = null;
    private Map<String,ESGPeer> unavailablePeers = null;
    private RegistryUpdateDigest lastRud = null;
    private ESGPeer defaultPeer = null;

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
        
        try{
            props = new ESGFProperties();
            //periodicallyPingToPeers();
            periodicallyRegisterToPeers();
        }catch(java.io.IOException e) {
            System.out.println("Damn, ESGConnectionManager can't fire up... :-(");
            log.error(e);
        }
    }

    //--------------------------------------------
    // Status Methods
    //--------------------------------------------
    
    public int numAvailablePeers() { return peers.size(); }
    public int numUnavailablePeers() { return unavailablePeers.size(); }

    
    
    private void periodicallyPingToPeers() {
        log.trace("Launching ping timer...");
        Timer timer = new Timer();
        timer.schedule(new TimerTask() { 
                public final void run() {
                    ESGConnectionManager.this.pingToPeers();
                }
            },5*1000,30*1000);
    }
    private void pingToPeers() {
        Collection<? extends ESGPeer> peers_ = peers.values();
        for(ESGPeer peer: peers_) {
            peer.ping();
        }
    }


    private void periodicallyRegisterToPeers() {
        log.trace("Launching connection manager's registration push timer...");
        long delay  = Long.parseLong(props.getProperty("conn.mgr.initialDelay","10"));
        long period = Long.parseLong(props.getProperty("conn.mgr.period","300"));
        log.trace("connection registration delay:  "+delay+" sec");
        log.trace("connection registration period: "+period+" sec");
	
        Timer timer = new Timer();
        
        //This will transition from active map to inactive map
        timer.schedule(new TimerTask() { 
                public final void run() {
                    if (lastRud == null) return;
                    sendOutRegistryState();
                }
            },delay*1000,period*1000);
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
    
    //Cached last registry data and checksum in lastRud.
    //Send out the same info to another random pair of neighbors.
    private synchronized boolean sendOutRegistryState() {
        log.info("Sending out registry state to peers...");
        //Bootstrap condition...
        if(lastRud == null && defaultPeer != null) {
            //Damnit, I didn't want this dependency..!!!
            esg.node.components.registry.RegistrationGleaner ephemeralGleaner = new esg.node.components.registry.RegistrationGleaner();
            String registration = ephemeralGleaner.loadMyRegistration().toString();
            defaultPeer.handleESGRemoteEvent(new ESGRemoteEvent(ESGEventHelper.getMyServiceUrl(),
                                                                ESGRemoteEvent.REGISTER,
                                                                registration,
                                                                ephemeralGleaner.getMyChecksum(),
                                                                Utils.nextSeq(),
                                                                0));
            log.info("Bootstrapping... sending my registration to: "+((List<ESGPeer>)peers.values()).get(0).getServiceURL());
            log.info("My Registration is:"+ registration);
            ephemeralGleaner = null; //gc niceness.
            return true;
        }
        //delagate through with no so "new" state :-)
        return this.sendOutNewRegistryState(this.lastRud.xmlDocument(),this.lastRud.xmlChecksum());
    }
    
    //Helper method containing the details of the Gossip protocol dispatch logic
    //Basically - choose two random peers (that are not me) to send my state to.
    private synchronized boolean sendOutNewRegistryState(String xmlDocument, String xmlChecksum) {

        ESGRemoteEvent myRegistryState = new ESGRemoteEvent(ESGEventHelper.getMyServiceUrl(),
                                                            ESGRemoteEvent.REGISTER,
                                                            xmlDocument,
                                                            xmlChecksum,
                                                            Utils.nextSeq(),
                                                            0);

        int networkSizeLimit = 10000; //Essentially the total number of nodes to randomly choose from is between 0 and networkSizeLimit+1
        int retries = 3; //how many times to try to get this event to [branchFactor] peers
        int numDispatchedPeers = 0; //how many peers have successfully had events sent to them.
        int branchFactor = 2; //how many peers we need to send to on the next hop
        int idx = -1; // index into list of peer (stub) objects
        int lastIdx = -1; //the last index value that you choose.
        int rechooseLimit = 4; //number of times to select a peer that you haven't selected before

        for(int i=0; i < retries; i++)  {
            //It is possible, to randomly keep getting the same index
            //number again and again to prevent that we try up to
            //[rechooseLimit] times to select another peer If we hit
            //the limit we re-try again up to [retries] times.
            
            //So if you are tremendously unlucky or in a situation
            //where there is less than 1 other peer to send to, you
            //will do this reselection a bounded number of times.
            //Also if you have selected [branchFactor] distinct number
            //of peers to dispatch to but they both were "bad" then
            //you
            int rechooseIndexCount = 0; 
            while((numDispatchedPeers < branchFactor) || (rechooseIndexCount > rechooseLimit)) {
                //Randomly select a peer to send our state to...
                idx = ((int)(Math.random()*networkSizeLimit)) % peers.size();

                //Notice that the following single step check works
                //well because our branching factor is 2 otherwise
                //we'd have to check in a SET of previously selected
                //values or something
                if(lastIdx == idx) { rechooseIndexCount++; continue;}

                //NOTE: I can't check for "success" of the message
                //getting to the peer so there could be the case where
                //my bad luck has choosen two dead beat peers and I
                //would not know and thus the message propagation
                //would stop dead in its tracks.  Though, if I did
                //such a thing the peers would send a signal to purge
                //themselves from the active list, but still be in the
                //node managers peer list.... so I thinkI need to
                //recant the preceding paragraph.  I do want some
                //reasonable notion that I am not sending messages to
                //dead machines.... Okay I have convinced myself to
                //use the local active data structure...  Rule of
                //thumb, keep things local to this object as much as
                //you can. And try to stay on the stack not heap (yes,
                //in Java it's hard)

                ESGPeer chosenPeer = ((List<ESGPeer>)peers.values()).get(idx);
                log.trace("Selected: "+chosenPeer.getName());
                chosenPeer.handleESGRemoteEvent(myRegistryState);
                lastIdx = idx;
                numDispatchedPeers++;
            }
            if(numDispatchedPeers >= branchFactor) break;
        }
        return (numDispatchedPeers > 1); //I was at least able to get one off!
    }

    //--------------------------------------------
    //Event handling...
    //--------------------------------------------

    public synchronized boolean handleESGQueuedEvent(ESGEvent event) {
        log.trace("["+getName()+"]:["+this.getClass().getName()+"]: Got A QueuedEvent!!!!: "+event);

        if(event.getData() instanceof RegistryUpdateDigest) {
            log.trace("Getting update information regarding internal representation of the federation");
            RegistryUpdateDigest rud = (RegistryUpdateDigest)event.getData();

            //Add all the newly discovered peers that I don't already
            //know first hand are active... but they are not fully "available"
            //yet.
            ESGPeer peer = null;
            String peerServiceUrl = null;
            for(Node node : rud.updatedNodes()) {
                
                //Scenario A:
                //This was the first way... Where we enforced the service url... maybe not a bad idea?
                //peer = peers.get(peerServiceUrl = Utils.asServiceUrl(node.getHostname()));

                //Scenario B: Get the service endpoint advertised by the peer in their registration...
                //Check this node to see if it has an entry for a node manager... (required);
                try{
                    peerServiceUrl = node.getNodeManager().getEndpoint();
                }catch (Throwable t) { 
                    log.warn(node.getHostname()+" does not seem to be running a node manager thus, can't be a peer... dropping'em"); 
                    continue;
                }
                
                peer = peers.get(peerServiceUrl);

                //If we don't have you in our peer list then we'll add
                //you... (indirectly) The act of registering this new
                //peer fires off a join event which is caught here and
                //handled below in the implementation of
                //this.handleESGEvent where the peer is then added to
                //the peers datastructure (map).
                try{
                    if (peer == null) getDataNodeManager().registerPeer(new BasicPeer(peerServiceUrl, ESGPeer.PEER));
                }catch(java.net.MalformedURLException e) {log.error(e); }
            }
            lastRud=rud;
            return sendOutNewRegistryState(rud.xmlDocument(), rud.xmlChecksum());
        }
        
        //--------------------
        //Routing of events...
        //--------------------

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

        rEvent.decTTL();
        if(rEvent.isValid()) {
            targetPeer.handleESGRemoteEvent(ESGEventHelper.createProxiedOutboundEvent(rEvent));
        }
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
            log.trace("6)) Detected That A Peer Component Has Joined: "+event.getJoiner().getName());
            ESGPeer peer = (ESGPeer)event.getJoiner();
            String peerURL = peer.getServiceURL();
            if(peerURL != null) {
        
                //Have the newly joined peer (stub) attempt to contact
                //it's endpoint to establish notification.  By adding
                //"this" connection manager, the peer stub can now
                //send us an event if the notify call to the endpoint
                //was successful or not.(see handlePeerEvent below)
                peer.addPeerListener(this);
                peers.put(peer.getName(),peer);
                if (peer.getPeerType() == ESGPeer.DEFAULT_PEER) defaultPeer = peer;
        
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
            log.trace("Got ESGPeerEVent.CONNECTION_FAILED from: "+peer.getName());
        case ESGPeerEvent.CONNECTION_BUSY:
            log.trace("Got ESGPeerEVent.CONNECTION_BUSY from: "+peer.getName());
            if(peers.remove(peer.getName()) != null) {
                log.trace("Transfering from active -to-> inactive list");
                unavailablePeers.put(peer.getName(),peer);
            }
            break;
        case ESGPeerEvent.CONNECTION_AVAILABLE:
            log.trace("Got ESGPeerEVent.CONNECTION_AVAILABLE from: "+peer.getName());
            if(unavailablePeers.remove(peer.getName()) != null) {
                log.trace("Transfering from inactive -to-> active list");
                peers.put(peer.getName(),peer);
            }else {
                log.trace("no status change for "+peer.getName());
            }
            break;
        default:
            break;
        }
        log.trace("Available Peers: ["+peers.size()+"] Unavailable: ["+unavailablePeers.size()+"]");    
    }
    
}