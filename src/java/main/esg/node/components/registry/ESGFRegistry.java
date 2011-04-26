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
package esg.node.components.registry;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.util.ESGFProperties;
import esg.common.service.ESGRemoteEvent;
import esg.node.connection.ESGConnectionManager;
import esg.node.core.*;
import esg.common.generated.registration.*;

/**
   Description:
   
   Core object for providing the Registry service.  The registration
   "form" (xml payload defined by the registration.xsd) that is passed
   among nodes describes the set of services available on a particular
   node.  This object exists in service of registration distribution
   and collection.
   
*/
public class ESGFRegistry extends AbstractDataNodeComponent {
    
    private static Log log = LogFactory.getLog(ESGFRegistry.class);
    private Properties props = null;
    private boolean isBusy = false;
    private RegistrationGleaner gleaner = null;

    public ESGFRegistry(String name) {
        super(name);
        log.debug("Instantiating ESGFRegistry...");
    }
    
    public void init() {
        log.info("Initializing ESGFRegistry...");
        try{
            //props = getDataNodeManager().getMatchingProperties("*");
            props = new ESGFProperties();
            gleaner = new RegistrationGleaner(props);
            startRegistry();
        }catch(java.io.IOException e) {
            System.out.println("Damn ESGFRegistry can't fire up... :-(");
            log.error(e);
        }
    }

    private synchronized boolean fetchNodeInfo() {
        log.trace("Registry's fetchNodeInfo() called....");
        return true;
    }
    
    private void startRegistry() {
        log.trace("launching registry timer");
        long delay  = Long.parseLong(props.getProperty("registry.initialDelay","8"));
        long period = Long.parseLong(props.getProperty("registry.period","86405"));
        log.trace("registry delay:  "+delay+" sec");
        log.trace("registry period: "+period+" sec");
	
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
                public final void run() {
                    //log.trace("Checking for any new node information... [busy? "+ESGFRegistry.this.isBusy+"]");
                    if(!ESGFRegistry.this.isBusy) {
                        ESGFRegistry.this.isBusy = true;
                        if(fetchNodeInfo()) {
                            //TODO
                            gleaner.createMyRegistration().saveRegistration();
                        }
                        ESGFRegistry.this.isBusy = false;
                    }
                }
            },delay*1000,period*1000);
    }

    //When peer registration messages are encountered grab those
    //events and collect the peer's registration information and
    //incorporate it into our own world view.
    public boolean handleESGQueuedEvent(ESGEvent event) {
        log.trace("handling enqueued event ["+getName()+"]:["+this.getClass().getName()+"]: Got A QueuedEvent!!!!: "+event);

        //Pull out our registration information (parse the xml string
        //payload into object form via the gleaner) and send the event
        //on its way (the connection manager should be downstream this
        //event path
        event.setData(gleaner.createRegistrationFromString((String)event.getRemoteEvent().getPayload()));

        enqueueESGEvent(event);
        return true;
    }

    //Listen out for Joins from comm.  TODO: grab the peer
    //datastructure from comm and poke at it accordingly
    public void handleESGEvent(ESGEvent esgEvent) {
        //we only care about join events... err... sort of :-)

        //Note: should not be so myopic in dealing with system
        //events. There may be others that need to be acted
        //upon... but I am in Brody mode now -gavin
        if((esgEvent instanceof ESGSystemEvent) && 
           (((ESGSystemEvent)esgEvent).getEventType() == ESGSystemEvent.ALL_LOADED) ) {
            log.trace("I must have missed you in the load sequence CONN_MGR... I got you now");
            addESGQueueListener(getDataNodeManager().getComponent("CONN_MGR"));            
        }

        if(!(esgEvent instanceof ESGJoinEvent)) return;

        ESGJoinEvent event = (ESGJoinEvent)esgEvent;
    
        //we only care bout peer joining
        if(!(event.getJoiner() instanceof ESGConnectionManager)) return;

        if(event.hasJoined()) {
            log.trace("Detected That The ESGConnectionManager Has Joined: "+event.getJoiner().getName());
            addESGQueueListener(event.getJoiner());
        }else {
            log.trace("Detected That The ESGConnectionManager Has Left: "+event.getJoiner().getName());
            removeESGQueueListener(event.getJoiner());
        }
    }

}
