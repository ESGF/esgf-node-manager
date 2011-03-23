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
import esg.common.util.ESGFProperties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

/**
   Description:
   
   Encapsulates the logic for fetching and generating this local
   node's registration form, as defined by the registration.xsd.  (The
   LasSistersGleaner is also used to further create the
   las_servers.xml derivative file.)

*/
public class RegistrationGleaner {

    private static final Log log = LogFactory.getLog(RegistrationGleaner.class);
    
    private static final String registrationFile = "registration.xml";
    private static final boolean DEBUG=true;
    private String registrationPath = null;
    private Registration myRegistration = null;
    private Properties props = null;
    private RegistrationGleanerHelperDAO helperDAO = null;

    public RegistrationGleaner() { this(null); }
    public RegistrationGleaner(Properties props) { 
        this.props = props;
        this.init(); 
    }

    public void init() {
        try {
            if(props == null) this.props = new ESGFProperties();
        } catch(Exception e) {
            log.error(e);
        }
        registrationPath = props.getProperty("node.manager.app.home",".")+File.separator;
    }
    
    
    public Registration getMyRegistration() { return myRegistration; }
    
    public boolean saveRegistration() { return saveRegistration(myRegistration); }
    public boolean saveRegistration(Registration registration) {
        boolean success = false;
        if (registration == null) {
            log.error("Registration is ["+registration+"]"); 
            return success;
        }
        log.info("Saving registration information for "+registration.getNode().get(0).getHostname()+" to "+
                 props.getProperty(registrationPath+this.registrationFile));
        try{
            JAXBContext jc = JAXBContext.newInstance(Registration.class);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(registration, new FileOutputStream(registrationPath+this.registrationFile));
            
            success = true;
        }catch(Exception e) {
            log.error(e);
        }
        
        //pull from registry to create las sisters file.
        try{
            String endpoint=null;
            if( (null != (endpoint=props.getProperty("las.endpoint"))) &&
                (new File(props.getProperty("las.app.home"))).exists() ) {
                LasSistersGleaner lasSisterGleaner = new LasSistersGleaner(props); 
                lasSisterGleaner.appendToMyLasServersFromRegistration(registration).saveLasServers();
            }
        }catch(Exception e) {
            log.error(e);
        }
        
        return success;
    }
    
