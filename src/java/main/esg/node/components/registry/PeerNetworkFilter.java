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
package esg.node.components.registry;

import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.Utils;

/**
   Description:

   A utility class for encapsulating the logic for determining network
   affilliation.  The point of this class is to be able to read a peer
   node's network value (at the moment written in the property
   "node.namespace" in the main configuration file) and determine if
   it is a part of the networks this node has been configured to be a
   part of.
   
   Note: Be sure not to put any regex type syntax in the
   node.namespace value.  Perhaps I will scrub the incoming (parsed)
   values as well as I scrub the candidate ones... but for now don't
   put *s and stuff in the value just delimit by ",|:" The * is
   implicit. 
   
   TODO: Do a better job scrubbing the prefix of network strings.
   Ex: simple-org.esgf candiate passes, so does simple-org.esgf-bar
   Be more regimented with the values that can be assigned.

*/
public class PeerNetworkFilter {
    
    private static Log log = LogFactory.getLog(PeerNetworkFilter.class);

    private static final String key = "node.namespace";
    private Matcher networkMatcher = null;

    public PeerNetworkFilter() { }
    public PeerNetworkFilter(Properties props) { init(props); }

    //Parse property value for 'key'...
    public void init(Properties props) {
        try{
            String[] myNetworks = props.getProperty(key).split("[ ]*(?:,|[|]|[:])[ ]*");
            init(myNetworks);
        }catch(Throwable t) {
            log.error(t);
        }
    }

    protected void init(String[] myNetworks) {
        StringBuffer regex = new StringBuffer();
        StringBuffer networks = new StringBuffer(); //for the sake of log output...
        regex.append("^[a-zA-Z0-9,|:_. -]*(");
        int i = 0;
        for(String network : myNetworks) {
            regex.append(network); networks.append(network+"*");
            if (i < (myNetworks.length-1)) { regex.append("|"); networks.append(" + "); }
            i++;
        }
        log.info("Recognizing Peer Network(s): ["+networks.toString()+"]");
        regex.append(")[a-zA-Z0-9,|:_. -]*$");
        log.trace("pattern = "+regex.toString());
        networkMatcher = Pattern.compile(regex.toString()).matcher("");
        networkMatcher.reset();
    }

    public boolean isInNetwork(String candidate) {
        log.trace("network candidate = "+candidate);
        if(networkMatcher == null) {
            log.error("ERROR: NotINITIALIZED - Not able to affirm ANY network affiliation: \n\t"+
                      "Check that configuration property \"node.namespace\" exists and set properly");
            return false;
        }
        networkMatcher.reset(candidate);
        return networkMatcher.find();
    }
    
    //TODO, put this into a JUNIT TEST...
    //keeping it gutter right now cause it's brodying time! :-)
    public static void main(String[] args) {
        Properties p = new Properties();
        p.setProperty("node.namespace","org.esgf,gov.llnl");

        PeerNetworkFilter pnf = new PeerNetworkFilter(p);

        if(pnf.isInNetwork(args[0]))
            System.out.println("YES");
        else
            System.out.println("NO");
    }
    
}