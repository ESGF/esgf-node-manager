/***************************************************************************
*                                                                          *
*  Organization: Earth System Grid Federation                              *
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
*   For details, see http://esgf.org/                                      *
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
   Object representing the esg.ini file's salient features

**/
package esg.common.util;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

public class ESGIni {

    private static Log log = LogFactory.getLog(ESGIni.class);
    
    private File esgIniFile = null;
    private Map<String,String> mountPoints = null;

    public ESGIni() { 
        //Loads the default esg.ini file from $ESGF_HOME/conf/esgcet/esg.ini
        String prefix = System.getenv().get("ESGF_HOME");
        if(prefix != null) {
            loadFile(prefix+File.separator+"conf/esgcet/esg.ini");
        }else{
            log.warn("No ini file loaded - could not get value for $ESGF_HOME!!!!");
        }
    }
    public ESGIni(String esgIniFilename) {
        this.loadFile(new File(esgIniFilename));
    }
    public ESGIni(File esgIniFile) {
        this.loadFile(esgIniFile);
    }
    public ESGIni loadFile(String esgIniFilename) {
        return this.loadFile(new File(esgIniFilename));
    }
    public ESGIni loadFile(File esgIniFile) {
        this.esgIniFile=esgIniFile;
        System.out.println("Loading File: "+esgIniFile.getAbsolutePath());
        //TODO parse the file into its parts
        return this;
    }
    
    public Map<String,String> getMounts() {
        return mountPoints;
    }
}
