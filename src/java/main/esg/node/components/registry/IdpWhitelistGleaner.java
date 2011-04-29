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
   
   Simple xml list generator.  Creates an xml list of idp urls for
   "trusted" federation members.

**/
public class IdpWhitelistGleaner {
    
    private static final Log log = LogFactory.getLog(IdpWhitelistGleaner.class);
    private IdpWhitelist idps = null;
    private String idpWhitelistFile = "idpWhiteList.xml";
    private String idpWhitelistPath = null;
    private Properties props = null;
    private String defaultLocation = null;

    public IdpWhitelistGleaner() { this(null); }
    public IdpWhitelistGleaner(Properties props) {
        try {
            if(props == null) this.props = new ESGFProperties();
            else this.props = props;

            idpWhitelistPath = props.getProperty("node.manager.app.home",".")+File.separator;
            if (!new File(idpWhitelistPath).exists()) {
                log.warn("Could not locate "+idpWhitelistPath+" - will try to put in /tmp");
                idpWhitelistPath = "/tmp/";
                (new File(idpWhitelistPath)).mkdirs();
            }
                
            idps = new IdpWhitelist();

        } catch(Exception e) {
            log.error(e);
        }
    }
    
    public IdpWhitelist getMyIdpWhitelist() { return idps; }
    
    public boolean saveIdpWhitelist() { return saveIdpWhitelist(idps); }
    public synchronized boolean saveIdpWhitelist(IdpWhitelist idps) {
        boolean success = false;
        if (idps == null) {
            log.error("idps (whitelist) is ["+idps+"]"); 
            return success;
        }
        log.info("Saving IDP Whitelist information to "+idpWhitelistPath+idpWhitelistFile);
        try{
            JAXBContext jc = JAXBContext.newInstance(IdpWhitelist.class);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(idps, new FileOutputStream(idpWhitelistPath+idpWhitelistFile));
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
    public synchronized IdpWhitelistGleaner appendToMyIdpWhitelistFromRegistration(Registration registration) {
        log.info("Creating my IDP whitelist representation...");
        try{
            //NOTE: Entries stored in the registration are dedup'ed so no worries here.
            for(Node node : registration.getNode()) {
                idps.getValue().add(node.getOpenIDProvider().getEndpoint());
            }
        } catch(Exception e) {
            log.error(e);
        }
        
        return this;
    }
    
    public void clear() {
        if(this.idps != null) this.idps = new IdpWhitelist();
    }

    public synchronized IdpWhitelistGleaner loadMyIdpWhitelist() {
        log.info("Loading my IDP Whitelist info from "+idpWhitelistPath+idpWhitelistFile);
        try{
            JAXBContext jc = JAXBContext.newInstance(IdpWhitelist.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<IdpWhitelist> root = u.unmarshal(new StreamSource(new File(idpWhitelistPath+idpWhitelistFile)),IdpWhitelist.class);
            idps = root.getValue();
        }catch(Exception e) {
            log.error(e);
        }
        return this;
    }

    //****************************************************************
    // Not really used methods but here for completeness
    //****************************************************************

    public IdpWhitelist createIdpWhitelistFromString(String idpWhitelistContentString) {
        log.info("Loading my IDP Whitelist info from \n"+idpWhitelistContentString+"\n");
        IdpWhitelist fromContentIdpWhitelist = null;
        try{
            JAXBContext jc = JAXBContext.newInstance(IdpWhitelist.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<IdpWhitelist> root = u.unmarshal(new StreamSource(new StringReader(idpWhitelistContentString)),IdpWhitelist.class);
            fromContentIdpWhitelist = root.getValue();
        }catch(Exception e) {
            log.error(e);
        }
        return fromContentIdpWhitelist;
    }
    
}
