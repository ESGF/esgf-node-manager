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

        This is essentially the wrapped stubs to the gateway.
        They are the "client" side of the rpc call where calls
        ORIGINATE. (from DataNode -to-> Gateway).

        ----------------------------------------------------
        THIS CLASS REPRESNTS *EGRESS* CALLS TO THE GATEWAY!!
        ----------------------------------------------------
           (I don't think I can make this any clearer)

**/
package esg.node.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import java.net.MalformedURLException;
import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;

import esg.node.core.ESGGatewayListener;
import esg.gateway.service.ESGGatewayService;
import esg.common.service.ESGRemoteEvent;
import esg.common.Utils;

public class BasicGateway extends HessianGateway {

    private static final Log log = LogFactory.getLog(BasicGateway.class);
    private ESGGatewayService gatewayService = null;
    private List<ESGGatewayListener> gatewayEventListeners = null;
    
    private boolean pingState = false;

    public BasicGateway(String name, String serviceURL) { 
	super(name,serviceURL); 
	gatewayEventListeners = new ArrayList<ESGGatewayListener>();
    }
    
    public BasicGateway(String name) { this(name,null); }

    //The idea here is to establish communication with the rpc
    //endpoint this class is a stub for. init() may be called multiple times until valid;
    public void init() {
	if(isValid) {
	    log.info("I am already valid... :-)");
	    return;
	}

	//I am not a valid object if I don't have a name
	if(getName() == null) {
	    log.warn("Cannot initialize Gateway : NO NAME!!");
	    isValid = false;
	    return;
	}

	//I am not a valid object if I don't have an endpoint URL
	if(getServiceURL() == null) {
	    log.warn("Cannot initialize Gateway ["+getName()+"]: NO SERVICE URL!!");
	    isValid = false;
	    return;
	}

	try {
	    //The call to our superclass to create the proper stub object to remote service.
	    gatewayService = (ESGGatewayService)factoryCreate(ESGGatewayService.class, getServiceURL());
	}catch(Exception ex) {
	    log.warn("Could not connect to serviceURL ["+getServiceURL()+"]",ex);
	    isAvailable = false;
	}
	
	if(gatewayService != null) {
	    log.trace(getName()+" Proxy properly initialized");
	    isValid = true;
	}else {
	    log.warn(getName()+" Proxy NOT properly initialized");
	    isValid = false;
	}
    }
    
    
    //-----------------------------------------------
    //Delgated remote methods...
    //-----------------------------------------------

    //The simplest of communications to make sure folks are alive and
    //that the RPC works at a basic level... If and only if I can ping
    //you and you respond (return true) that I can consider you a
    //VALID end point, and therefore, my proxy ;-), *I* am valid to
    //use for communicating to you, mr. gateway. gyot it? :-) 

    //A return true of false indicate that you are able to be
    //communicated with, however a return of false is you responding
    //that you are open to being talked to and thus I, by proxy am not
    //going to perform calls on your behalf, and therefore not valid.

    //This semantically expected to be the START of the communication
    //between data node and gateway.  (called by the bootstrapping
    //service: ESGDataNodeService)
    public boolean ping() { 
	log.trace("ping -->> ["+getName()+"]");
	boolean response = false;
	try {
	    //TODO see about changing the timeout so don't have to wait forever to fail!	    
	    response = gatewayService.ping();
	    pingState = (response  && isValid);
	    log.trace( (response ? "[OK]" : "[BUSY]") );
	}catch (RuntimeException ex) {
	    log.trace("[FAIL]");
	    log.error("Problem calling \"ping\" on ["+getServiceURL()+"] "+ex.getMessage());
	    response = false;
	    fireConnectionFailed(ex);
	}
	
	//This is basically saying that because of a ping there is a
	//change in the availability state of the gateway in question.
	//So only upon a change in state will there be events fired
	//through the rest of the system and the new state recorded
	//and dispatched to the rest of the system.  This saves us
	//from sending out events that doesn't contain any *new*
	//information.
	if(isAvailable != pingState) {
	    if(pingState) fireConnectionAvailable(); else fireConnectionBusy();
	}
	isAvailable = pingState;

	log.trace("isValid = "+isValid);
	log.trace("response = "+response);
	log.trace("isAvailable = "+isAvailable);
	return isAvailable;
	//FYI: the isAvailable() method is defined in super-superclass)
    }
    
    //Present the gateway with this client's identity and thus
    //callback address for making calls back to the data node
    //services for sending notifications
    public boolean registerToGateway() { 
	if(!isValid  || !isAvailable) {
	    log.warn("May not issue \"register\" rpc call unless object has been initialized to be made valid and ping has been issued to make sure I am available!!!");
	    return false;
	}

	String myLocation = null;
	try{
	    myLocation = "http://"+InetAddress.getLocalHost().getCanonicalHostName()+"/esg-node/datanode";
	}catch (java.net.UnknownHostException ex) {
	    log.error("Could not build proper location string for myself",ex);
	}
	
	ESGRemoteEvent registrationEvent = new ESGRemoteEvent(myLocation,ESGRemoteEvent.REGISTER,Utils.nextSeq());
	
	try {
	    log.trace("Making Remote Call to \"register\" method, sending: "+registrationEvent);
	    gatewayService.register(registrationEvent);
	    return true;
	}catch (RuntimeException ex) {
	    log.error("Problem calling \"register\" on ["+getServiceURL()+"] "+ex.getMessage());
	    fireConnectionFailed(ex);
	    return false;
	}
    }


    protected void fireConnectionAvailable() {
	log.trace("Firing Connection Available to "+getServiceURL());
	fireESGGatewayEvent(new ESGGatewayEvent(this,ESGGatewayEvent.CONNECTION_AVAILABLE));
    }
    protected void fireConnectionFailed(Throwable t) {
	log.trace("Firing Connection Failed to "+getServiceURL());
	fireESGGatewayEvent(new ESGGatewayEvent(this,t.getMessage(),ESGGatewayEvent.CONNECTION_FAILED));
    }
    protected void fireConnectionBusy() {
	log.trace("Firing Connection Busy to "+getServiceURL());
	fireESGGatewayEvent(new ESGGatewayEvent(this,ESGGatewayEvent.CONNECTION_BUSY));
    }
    //--------------------------------------------
    //Event dispatching to all registered ESGGatewayListeners
    //calling their handleGatewayEvent method
    //--------------------------------------------
    protected void fireESGGatewayEvent(ESGGatewayEvent esgEvent) {
	log.trace("Firing Event: "+esgEvent);
	for(ESGGatewayListener listener: gatewayEventListeners) {
	    listener.handleGatewayEvent(esgEvent);
	}
    }


    
}
