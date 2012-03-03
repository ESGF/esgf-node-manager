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
package esg.common;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.net.Socket;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);
    
    private static AtomicLong msgCounter = new AtomicLong(0);
    private static String myHostname = null;
    private static String myServiceUrl = null;
    private static final String urlRegex = "http[s]?://([^:/]*)(:(?:[0-9]*))?/(.*/)*(.*$)";
    private static final Pattern urlPattern = Pattern.compile(urlRegex,Pattern.CASE_INSENSITIVE);
    private static final String serviceUrlRegex = "http[s]?://([^:/]*)(:(?:[0-9]*))?/esgf-node-manager/node";
    private static final Pattern serviceUrlPattern = Pattern.compile(serviceUrlRegex,Pattern.CASE_INSENSITIVE);
    private static QuickHash quickHash = null;


    public static long nextSeq() { return msgCounter.getAndIncrement(); }

    public static String getNodeID() {
        String nodeID = null;
        if(null != nodeID) { return nodeID; }
        try{
            nodeID = java.net.InetAddress.getLocalHost().getHostAddress();
        }catch(java.net.UnknownHostException ex) {
            log.error(ex);
        }
        //NOTE: Doing the call to "toString" on purpose to force a null
        //pointer exception the return value should NEVER be null!
        return nodeID.toString();
    }

    public static boolean poke(String host, int port) { return poke(host,port,200); }
    public static boolean poke(String host, int port, int timeout) {
        boolean ret = false;
        Socket socket = null;
        try{
            (socket = new Socket()).connect(new InetSocketAddress(host,port),timeout);
            socket.close();
            ret=true;
        }catch(Throwable t) {
            log.error(t.getMessage());
        }finally {
            try{ socket.close(); } catch (Throwable t) {}
        }
        return ret;
    }

    public static String hashSum(String plaintext) {
        try {
            if(quickHash == null) quickHash = new QuickHash();
            return quickHash.sum(plaintext);
        }catch(Throwable t) { 
            System.out.println(t.getMessage());
            return null;
        }
    }

    public static String getFQDN() {
        try{
            if (myHostname == null) {
                myHostname = java.net.InetAddress.getLocalHost().getCanonicalHostName();
            }
        }catch (java.net.UnknownHostException ex) {
            log.error(ex);
        }
        return myHostname;
    }

    public static String getMyServiceUrl() {
        if(myServiceUrl == null)
            myServiceUrl = asServiceUrl(getFQDN());
        return myServiceUrl;
    }

    public static String asServiceUrl(String hostname, boolean secured) {
        return "http"+(secured ? "s" : "")+"://"+hostname+"/esgf-node-manager/node";
    }

    public static String asServiceUrl(String hostname) { return  asServiceUrl(hostname,false); }

    //NOTE: If there is a point where we want to support running the
    //node manager on another port that 80 Then we may want to always
    //consider the port part of the hostname.  In which case we need
    //to change the regex above for the urlPattern to include the port
    //in the hostname.  That should do it. Then foohost:80 would be
    //diff than foohost:8080 (at the moment we only return foohost
    //regardless of port) 
    //Affected code would include callers of the ESGConnector...
    public static String asHostname(String serviceUrl) {
        Matcher m = urlPattern.matcher(serviceUrl);
        if(m.find()) return m.group(1);
        return null;
    }

    public static boolean isLegalUrl(String urlCandidate) { return (urlPattern.matcher(urlCandidate)).find(); }
    public static boolean isLegalServiceUrl(String urlCandidate) { return (serviceUrlPattern.matcher(urlCandidate)).find(); }

    /**
       Takes two version strings of the form v#.#.#and compares them.
       @param candidate version string
       @param reference version 
     */
    public static int versionCompare(String candidate, String reference) throws esg.common.InvalidVersionStringException {
        return 0;
    }
}