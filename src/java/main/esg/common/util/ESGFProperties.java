/***************************************************************************
 *                                                                          *
 *  Organization: Earth System Grid Federation                              *
 *                                                                          *
 ****************************************************************************
 *                                                                          *
 *   Copyright (c) 2009, Lawrence Livermore National Security, LLC.         *
 *   Produced at the Lawrence Livermore National Laboratory                 *
 *   LLNL-CODE-420962                                                       *
 *                                                                          *
 *   All rights reserved. This file is part of the:                         *
 *   Earth System Grid (ESG) Data Node Software Stack, Version 1.0          *
 *                                                                          *
 *   For details, see http://esg-repo.llnl.gov/esg-node/                    *
 *   Please also read this link                                             *
 *    http://esg-repo.llnl.gov/LICENSE                                      *
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
package esg.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ESGFProperties extends Properties {
    
    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public ESGFProperties() throws IOException, FileNotFoundException {

        // initialize empty Properties
        super();

        // load ESGF property values
        File propertyFile = new File( System.getenv().get("ESGF_HOME")+"/config/esgf.properties" );
        if (!propertyFile.exists()) propertyFile = new File("/esg/config/esgf.properties");
        this.load( new FileInputStream(propertyFile) );
        if (log.isInfoEnabled()) log.info("Loading properties from file: "+propertyFile.getAbsolutePath());

    }

    private String securityAdminPassword = null;

    public String getAdminPassword() { return this.getAdminPassword(false); }
    public String getAdminPassword(boolean force) { 
        if((securityAdminPassword == null) || force) {
            securityAdminPassword = readFromFile(".esgf_pass"); 
        }
        return securityAdminPassword;
    }
    public String loadAdminPasswordAs(String propertyKey) {
        return loadAdminPasswordAs(propertyKey,false);
    }
    public String loadAdminPasswordAs(String propertyKey, boolean force) {
        this.setProperty(propertyKey,getAdminPassword(force));
        return this.getProperty(propertyKey);
    }

    private String securityDatabasePassword = null;

    public String getDatabasePassword() { return this.getDatabasePassword(false); }
    public String getDatabasePassword(boolean force) { 
        if((securityDatabasePassword == null) || force) {
            securityDatabasePassword = readFromFile(".esgf_pass"); 
        }
        return securityDatabasePassword;
    }
    public String loadDatabasePasswordAs(String propertyKey) {
        return loadDatabasePasswordAs(propertyKey,false);
    }
    public String loadDatabasePasswordAs(String propertyKey, boolean force) {
        this.setProperty(propertyKey,getDatabasePassword(force));
        return this.getProperty(propertyKey);
    }


    /**
       Reads a single-line config file in ESGF_HOME/config directory
     */
    private String readFromFile(String filename) {
        String value = null;
        String configDir = null;
        if (null != (configDir = System.getenv().get("ESGF_HOME"))) {
            configDir = configDir+File.separator+"config";
            try {
                File configFile = new File(configDir+File.separator+filename);
                if(configFile.exists()) {
                    BufferedReader in = new BufferedReader(new FileReader(configFile));
                    try{
                        value = in.readLine().trim();
                        log.trace("data = "+value);
                    }catch(java.io.IOException ex) {
                        log.error(ex);
                    }finally {
                        if(null != in) in.close();
                    }
                }
            }catch(Throwable t) {
                log.error(t);
            }
        }
        return value;
    }


}
