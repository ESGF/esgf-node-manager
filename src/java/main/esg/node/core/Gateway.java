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

   This is basically an abstract class that is the STUB for the remote
   Gateway (peer) for making/initiating calls on the peer.

**/
package esg.node.core;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.service.ESGRemoteEvent;

public abstract class Gateway extends AbstractDataNodeComponent{

    //TODO: Okay this should totally be an ENUM!!!!!
    public static final int DEFAULT_GATEWAY = 1;
    public static final int GATEWAY = 2;
    public static final int DATA_NODE_PEER  = 4;
    private int peerType = 0;

    private static final Log log = LogFactory.getLog(Gateway.class);
    private String serviceURL = null;

    protected boolean isValid = false;
    protected boolean isAvailable = false;

    private static Pattern hostPattern = Pattern.compile("http://([^/ ]*)/.*[/]*/(datanode|gateway)");
    
    public Gateway(String serviceURL, int type) throws java.net.MalformedURLException { 
	//TODO: regex out the IP and set as name
	new java.net.URL(serviceURL);
	String host = null;
	String typeString = null;
	Matcher hostMatcher = hostPattern.matcher(serviceURL);
	if(hostMatcher.find()) {
	    host = hostMatcher.group(1);
	    typeString = hostMatcher.group(2);
	}
	setMyName(host);
	this.peerType = type;
	this.serviceURL = serviceURL;
    }

    public Gateway(String name) throws java.net.MalformedURLException { this(name,GATEWAY); }
    public int getPeerType() { return peerType; }

    public abstract void init();
    protected void setServiceURL(String serviceURL) { this.serviceURL = serviceURL; }
    public String getServiceURL() { return serviceURL; }
    public boolean isValid() { return isValid; }
    public boolean isAvailable() { return isAvailable; }
    
    
    //-----------------------------------------------------------------
    //Delgating: remote method wrappers.
    //-----------------------------------------------------------------
    //override me (all services should have a "ping" remote method! according to me! :-)
    public abstract boolean ping();

    //Present the gateway with a token and callback address for 
    //making calls back to the data node services.
    public abstract boolean registerToGateway();

    //Send the represented Gateway endpoint an event object
    public abstract void handleESGRemoteEvent(ESGRemoteEvent evt);
    //-----------------------------------------------------------------
    


    //-----------------------------------------------------------------
    //Overriding superclass to perform gateway object specific unregistration
    //from the data node manager. (unrelated to RPC, internal management)
    //-----------------------------------------------------------------
    public void unregister() {
	DataNodeManager dataNodeManager = getDataNodeManager();
	if(dataNodeManager == null) return;
	if(this instanceof Gateway) { dataNodeManager.removeGateway(this); }
	//Note: don't have to null out mgr like in the super class
	//because I am never holding on to a mgr handle directly ;-)
    }
    //-----------------------------------------------------------------


    public String toString() {
	return "Gateway: ["+getName()+"] -> ["+(serviceURL == null ? "NULL" : serviceURL)+"] ("+isValid+") [PT:"+peerType+"]";
    }
    
}
