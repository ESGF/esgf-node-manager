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

   This is the result of my push to factor out RPC specific details
   such that this framework as a whole can support multiple RPC
   mechanisms and folks know where/how to integrate said new RPC
   mechanisms. :-) I am positive that as new RPC mechanisms come on
   line that further factoring etc will take place.  So this can be
   considered as a first best guess factoring given that at the moment
   (Feb, 2010) there is only one in place. -gavin :-)

   :-| . o 0 ( I may want to use Generics here to make things
   easier... Hmmm... Think about this summore later)
 
**/
package esg.node.core;

import esg.common.ESGException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import com.caucho.hessian.client.HessianProxyFactory;

public abstract class HessianPeer extends ESGPeer {
    
    private static final Log log = LogFactory.getLog(HessianPeer.class);    

    private HessianProxyFactory factory;
   
    public HessianPeer(String serviceURL, int type) throws java.net.MalformedURLException { 
        super(serviceURL,type); 
        this.factory = new HessianProxyFactory();
    }
    
    //From the super-class the default type is set to "DATA_NODE_PEER"
    public HessianPeer(String serviceURL) throws java.net.MalformedURLException { 
        super(serviceURL); 
        this.factory = new HessianProxyFactory();
    }
    protected HessianProxyFactory getFactory() { return factory; }

    //Note: This is what makes this Hessian specific... the
    //use of the hessian "factory.". Also Note, all RPC
    //mechanisms follow the same basic mechanics 
    //protected Object factoryCreate(Class serviceClass) throws ESGException {
    //  return this.factoryCreate(serviceClass,null);
    //}

    //TODO: make this a "generics" function...
    protected Object factoryCreate(Class serviceClass,String serviceURL) throws ESGException { 
        log.trace("factoryCreate -> serviceClass: "+serviceClass+" , serviceURL: "+serviceURL);
        if (serviceURL == null) serviceURL = getServiceURL();
        Object endpoint = null;
        try{
            endpoint = factory.create(serviceClass, serviceURL); 
        }catch(Exception e) {
            log.error(e);
            e.printStackTrace();
            throw new ESGException(e);
        }
        return endpoint;
    }
    
    
}