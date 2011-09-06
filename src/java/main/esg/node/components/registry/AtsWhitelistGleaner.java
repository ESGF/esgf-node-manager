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
   
   Simple xml list generator.  Creates an xml list of ats urls for
   "trusted" federation members.

**/
public class AtsWhitelistGleaner {
    
    private static final Log log = LogFactory.getLog(AtsWhitelistGleaner.class);
    private AtsWhitelist atss = null;
    private final String atsWhitelistFile = "esgf_ats.xml";
    private String atsWhitelistPath = null;
    private Properties props = null;
    private String defaultLocation = null;

    public AtsWhitelistGleaner() { this(null); }
    public AtsWhitelistGleaner(Properties props) {
        try {
            if(props == null) this.props = new ESGFProperties();
            else this.props = props;

            if (null != (atsWhitelistPath = System.getenv().get("ESGF_HOME"))) {
                atsWhitelistPath = atsWhitelistPath+"/config/";
            }

            if (!(new File(atsWhitelistPath).exists())) {
                log.warn("Could not locate ["+atsWhitelistPath+"] - will try to put in /tmp");
                atsWhitelistPath = "/tmp/";
                (new File(atsWhitelistPath)).mkdirs();
            }
                
            atss = new AtsWhitelist();

        } catch(Exception e) {
            log.error(e);
        }
    }
    
    public AtsWhitelist getMyAtsWhitelist() { return atss; }
    
    public boolean saveAtsWhitelist() { return saveAtsWhitelist(atss); }
    public synchronized boolean saveAtsWhitelist(AtsWhitelist atss) {
        boolean success = false;
        if (atss == null) {
            log.error("atss (whitelist) is null ? ["+atss+"]"); 
            return success;
        }
        log.trace("Saving ATS Whitelist information to "+atsWhitelistPath+atsWhitelistFile);
        try{
            JAXBContext jc = JAXBContext.newInstance(AtsWhitelist.class);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(atss, new FileOutputStream(atsWhitelistPath+atsWhitelistFile));
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
    public synchronized AtsWhitelistGleaner appendToMyAtsWhitelistFromRegistration(Registration registration) {
        log.trace("Creating my ATS whitelist representation...");
        log.trace("Registration is ["+registration+"]");
        try{
            AttributeService ats = null; //The Attribute service entry from registration
            Attribute attribute = null;
            RegistrationService regSvc = null;
            
            //NOTE: Entries stored in the registration are dedup'ed so no worries here ;-)
            int numNodes = registration.getNode().size();
            int atsNodes = 0;
            log.trace("Registration has ("+numNodes+") nodes");
            for(Node node : registration.getNode()) {
                //TODO - put in sanity check for nodeType integrity
                ats = node.getAttributeService();
                if (null == ats) {
                    log.trace(node.getShortName()+" does not run an Attribute service.");
                    continue;
                }
                
                String service = ats.getEndpoint();
                log.trace("Looking at attribute service: "+service);
                String type = null;
                for(Group group : ats.getGroup()) {
                    type = group.getName();
                    log.trace("gleaning group/attribute: "+type);
                    if (type.equals("wheel")) {
                        log.trace("(skipping wheel");
                        continue;
                    }
                    attribute = new Attribute();
                    attribute.setType(type);
                    attribute.setService(service);
                    atss.getAttribute().add(attribute);
                }
                atsNodes++;
                
                regSvc =  node.getRegistrationService();
                if(null != regSvc) {
                    //zoiks
                }
            }
            log.trace(atsNodes+" of "+numNodes+" gleaned");
        } catch(Exception e) {
            log.error(e);
            e.printStackTrace();
            if(log.isTraceEnabled()) e.printStackTrace();
        }
        
        return this;
    }
    
    public AtsWhitelistGleaner clear() {
        if(this.atss != null) this.atss = new AtsWhitelist();
        return this;
    }

    public synchronized AtsWhitelistGleaner loadMyAtsWhitelist() {
        log.info("Loading my ATS Whitelist info from "+atsWhitelistPath+atsWhitelistFile);
        try{
            JAXBContext jc = JAXBContext.newInstance(AtsWhitelist.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<AtsWhitelist> root = u.unmarshal(new StreamSource(new File(atsWhitelistPath+atsWhitelistFile)),AtsWhitelist.class);
            atss = root.getValue();
        }catch(Exception e) {
            log.error(e);
        }
        return this;
    }

    //****************************************************************
    // Not really used methods but here for completeness
    //****************************************************************

    public AtsWhitelist createAtsWhitelistFromString(String atsWhitelistContentString) {
        log.info("Loading my ATS Whitelist info from \n"+atsWhitelistContentString+"\n");
        AtsWhitelist fromContentAtsWhitelist = null;
        try{
            JAXBContext jc = JAXBContext.newInstance(AtsWhitelist.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<AtsWhitelist> root = u.unmarshal(new StreamSource(new StringReader(atsWhitelistContentString)),AtsWhitelist.class);
            fromContentAtsWhitelist = root.getValue();
        }catch(Exception e) {
            log.error(e);
        }
        return fromContentAtsWhitelist;
    }
    
}
