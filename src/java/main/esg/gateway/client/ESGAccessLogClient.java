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
package esg.gateway.client;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import com.caucho.hessian.client.HessianProxyFactory;

import esg.gateway.service.ESGAccessLogService;

/**
   Description:
   This is the client side the the AccessLogService.
*/
public class ESGAccessLogClient {
    private static final Log log = LogFactory.getLog(ESGAccessLogClient.class);    
    private HessianProxyFactory factory = null;

    public ESGAccessLogClient() {
	log.trace("Instantiating ESGAccessLogClient");
	this.factory = new HessianProxyFactory();	
    }

    private Object factoryCreate(Class serviceClass,String serviceURL) { 
	log.trace("factoryCreate -> serviceClass: "+serviceClass.getName()+" , serviceURL: "+serviceURL);
	if (serviceURL == null) {
	    log.error("The service url cannot be null ["+serviceURL+"]");
	    return null;
	}
	Object endpoint = null;
	try{
	    endpoint = factory.create(serviceClass, serviceURL); 
	}catch(Exception e) {
	    log.error(e);
	    e.printStackTrace();
	}
	return endpoint;
    }
    
    /**
       Overloading method... Takes no args - fetches all access log data from test data node
     */
    public List<String[]> fetchAccessLogData() {
	return this.fetchAccessLogData("http://test-dnode.llnl.gov/esg-node/accesslog",Long.MIN_VALUE,Long.MAX_VALUE);
    }
    
    /**
       Overloading method... takes the service url - uses min and max long value for time span.
       @param serviceURL The url for the RPC target
     */
    public List<String[]> fetchAccessLogData(String serviceURL) {
	return this.fetchAccessLogData(serviceURL,Long.MIN_VALUE,Long.MAX_VALUE);
    }
    
    /**
       Providing a simple delgating wrapper, for making the remote call
       @param serviceURL The url for the RPC target
       @param startTime  The <code>Long</code> time stamp value for the earliest record (inclusive)
       @param endTime    The <code>Long</code> time stamp value for the latest record (not inclusive)
     */
    public List<String[]> fetchAccessLogData(String serviceURL, long startTime, long endTime) {
	ESGAccessLogService endpoint = null;
	try{
	    log.trace("Creating stub endpoint to : "+serviceURL);
	    endpoint = (ESGAccessLogService)factory.create(ESGAccessLogService.class,serviceURL);
	}catch(Exception e) {
	    log.error(e);
	    e.printStackTrace();
	}
	
	log.info("Query for: "+serviceURL+" -> from: "+startTime+" to: "+endTime);
	return endpoint.fetchAccessLogData(startTime,endTime);
    }
    
    public static void main(String[] args) {
	try {
	    ESGAccessLogClient client = new ESGAccessLogClient();
	    List<String[]> results = null;
	    if(args.length > 0) {
		results = client.fetchAccessLogData(args[0]);
	    }else {
		results = client.fetchAccessLogData();
	    }
	    System.out.println("---results:["+results.size()+"]---");
	    for(String[] record : results) {
		StringBuilder sb = new StringBuilder();
		for(String column : record) {
		    sb.append("["+column+"] ");
		}
		System.out.println(sb.toString());
	    }
	    System.out.println("-----------------");
	}catch(Throwable t) {
	    log.error(t);
	    t.printStackTrace();
	}
    }
}