    public String toString() {
        StringWriter sw = null;
        if (myRegistration == null) {
            log.error("Registration is ["+myRegistration+"]"); 
            return null;
        }
        log.info("Writing registration information to String, for "+myRegistration.getNode().get(0).getHostname());
        sw = new StringWriter();
        try{
            JAXBContext jc = JAXBContext.newInstance(Registration.class);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(myRegistration, sw);
        }catch(Exception e) {
            log.error(e);
        }
        return sw.toString();
    }

    
    /**
       Looks through the current system and gathers the configured
       node service information.  Takes that information and
       creates a local representation of this node's registration.
    */
    public RegistrationGleaner createMyRegistration() {
        log.info("Creating my registration representation...");
        String endpointDir = null;
        String endpoint = null;
        Node node = new Node();
        try{
            String nodeHostname =props.getProperty("esgf.host");

            //************************************************
            //CORE
            //************************************************

            //Query the user for...
            node.setOrganization(props.getProperty("esg.root.id"));
            node.setLongName(props.getProperty("node.long.name"));
            node.setShortName(props.getProperty("node.short.name"));
            node.setSupportEmail(props.getProperty("mail.admin.address"));
            
            //Pulled from system or install
            node.setHostname(nodeHostname);
            node.setDN(props.getProperty("node.dn")); //zoiks
            node.setNamespace(props.getProperty("node.namespace")); //zoiks
            node.setTimeStamp((new Date()).getTime());

            //What is this ?
            CA ca = new CA();
            ca.setEndpoint(props.getProperty("esgf.host","dunno"));
            ca.setHash(props.getProperty("security.ca.hash","dunno"));
            ca.setDN(props.getProperty("security.ca.dn","dunno"));
            node.setCA(ca);

            //************************************************
            //Data
            //************************************************

            try{
                if( (null != (endpoint=props.getProperty("thredds.endpoint"))) &&
                    (new File(props.getProperty("thredds.app.home"))).exists() ) {
                    ThreddsService tds = new ThreddsService();
                    tds.setEndpoint(endpoint);
                    node.setThreddsService(tds);
                }
            }catch(Throwable t) {
                log.error(t);
            }

            try{
                if( (null != (endpoint=props.getProperty("relyingparty.endpoint"))) &&
                    (new File(props.getProperty("relyingparty.app.home"))).exists() ) {
                    RelyingPartyService orp = new RelyingPartyService();
                    orp.setEndpoint(endpoint);
                    node.setRelyingPartyService(orp);
                }
            }catch(Throwable t) {
                log.error(t);
            }


            //------------------------------------------------
            //GLOBUS SUPPORT TOOLS
            //------------------------------------------------

            try{
                if( (null != (endpoint=props.getProperty("myproxy.endpoint"))) &&
                    (new File(props.getProperty("myproxy.app.home"))).exists() ) { //zoiks
                    MyProxyService mproxy = new MyProxyService();
                    mproxy.setEndpoint(endpoint);
                    mproxy.setDN(props.getProperty("mproxy.dn"));
                    node.setMyProxyService(mproxy);
                }
            }catch(Throwable t) {
                log.error(t);
            }
            
            try{
                if( (null != (endpoint=props.getProperty("gridftp.endpoint"))) &&
                    (new File(props.getProperty("gridftp.app.home"))).exists() ) {
                    GridFTPService gftp = new GridFTPService();
                    gftp.setEndpoint(endpoint);
                    String serviceType = props.getProperty("gridftp.service.type");
                    if (serviceType != null) {
                        if (serviceType.equals(GridFTPServiceType.REPLICATION.value())) {
                            gftp.setServiceType(GridFTPServiceType.REPLICATION);
                        }else{
                            gftp.setServiceType(GridFTPServiceType.DOWNLOAD);
                        }
                    }else {
                        gftp.setServiceType(GridFTPServiceType.DOWNLOAD);
                    }
                    node.setGridFTPService(gftp);
                }
            }catch(Throwable t) {
                log.error(t);
            }
            
            //************************************************
            //INDEX
            //************************************************
            
            try{
                if( (null != (endpoint=props.getProperty("index.endpoint"))) &&
                    (new File(props.getProperty("index.app.home"))).exists() ) {
                    IndexService idx = new IndexService();
                    idx.setEndpoint(endpoint);
                    node.setIndexService(idx);
                }
            }catch(Throwable t) {
                log.error(t);
            }

            try{
                if( (null != (endpoint=props.getProperty("publisher.endpoint"))) &&
                    (new File(props.getProperty("publisher.app.home"))).exists() ) {
                    PublishingService pub = new PublishingService();
                    pub.setEndpoint(endpoint);
                    node.setPublishingService(pub);
                }
            }catch(Throwable t) {
                log.error(t);
            }

            //************************************************
            //COMPUTE
            //************************************************

            try{
                if( (null != (endpoint=props.getProperty("las.endpoint"))) &&
                    (new File(props.getProperty("las.app.home"))).exists() ) {
                    LASService las = new LASService();
                    las.setEndpoint(endpoint);
                    node.setLASService(las);
                }
            }catch(Throwable t) {
                log.error(t);
            }

            //************************************************
            //IDP (security)
            //************************************************

            //esgf-idp
            try{
                if( (null != (endpoint=props.getProperty("idp.service.endpoint"))) &&
                    (new File(props.getProperty("idp.app.home"))).exists() ) {
                    OpenIDProvider openid = new OpenIDProvider();
                    openid.setEndpoint(endpoint);
                    node.setOpenIDProvider(openid);
                }
            }catch(Throwable t) {
                log.error(t);
            }

            //esgf-security
            try{
                if( (null != (endpoint=props.getProperty("security.authz.service.endpoint"))) &&
                    (new File(props.getProperty("security.app.home"))).exists() ) {
                    AuthorizationService authzSvc = new AuthorizationService();
                    authzSvc.setEndpoint(endpoint);
                    node.setAuthorizationService(authzSvc);
                }
            }catch(Throwable t) {
                log.error(t);
            }

            //esgf-security
            try{
                if( (null != (endpoint=props.getProperty("security.attribute.service.endpoint"))) &&
                    (new File(props.getProperty("security.app.home"))).exists() ) {
                    AttributeService attrSvc = new AttributeService();
                    attrSvc.setEndpoint(endpoint);
                    loadAttributeServiceGroups(attrSvc);
                    node.setAttributeService(attrSvc);
                }
            }catch(Throwable t) {
                log.error(t);
            }
	    
            PEMCert cert = new PEMCert();
            cert.setCert("PLACE HOLDER STRING FOR PEM DATA (ZOIKS)");
            node.setPEMCert(cert);
            
            
            //************************************************
            //INDEX DEPRECATED SERVICE
            //************************************************
            //OAIRepository oaiRepo = new OAIRepository();
            //oaiRepo.setEndpoint("oairepo-endpoint");
            //node.setOAIRepository(oaiRepo);			

        } catch(Exception e) {
            log.error(e);
        }

        myRegistration = new Registration();
        myRegistration.getNode().add(node);
        return this;
    }
    
    public RegistrationGleaner loadMyRegistration() {
        log.info("Loading my registration info from "+registrationPath+registrationFile);
        try{
            JAXBContext jc = JAXBContext.newInstance(Registration.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<Registration> root = u.unmarshal(new StreamSource(new File(registrationPath+this.registrationFile)),Registration.class);
            myRegistration = root.getValue();
        }catch(Exception e) {
            log.error(e);
        }
        return this;
    }

    public Registration createRegistrationFromString(String registrationContent) {
        log.info("Loading my registration info from \n"+registrationContent+"\n");
        Registration fromContentRegistration = null;
        try{
            JAXBContext jc = JAXBContext.newInstance(Registration.class);
            Unmarshaller u = jc.createUnmarshaller();
            JAXBElement<Registration> root = u.unmarshal(new StreamSource(new StringReader(registrationContent)),Registration.class);
            fromContentRegistration = root.getValue();
        }catch(Exception e) {
            log.error(e);
        }
        return fromContentRegistration;
    }

    //Delegate out to our helper so we can get things out of the
    //esgf_security.group database table
    private void loadAttributeServiceGroups(AttributeService attrSvc) {
        //lazy instantiate this guy... Because it is only used when
        //this service is available, which is not on every node, so
        //don't waste memory :-) -gavin
        if (helperDAO == null) {
            helperDAO = new RegistrationGleanerHelperDAO(props);
        }
        helperDAO.loadAttributeServiceGroups(attrSvc);
    }

    //Allow this class to be used as a command line tool for bootstrapping this node.
    public static void main(String[] args) {
        if(args.length > 0) {
            if(args[0].equals("bootstrap")) {
                System.out.println(args[0]+"ing...");
                (new RegistrationGleaner()).createMyRegistration().saveRegistration();
            }else if(args[0].equals("load")) {
                System.out.println(args[0]+"ing...");
                //(new RegistrationGleaner()).loadMyRegistration().saveRegistration();
                System.out.println((new RegistrationGleaner()).loadMyRegistration());
            }else {
                System.out.println("illegal arg: "+args[0]);
            }
        }
    }
    
}