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
*   For details, see http://esgf.org/esg-node/                    *
*   Please also read this link                                             *
*    http://esgf.org/LICENSE                                      *
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
package esg.gateway.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import java.net.MalformedURLException;
import java.net.InetAddress;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;

import esg.common.Utils;
import esg.common.service.ESGRemoteEvent;
import esg.node.service.ESGDataNodeService;



public class ESGGatewayServiceImpl implements ESGGatewayService {

    private static final Log log = LogFactory.getLog(ESGGatewayServiceImpl.class);
    private HessianProxyFactory factory = null;
    private ESGDataNodeService datanodeService = null;

    public ESGGatewayServiceImpl() {
	log.info("ESGGatewayServiceImpl instantiated...");
	factory = new HessianProxyFactory();
    }
    
    //ingress ping request...
    public boolean ping() {
	log.trace("Gateway service got \"ping\"");
	return true;
    }

    //ingress registration request... returns registration event to remote caller's notify method...
    public void register(ESGRemoteEvent evt) {
	log.trace("Gateway service got \"register\" call from datanode with event: ["+evt+"]");
	
	//Triage incoming revent...
	if(evt.getMessageType() != ESGRemoteEvent.REGISTER) {
	    log.trace("Registration called with wrong event type... dropping on floor...");
	    return;
	}

	//Create the string for *our* callback address...
	String myLocation = null;
	try{
	    myLocation = "http://"+InetAddress.getLocalHost().getCanonicalHostName()+"/esg-node/gateway";
	}catch (java.net.UnknownHostException ex) {
	    log.error("Could not build proper location string for myself",ex);
	}


	//Create proxy endpoint for data node service that sent us this event.
	try {
	    datanodeService = (ESGDataNodeService)factory.create(ESGDataNodeService.class, evt.getSource());
	}catch(MalformedURLException ex) {
	    log.warn("Could not connect to serviceURL ["+evt.getSource()+"]",ex);
	}
	
	//Assemble info to create an event and send it to data node in response to this 'register' call.
	try {
	    ESGRemoteEvent registrationResponseEvent = new ESGRemoteEvent(myLocation,ESGRemoteEvent.REGISTER,Utils.nextSeq());
	    log.trace("Completing Registration By Making Remote Call to \"notify\" method, sending: "+registrationResponseEvent);
	    if(datanodeService.notify(registrationResponseEvent))
		log.trace("Registration Request Successfully Submitted...");
	    else
		log.trace("Registration Request Rejected");
	}catch (HessianRuntimeException ex) {
	    log.error("Problem calling \"register\" on ["+evt.getSource()+"] "+ex.getMessage());
	}
    }

    //ingress remote event to be handled....
    public void handleESGRemoteEvent(ESGRemoteEvent evt) {
	log.trace("Gateway service got \"handleESGRemoteEvent\" call with event: ["+evt+"]");	
    }

}
