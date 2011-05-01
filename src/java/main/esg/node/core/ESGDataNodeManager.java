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

**/
package esg.node.core;

import esg.node.connection.ESGConnectionManager;
import esg.node.components.notification.ESGNotifier;
import esg.node.components.monitoring.ESGMonitor;
import esg.node.components.metrics.ESGMetrics;
import esg.node.components.registry.ESGFRegistry;
import esg.common.Utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ESGDataNodeManager extends AbstractDataNodeManager {

    private static Log log = LogFactory.getLog(ESGDataNodeManager.class);
    private ESGConnectionManager connMgr = null;

    private static final Pattern peerSvcRootPattern = Pattern.compile("(?:.*://)*([^/]*)[/]*",Pattern.CASE_INSENSITIVE);

    //TODO: the logic of the system, pulling in the components from a
    //config file and managing them.
    
    public ESGDataNodeManager() {
        log.info("Instantiating ESGDataNodeManager...");
    }

    private void initCoreServices() {
        log.info("Loading core components");

        connMgr = new ESGConnectionManager("CONN_MGR");
        registerComponent(connMgr);
    

        try{
            //NOTES: ESGF_PEER_SVC_ROOT should be the hostname of the
            //machine but it turns out that we need to be flexible to
            //take other values users mayb put in here, such as the
            //url to the "main" front-end service.  So we need to cull
            //out the hostname.
            String myHostname = null;
            try {
                myHostname = getNodeProperty("esgf.host",java.net.InetAddress.getLocalHost().getHostAddress());
                System.out.println(" Manager says, \"I am ["+myHostname+"]\"");
            }catch(java.net.UnknownHostException ex) {log.error(ex); }
            
            String defaultPeer = System.getenv().get("ESGF_PEER_SVC_ROOT");
            String defaultPeerName=null;
            
            if(null != defaultPeer) {
                Matcher peerSvcNameMatcher = peerSvcRootPattern.matcher(defaultPeer);
                if (peerSvcNameMatcher.find()) { defaultPeerName = peerSvcNameMatcher.group(1); }
                System.out.println(" Mangager says, \"My Default Peer is ["+defaultPeerName+"]\"");
                
                if((null != defaultPeerName) && !(defaultPeerName.equalsIgnoreCase(myHostname))) {
                    ESGPeer peer = new BasicPeer(Utils.asServiceUrl(defaultPeerName), ESGPeer.DEFAULT_PEER);
                    log.trace("1)) Created default peer attempting to register it");
                    registerPeer(peer);
                }else{
                    if(defaultPeerName.equalsIgnoreCase(myHostname)) {
                        log.warn("You may not set yourself as your peer ;-)... when bootstrapping this puts you in passive peer mode - waiting to be contacted.");
                    }else{
                        log.error("The Default Peer is: ["+defaultPeer+"]: Hint - set ESGF_PEER_SVC_ROOT in /etc/esg.env");
                    }
                }
            }
        }catch(java.net.MalformedURLException e) {log.error(e); }
        
        ESGFRegistry registry = new ESGFRegistry("REGISTRY");
        registerComponent(registry);
    
        ESGNotifier notifier = new ESGNotifier("NOTIFIER");
        registerComponent(notifier);

        ESGMonitor monitor = new ESGMonitor("MONITOR");
        registerComponent(monitor);
    
        ESGMetrics metrics = new ESGMetrics("METRICS");
        registerComponent(metrics);

        log.info("Connecting core components");

        //Now build the "FSM" connections.
        //Ingress messages from the network come in through the DNODE_SVC
        //The DNODE_SVC then routes the message to the appropriate 
    
        connect("DNODE_SVC","REGISTRY");
        connect("DNODE_SVC","NOTIFER");
        connect("DNODE_SVC","MONITOR");
        connect("DNODE_SVC","METRICS");

        connect("REGISTRY","CONN_MGR"); 
        connect("NOTIFIER","CONN_MGR");
        connect("MONITOR","CONN_MGR");
        connect("METRICS","CONN_MGR");
    }

    public void init() {
        log.info("Initializing ESG Data Node Manager...");
    
        //TODO
        //Read in configuration file
    
        initCoreServices();
    
        //TODO
        //load up other services....
    
        sendAllLoadedNotification();
    }

} 