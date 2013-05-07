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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

public class ESGIni {

    private static Log log = LogFactory.getLog(ESGIni.class);

    private static final String regexBegin = "[ ]*thredds_dataset_roots[ ]*=";
    private static final Pattern beginPat = Pattern.compile(regexBegin,Pattern.CASE_INSENSITIVE);

    private static final String regexEnd = "[ ]*=[ ]*";
    private static final Pattern endPat = Pattern.compile(regexEnd,Pattern.CASE_INSENSITIVE);

    private static final String regexEntries = "[ ]*([^|]*)(.*)$";
    private static final Pattern entryPat = Pattern.compile(regexEntries,Pattern.CASE_INSENSITIVE);
    
    private Map<String,String> mountPoints = new HashMap<String,String>(5);

    public ESGIni() { 
        //Loads the default esg.ini file from $ESGF_HOME/conf/esgcet/esg.ini
        String esgfHome = System.getenv().get("ESGF_HOME");
        if(esgfHome != null) {
            loadFile(esgfHome+File.separator+"conf/esgcet/esg.ini");
        }else{
            log.warn("No ini file loaded - could not get value for $ESGF_HOME!!!!");
        }
    }

    public ESGIni loadFile(String filename) { 
        File file = new File(filename);
        if (file.exists()) {
            this.loadFile(file);
        }else {
            System.out.println("Sorry file ["+filename+"] does not exist on local filesystem");
            //return this;
        }
        return this;
    }

    public ESGIni loadFile(File f) {
        System.out.println("Loading File: "+f.getAbsolutePath());
        BufferedReader buff = null;
        try{
            buff = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String line = null;
            Matcher begin = beginPat.matcher("");
            Matcher end = endPat.matcher("");
            Matcher entry = entryPat.matcher("");
            boolean in = false;
            while((line = buff.readLine()) != null) {
                //System.out.println("buff -> "+line);
                begin.reset(line);
                if(begin.find()) { in=true; continue; }
                if(in) {
                    //System.out.println("IN -> "+line);
                    entry.reset(line);
                    if(entry.find()) {
                        String mountPoint = entry.group(1).trim();
                        String localpath = entry.group(2);
                        if (localpath.startsWith("|")) localpath = localpath.substring(1).trim();
                        //System.out.println("Entry = m:["+mountPoint+"] -> l:["+localpath+"]");
                        if( !mountPoint.isEmpty() && !localpath.isEmpty()) {
                            System.out.println("Mount Point: ["+mountPoint+"] --> ["+localpath+"]");
                            mountPoints.put(mountPoint,localpath);
                        }
                    }else {
                        in=false;
                        break;
                    }
                }
                end.reset(line);
                if(end.find() && in) { in=false; break; }
            }
        }catch(java.io.IOException e) {
            e.printStackTrace();
        }finally {
            try { buff.close(); } catch(Throwable t) { }
        }
        return this;
    }

    public Map<String,String> getMounts() {
        return mountPoints;
    }

    public ESGIni clear() {
        mountPoints.clear();
        return this;
    }

}
