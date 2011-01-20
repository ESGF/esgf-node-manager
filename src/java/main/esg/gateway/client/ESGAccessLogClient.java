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
    private ESGAccessLogService currentEndpoint = null;
    private String currentServiceURL = null;

    public ESGAccessLogClient() {
        log.trace("Instantiating ESGAccessLogClient");
        this.factory = new HessianProxyFactory();
    }

    public ESGAccessLogClient(String serviceHost) {
        this();
        this.setEndpoint(serviceHost);
    }

    /**
       Generic Hessian endpoint creation method (Straight up Hessian no frills)
    */
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
       Sets up this current endpoint to the service URL This is a
       "fluent" style function that returns <code>this</code> properly
       typed "stub" object back.
       @param serviceURL The url for the RPC target
    */
    public ESGAccessLogClient setEndpoint(String serviceHost) {
        assert (serviceHost != null);
        String serviceURL = "http://"+serviceHost+"/esgf-node-manager/accesslog";
        log.trace("Creating stub endpoint to : "+serviceURL);
        this.currentEndpoint = (ESGAccessLogService)factoryCreate(ESGAccessLogService.class,serviceURL);
        this.currentServiceURL=serviceURL;
        return this;
    }

    /**
       Simple ping function to access the viability of the RPC mechanism.
       (if you can ping you know the RPC works)
    */
    public boolean ping() {
        boolean ret = false;
        try{
            if(null == currentEndpoint) {
                log.warn("endpoint not set! call setEndpoint before calling ping");
            }
            ret = currentEndpoint.ping();
            log.trace("Ping returns: "+ret);
        }catch(Exception e) {
            log.error(e);
            e.printStackTrace();
        }
        return ret;
    }
    
    /**
       Overloading method... Takes no args - fetches all access log data from test data node
    */
    public List<String[]> fetchAccessLogData() {
        return this.fetchAccessLogData(0,Long.MAX_VALUE);
    }
    
    
    /**
       Providing a simple delgating wrapper, for making the remote call
       @param startTime  The <code>Long</code> time stamp value for the earliest record (inclusive)
       @param endTime    The <code>Long</code> time stamp value for the latest record (not inclusive)
    */
    public List<String[]> fetchAccessLogData(long startTime, long endTime) {
        log.info("Query for: "+currentServiceURL+" -> from: "+startTime+" to: "+endTime);
        return currentEndpoint.fetchAccessLogData(startTime,endTime);
    }

    //Run the program like java -jar esg-node-accesslog-client.0.0.3.jar [service host]
    public static void main(String[] args) {
        String serviceHost=null;
        long startTime=0;
        long endTime=Long.MAX_VALUE;

        try{
            serviceHost=args[0];
            startTime = Long.parseLong(args[1]);
            endTime = Long.parseLong(args[2]);
        }catch(Throwable t) { log.error(t.getMessage()); }

        try {
            ESGAccessLogClient client = new ESGAccessLogClient();
            if(client.setEndpoint(serviceHost).ping()) {
                List<String[]> results = null;
                results = client.fetchAccessLogData(startTime,endTime);

                System.out.println("---results:["+(results.size() - 1)+"]---");
                for(String[] record : results) {
                    StringBuilder sb = new StringBuilder();
                    for(String column : record) {
                        sb.append("["+column+"] ");
                    }
                    System.out.println(sb.toString());
                }
            }
            System.out.println("-----------------");
        }catch(Throwable t) {
            log.error(t);
            t.printStackTrace();
        }
    }
}
