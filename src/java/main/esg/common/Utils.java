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
    private static final String httpSchemeRegex = "(http)(:.*)";
    private static final Pattern httpSchemePattern = Pattern.compile(httpSchemeRegex,Pattern.CASE_INSENSITIVE);
    private static final String httpsSchemeRegex = "(https)(:.*)";
    private static final Pattern httpsSchemePattern = Pattern.compile(httpsSchemeRegex,Pattern.CASE_INSENSITIVE);
    private static final String urlRegex = "http[s]?://([^:/]*)(:(?:[0-9]*))?/(.*/)*(.*$)";
    private static final Pattern urlPattern = Pattern.compile(urlRegex,Pattern.CASE_INSENSITIVE);
    private static final String serviceUrlRegex = "http[s]?://([^:/]*)(:(?:[0-9]*))?/esgf-node-manager/node";
    private static final Pattern serviceUrlPattern = Pattern.compile(serviceUrlRegex,Pattern.CASE_INSENSITIVE);
    private static QuickHash quickHash = null;

    //For version compare methods...
    private static final Pattern versionPattern = Pattern.compile("[v.-]([0-9]*)");

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
            (socket = new Socket()).connect(new InetSocketAddress(java.net.InetAddress.getByName(host),port),timeout);
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

    public static String asSSLUrl(String url) {
        if(url.startsWith("https")) return url;
        Matcher m = httpSchemePattern.matcher(url);
        if(m.find()) url = "https"+m.group(2);
        return url;
    }

    //Note: Uses Regex to check for https scheme instead of String's startsWith()...
    public static String asSSLUrlx(String url) {
        if ((httpsSchemePattern.matcher(url)).find()) return url;
        Matcher m = httpSchemePattern.matcher(url);
        if(m.find()) url = "https"+m.group(2);
        return url;
    }

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


    //------------------------------------------------------------------------------------
    //Util version string comparison functions...
    //------------------------------------------------------------------------------------

    /**
       Compares two version strings and returns comparator compatible
       return values indicating relationship between the versions.
       This uses the Util.versionPattern regex to parse pattern values
       expected in the form v#.#.# (hyphens are also allowed as
       delimiters).

       @param candidate the first version value to compare
       @param reference the second version value to compare
       @throws InvalideVersionStringException runtime exception thrown when a value cannot be numerically parsed.
       @return 1 if candidate > reference; -1 if candidate < reference; 0 if candidate = refernece;
     */
    public static int versionCompare(String candidate, String reference) throws esg.common.InvalidVersionStringException {
        if(candidate.equals(reference)) return 0;
        return versionCompare(candidate, versionPattern.matcher(candidate),
                              reference, versionPattern.matcher(reference));
    }

    /**
       In an attempt to be a bit more efficient.  If there is a lot of
       comparing that needs to be done it is better to pass in
       matchers, rather than have the matchers be instantiated by the
       Pattern every time a comparison is done.  Matchers are not
       thread safe therefore this method is NOT thread safe.  So be
       aware.  If in doubt call the other version of this method.

       @param candidate the first version value to compare
       @param c_matcher a Matcher object from a Pattern that knows how to tokenize the version values
       @param reference the second version value to compare
       @param r_matcher a Matcher object from a Pattern that knows how to tokenize the version values
       @throws InvalideVersionStringException runtime exception thrown when a value cannot be numerically parsed.
       @return 1 if candidate > reference; -1 if candidate < reference; 0 if candidate = refernece;
     */
    public static int versionCompare(String candidate, Matcher c_matcher, String reference, Matcher r_matcher)
        throws esg.common.InvalidVersionStringException {
        c_matcher.reset(candidate);
        r_matcher.reset(reference);

        int c, r = 0;
        try{
            while (c_matcher.find() && r_matcher.find()) {
                c = Integer.parseInt(c_matcher.group(1));
                r = Integer.parseInt(r_matcher.group(1));

                //log.trace("Comparing "+c+" and "+r);
                if (c > r) return 1;
                else if (c < r) return -1;
            }

            //When version positions don't line up...
            int c_len = candidate.length();
            int r_len = reference.length();
            //log.trace("candidate length = "+c_len);
            //log.trace("reference length = "+r_len);
            if(c_len > r_len) {
                c_matcher.reset(candidate.substring(r_len));
                while(c_matcher.find()) {
                    if(Integer.parseInt(c_matcher.group(1)) > 0) { return 1; }
                }
            }
            if(c_len < r_len) {
                r_matcher.reset(reference.substring(c_len));
                while(r_matcher.find()) {
                    if(Integer.parseInt(r_matcher.group(1)) > 0) { return -1; }
                }
            }
            return 0;
        }catch(NumberFormatException e) {
            log.error("Improper version string! "+e.getMessage());
            throw new InvalidVersionStringException(e);
        }catch(Exception e) {
            log.error("Improper version string! "+e.getMessage());
            throw new InvalidVersionStringException(e);
        }
    }

}