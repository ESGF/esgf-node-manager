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

import esg.common.generated.registration.*;
import esg.common.generated.whitelist.*;
import esg.common.util.ESGFProperties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.Properties;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

/**
   Description:
   
   Simple xml list generator.  Creates an xml list of azs urls for
   "trusted" federation members.

**/
public class AzsWhitelistGleaner {
    
    private static final Log log = LogFactory.getLog(AzsWhitelistGleaner.class);
    private AzsWhitelist azss = null;
    private final String azsWhitelistFile = "esgf_azs.xml";
    private String azsWhitelistPath = null;
    private Properties props = null;
    private String defaultLocation = null;

    public AzsWhitelistGleaner() { this(null); }
    public AzsWhitelistGleaner(Properties props) {
        try {
            if(props == null) this.props = new ESGFProperties();
            else this.props = props;

            if (null != (azsWhitelistPath = System.getenv().get("ESGF_HOME"))) {
                azsWhitelistPath = azsWhitelistPath+"/config/";
            }

            if (!(new File(azsWhitelistPath).exists())) {
                log.warn("Could not locate ["+azsWhitelistPath+"] - will try to put in /tmp");
                azsWhitelistPath = "/tmp/";
                (new File(azsWhitelistPath)).mkdirs();
            }
                
            azss = new AzsWhitelist();

        } catch(Exception e) {
            log.error(e);
        }
    }
    
    public AzsWhitelist getMyAzsWhitelist() { return azss; }
    
    public boolean saveAzsWhitelist() { return saveAzsWhitelist(azss); }
    public synchronized boolean saveAzsWhitelist(AzsWhitelist azss) {
        boolean success = false;
        if (azss == null) {
            log.error("azss (whitelist) is null ? ["+azss+"]"); 
            return success;
        }
        log.trace("Saving AZS Whitelist information to "+azsWhitelistPath+azsWhitelistFile);
        try{
            JAXBContext jc = JAXBContext.newInstance(AzsWhitelist.class);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(azss, new FileOutputStream(azsWhitelistPath+azsWhitelistFile));
            success = true;
        }catch(Exception e) {
            log.error(e);
        }
	
        return success;
    }

    
    /**
       Looks through the current system and gathers the configured
       node service information.  Takes that information and
       creates a local representation of this node's registration.
    */
    public synchronized AzsWhitelistGleaner appendToMyAzsWhitelistFromRegistration(Registration registration) {
        log.trace("Creating my AZS whitelist representation...");
        log.trace("Registration is ["+registration+"]");
        try{
            AuthorizationService azs = null; //The Authorization service entry from registration

            //NOTE: Entries stored in the registration are dedup'ed so no worries here ;-)
            int numNodes = registration.getNode().size();
            int azsNodes = 0;
            log.trace("Registration has ("+numNodes+") nodes");
            for(Node node : registration.getNode()) {
                //TODO - put in sanity check for nodeType integrity
                azs= node.getAuthorizationService();
                if (null == azs) {
                    log.trace(node.getShortName()+" does not run an Authorization service.");
                    continue;
                }
                azss.getValue().add(azs.getEndpoint());
                azsNodes++;
            }
            log.trace(azsNodes+" of "+numNodes+" gleaned");
        } catch(Exception e) {
            log.error(e);
            e.printStackTrace();
            if(log.isTraceEnabled()) e.printStackTrace();
        }
        
        return this;
    }
    
    public AzsWhitelistGleaner clear() {
        if(this.azss != null) this.azss = new AzsWhitelist();
        return this;
    }

    public synchronized AzsWhitelistGleaner loadMyAzsWhitelist() {
        log.info("Loading my AZS Whitelist info from "+azsWhitelistPath+azsWhitelistFile);
        try{
            JAXBContext jc = JAXBContext.newInstance(AzsWhitelist.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<AzsWhitelist> root = u.unmarshal(new StreamSource(new File(azsWhitelistPath+azsWhitelistFile)),AzsWhitelist.class);
            azss = root.getValue();
        }catch(Exception e) {
            log.error(e);
        }
        return this;
    }

    //****************************************************************
    // Not really used methods but here for completeness
    //****************************************************************

    public AzsWhitelist createAzsWhitelistFromString(String azsWhitelistContentString) {
        log.info("Loading my AZS Whitelist info from \n"+azsWhitelistContentString+"\n");
        AzsWhitelist fromContentAzsWhitelist = null;
        try{
            JAXBContext jc = JAXBContext.newInstance(AzsWhitelist.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<AzsWhitelist> root = u.unmarshal(new StreamSource(new StringReader(azsWhitelistContentString)),AzsWhitelist.class);
            fromContentAzsWhitelist = root.getValue();
        }catch(Exception e) {
            log.error(e);
        }
        return fromContentAzsWhitelist;
    }
    
}
