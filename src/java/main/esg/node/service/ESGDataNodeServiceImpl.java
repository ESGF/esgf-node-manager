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

        ----------------------------------------------------
      THIS CLASS RECEIVES ALL *INGRESS* CALLS *FROM* GATEWAY(s)!!
        ----------------------------------------------------
           (I don't think I can make this any clearer)


**/
package esg.node.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.core.ESGDataNodeManager;
import esg.node.core.AbstractDataNodeComponent;
import esg.node.core.ESGEvent;
import esg.node.core.ESGJoinEvent;
import esg.node.connection.ESGConnectionManager;
import esg.common.service.ESGRemoteEvent;

public class ESGDataNodeServiceImpl extends AbstractDataNodeComponent 
    implements ESGDataNodeService {

    private static final Log log = LogFactory.getLog(ESGDataNodeServiceImpl.class);
    private ESGDataNodeManager datanodeMgr = null;
    private ESGConnectionManager connMgr = null;
    

    public ESGDataNodeServiceImpl() {
	log.info("ESGDataNodeServiceImpl instantiated...");
	setMyName("DNODE_SVC");
	boot();
    }

    //******************************
    //Bootstrap the entire system...
    //******************************
    public void boot() {
	log.trace("Bootstrapping System...");
	datanodeMgr = new ESGDataNodeManager();
	datanodeMgr.registerComponent(this);
	datanodeMgr.init();
    }
    //******************************
    
    public void init() { log.trace("no-op initialization"); }

    //------------------------------------------------------------
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
	ret = (connMgr == null) ? false : connMgr.amAvailable(); 
	log.trace("amAvailable() -> "+ret);
	return ret;
    }
    
    public void unregister() {
	//Intentionally a NO-OP
	log.warn("Balking... What does it mean to unregister the service itself?... exactly... no can do ;-)");
    }
    //------------------------------------------------------------



    //Remote (ingress) calls to method...
    public boolean ping() { 
	log.trace("DataNode service got \"ping\"");
	return amAvailable(); 
    }

    //TODO: Think about the performance implications of having a synchronious return value
    //Remote (ingress) calls to method...
    public boolean notify(ESGRemoteEvent evt_) {
	log.trace("DataNode service got \"notify\" call with event: ["+evt_+"]");
	if(!amAvailable()) {
	    log.warn("Dropping ingress notification event on the floor, I am NOT available. ["+evt_+"]");
	    return false;
	}

	//Inspect the message type...
	if(evt_.getMessageType() != ESGRemoteEvent.REGISTER) return false;
	
	//TODO: Do Event Inspection, etc...
	
	return true;
    }

    //Ingress event from remote 'client'
    public void handleESGRemoteEvent(ESGRemoteEvent evt_) {
	log.trace("DataNode service got \"handleESGRemoteEvent\" call with event: ["+evt_+"]");
	if(!amAvailable()) {
	    log.warn("Dropping ingress notification event on the floor, I am NOT available. ["+evt_+"]");
	}
	
	//Being a nice guy and rerouting you to right method
	//I may be being too nice... consider taking this out if abused.
	if(evt_.getMessageType() == ESGRemoteEvent.REGISTER) { this.notify(evt_); };


	//TODO: Grab this remote event, inspect the payload and stuff
	//it into an esg event and fire it off to listeners to handle.
	//Example... if the payload was the Catalog xml then JAXB it
	//and send the object form as the payload for others to use
	//(for example)
	
	//Zoiks... this is a hack for now... should be more elegant with event hierarchy.
	ESGEvent evt = new ESGEvent(evt_,"Wrapped Remote Event");
	enqueueESGEvent(evt);
    }

    //--------------------------------------------
    //Event handling... (for join events) needs connection manager!
    //--------------------------------------------
    public void handleESGEvent(ESGEvent esgEvent) {
	//we only care about join events
	if(!(esgEvent instanceof ESGJoinEvent)) return;

	ESGJoinEvent event = (ESGJoinEvent)esgEvent;
	
	//we only care bout Gateways joining
	if(!(event.getJoiner() instanceof ESGConnectionManager)) return;

	if(event.hasJoined()) {
	    log.trace("Detected That The ESGConnectionManager Has Joined: "+event.getJoiner().getName());
	    connMgr = (ESGConnectionManager)event.getJoiner();
	    addESGQueueListener(connMgr);
	}else {
	    log.trace("Detected That The ESGConnectionManager Has Left: "+event.getJoiner().getName());
	    removeESGQueueListener(connMgr);
	    connMgr = null;	    
	}
	log.trace("Listener Queue has ["+esgQueueListeners.size()+"] connection Managers present");
	log.trace("connMgr = "+connMgr);
    }



}
