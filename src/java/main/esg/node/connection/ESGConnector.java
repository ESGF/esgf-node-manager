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

   The entire point of this class is to enable to ability to make
   calls against the esgf-node-manager.  It can be considered a node
   manager "stub".

**/
package esg.node.connection;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import esg.common.ESGException;
import esg.common.Utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import com.caucho.hessian.client.HessianProxyFactory;

import esg.node.service.ESGDataNodeService;

public class ESGConnector {

    private static final Log log = LogFactory.getLog(ESGConnector.class);
    private static ESGConnector connector = null;

    private HessianProxyFactory factory = null;
    private ESGDataNodeService currentEndpoint = null;
    private String currentServiceURL = null;
    private String currentServiceHost = null;
    private boolean secured=false;
    private Map<String,ESGDataNodeService> endpointCache = null;


    //------------------------------------------------------------------
    //Construction
    //------------------------------------------------------------------
    private ESGConnector() {
        this(null,false);
    }

    private ESGConnector(String serviceHost) {
        this(serviceHost, false);
    }

    private ESGConnector(boolean secured) {
        this(null,secured);
    }

    private ESGConnector(String serviceHost, boolean secured) {
        log.trace("Instantiating ESGConnector");
        this.factory = new HessianProxyFactory();
        this.secured = secured;
        if(null != serviceHost) this.setEndpoint(serviceHost);
    }

    //-----

    public static synchronized ESGConnector getInstance() {
        return connector == null ? connector = new ESGConnector(null,false) : connector;
    }

    public static synchronized ESGConnector getInstance(String serviceHost) {
        if(connector == null) {
            connector = new ESGConnector(serviceHost,false);
        }else{
            connector.setEndpoint(serviceHost,true);
        }
        return connector;

    }
    public static synchronized ESGConnector getInstance(boolean secured) {
        if(connector == null) {
            connector = new ESGConnector(null,secured);
        }else{
            connector.setSecured(secured);
        }
        return connector;
    }

    public static synchronized ESGConnector getInstance(String serviceHost, boolean secured) {
        if(connector == null) {
            connector = new ESGConnector(serviceHost,secured);
        }else{
            connector.setEndpoint(serviceHost).setSecured(secured);
        }
        return connector;
    }

    //------------------------------------------------------------------

    /**
       Determins if we are using the http or https protocol to talk to the endpoint
     */
    public ESGConnector setSecured(boolean secured) {
        this.secured = secured;
        return this;
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
    public ESGConnector setEndpoint() { return this.setEndpoint("localhost",false); }
    protected ESGConnector setEndpoint(String serviceHost) { return this.setEndpoint(serviceHost,false); }
    protected synchronized ESGConnector setEndpoint(String serviceHost_, boolean force) {
        String serviceHost = null;
        //--------------------
        //param sanity checks
        //--------------------
        if (serviceHost_ == null) {
            System.out.println("ERROR: You have passed in a null ["+serviceHost_+"] service host!!!!");
            log.error("You have passed in a null ["+serviceHost_+"] service host!!!!");
            return null;
        }
        
        //Basically I am allowing you to pass in a legal service url (just to be nice);
        if(Utils.isLegalUrl(serviceHost_)) {
            if(Utils.isLegalServiceUrl(serviceHost_)) {
                serviceHost = Utils.asHostname(serviceHost_);
            }else{
                System.out.println("ERROR: You have passed in an illegal endpoint ["+serviceHost_+"], can't accept that.");
                log.error("You have passed in an illegal endpoint ["+serviceHost_+"], can't accept that.");
                return null;
            }
        }else{
            serviceHost = serviceHost_;
        }
        //--------------------
        
        if( null == this.endpointCache) this.endpointCache = new HashMap<String,ESGDataNodeService>(1);
        
        if((null == (this.currentEndpoint = endpointCache.get(serviceHost))) || force ) {
            String serviceURL = Utils.asServiceUrl(serviceHost,secured);
            log.trace("Creating stub endpoint to : "+serviceURL);
            this.currentEndpoint = (ESGDataNodeService)factoryCreate(ESGDataNodeService.class,serviceURL);
            this.currentServiceURL=serviceURL;
            this.currentServiceHost=serviceHost;
            endpointCache.put(currentServiceHost,currentEndpoint);
        } else {
            log.trace("Using cached sub endpoint to : "+Utils.asServiceUrl(serviceHost,secured));
        }
        return this;
    }

    //Force peer stubs to be re-generated...
    public synchronized ESGConnector reset() {
        log.trace("resetting connector peer stubs...");
        Set<String> serviceHostSet = new HashSet<String>(endpointCache.keySet());
        clearCache(); //yes, a bit of overkill but want to make sure we 'super' force the creation of new stubs!
        for(String serviceHost : serviceHostSet) {
            setEndpoint(serviceHost,true);
        }
        return this;
    }

    //Force peer stubs to be re-generated...
    public synchronized ESGConnector reset(String serviceHost) { setEndpoint(serviceHost,true); return this; }
    
    public String getServiceUrl() { return this.currentServiceURL; }
    public String getEndpoint() { return this.currentServiceHost; }

    //cache utility methods...
    public synchronized ESGConnector clearCache() { endpointCache.clear(); return this; }
    public synchronized int cacheSize() { return endpointCache == null ? 0 : endpointCache.size(); }

    //Returns the hosts of the peers we have cached connections (stubs) to.
    public synchronized List<String> getCachedEndpointList() { 
        List<String> endpointList = new ArrayList<String>();
        if(endpointCache != null) endpointList.addAll(endpointCache.keySet());
        return endpointList;
    }
    public synchronized ESGConnector expungeFromCache(String serviceHost) {
        if (endpointCache == null) return this;
        endpointCache.remove(serviceHost);
        return this;
    }
    
    //------------------------------------------------------------------
    // Service Side methods
    //------------------------------------------------------------------

    /**
       Simple ping function to access the viability of the RPC mechanism.
       (if you can ping you know the RPC works)
    */
    public boolean ping() {
        log.info("Connector -> ping()...");
        boolean ret = false;
        try{
            if(null == currentEndpoint) {
                log.warn("NO endpoint set! Call setEndpoint before calling ping!");
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
       Prune function... tells the node manager to brute force check
       all its known peers and make sure they are indeed available.
       "Dead" nodes that are found are immediately removed leaving
       only "alive" nodes.  Hence we prune the list of peers.
     */
    public boolean prune() {
        log.info("Connector -> prune()...");
        boolean ret = false;
        try{
            if(null == currentEndpoint) {
                log.warn("NO endpoint set! call setEndpoint before calling prune!");
            }
            ret = currentEndpoint.prune();
            log.trace("Prune returns: "+ret);
        }catch(Exception e) {
            //REtry logic... If the protocol is having issues the
            //recreating the stub may help?
            log.error(e);
            e.printStackTrace();
            try{
                reset();
                ret = currentEndpoint.prune();
                log.trace("Prune retry (reset) returns: "+ret);
                return ret;
            }catch(Exception ex) {
                log.error(ex);
                ex.printStackTrace();
            }
        }
        return ret;
    }

    //------------------------------------------------------------------
 
}