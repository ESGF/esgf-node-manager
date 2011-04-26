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
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;

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
    private Map<String,String> processedMap = null;
    private NodeHostnameComparator nodecomp = null;

    public ESGFRegistry(String name) {
        super(name);
        log.debug("Instantiating ESGFRegistry...");

    }
    
    public void init() {
        log.info("Initializing ESGFRegistry...");
        try{
            //props = getDataNodeManager().getMatchingProperties("*"); //TODO: figure the right regex for only what is needed
            props = new ESGFProperties();
            gleaner = new RegistrationGleaner(props);
            nodecomp = new NodeHostnameComparator();
            processedMap = Collections.synchronizedMap(new HashMap<String,String>()); //TODO: may want to persist this and then bring it back.
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

    private void mergeNodes(List<Node> myList, List<Node> otherList) {
        //Sort lists by hostname (natural) ascending order a -> z
        //where a compareTo z is < 0 iff a is before z
        Collections.sort(myList,nodecomp);
        Collections.sort(otherList,nodecomp);
        
        Set<Node> newNodes = new TreeSet<Node>(nodecomp);

        for(int i=0, j=0; i<otherList.size();) {
            if( (nodecomp.compare(myList.get(i),otherList.get(j))) == 0 ) {
                if((myList.get(i)).getTimeStamp() > (otherList.get(j)).getTimeStamp()) {
                    newNodes.add(myList.get(i));
                }else {
                    newNodes.add(otherList.get(j));
                }
                i++;
                j++;
            }else if ( (nodecomp.compare(myList.get(i),otherList.get(j))) < 0 ) {
                //zoiks
            }else{
                //zoiks
            }
        }

        myList.clear();
        myList.addAll(newNodes); //because using set they are 
    }

    //When peer registration messages are encountered grab those
    //events and collect the peer's registration information and
    //incorporate it into our own world view.
    public boolean handleESGQueuedEvent(ESGEvent event) {
        log.trace("handling enqueued event ["+getName()+"]:["+this.getClass().getName()+"]: Got A QueuedEvent!!!!: "+event);
        
        
        String payloadChecksum  = event.getRemoteEvent().getPayloadChecksum();
        String sourceServiceURL = event.getRemoteEvent().getSource();
        
        //TODO: Heck no, I should NOT be using string comparison for
        //this...  I need to revisit the typing of the remote event
        //for type of the checksum.  The thing is I don't want to use
        //BigInteger because I don't know how portable that is and I
        //want the event object as type simple as can be.  Right now
        //using the string representation of the checksum... maybe
        //that's good enough for the type complexity trade off?

        String lastChecksum = processedMap.get(sourceServiceURL);
        if( (lastChecksum != null) && (lastChecksum.equals(payloadChecksum)) ) {
            log.trace("I have seen this payload before, there is nothing new to learn... dropping event on floor ["+event+"]");
        }
        
        //zoiks: use gleaner to get local registration object to modify.
        //       periodically write modified structure to file (getting a new checksum)
        //       back the list of nodes with a datastructure that can ...
        //       have another datastructure with checksum and source service url
        //       to see if we should even do anything at all.
        
        //Pull out our registration information (parse the xml string
        //payload into object form via the gleaner) and send the event
        //on its way (the connection manager should be downstream this
        //event path

        Registration myRegistration = gleaner.getMyRegistration();
        Registration peerRegistration = gleaner.createRegistrationFromString((String)event.getRemoteEvent().getPayload());

        List<Node> myNodes = myRegistration.getNode();
        List<Node> peerNodes = peerRegistration.getNode();
        
        mergeNodes(myNodes,peerNodes);
        gleaner.saveRegistration();

        processedMap.put(sourceServiceURL, payloadChecksum);
        enqueueESGEvent(new ESGEvent(this,gleaner.toString(),gleaner.getMyChecksum()));

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

    //------------------------------------------------------------
    //Utility inner class... comparator for hostnames
    //------------------------------------------------------------
    private class NodeHostnameComparator implements Comparator<Node> {
        public int compare(Node a, Node b) {
            return a.getHostname().compareTo(b.getHostname());
        }
    }
    
}
