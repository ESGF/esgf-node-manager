/***************************************************************************
*                                                                          *
*  Organization: Earth System Grid Federation                              *
*                                                                          *
****************************************************************************
*                                                                          *
*   Copyright (c) 2009, Lawrence Livermore National Security, LLC.         *
*   Produced at the Lawrence Livermore National Laboratory                 *
*                                                                          *
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


package esg.node.filters;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import esg.common.db.DatabaseResource;
import esg.common.util.ESGFProperties;

public class AccessLoggingFilter implements Filter {
    
    FilterConfig filterConfig = null;
    AccessLoggingDAO accessLoggingDAO = null;
    Properties dbProperties = null;
    private Pattern urlExtensionPattern = null;
    private Pattern exemptUrlPattern = null;
    private Pattern exemptServicePattern = null;
    private String serviceName = null;
    
    private static Log log = LogFactory.getLog(AccessLoggingFilter.class);
    
    private static String REGEXS = "REGEXS";

    public void init(FilterConfig filterConfig) throws ServletException {
    	
    	this.filterConfig = filterConfig;
    	
    	// initialize database connection properties from esgf.properties file
        ESGFProperties esgfProperties = null;
        try{
        	
            esgfProperties = new ESGFProperties();
            dbProperties = new Properties();
            dbProperties.put("db.protocol", esgfProperties.getProperty("db.protocol"));
            dbProperties.put("db.host", esgfProperties.getProperty("db.host"));
            dbProperties.put("db.port", esgfProperties.getProperty("db.port"));
            dbProperties.put("db.database", esgfProperties.getProperty("db.database"));
            dbProperties.put("db.user", esgfProperties.getProperty("db.user"));
            dbProperties.put("db.password", esgfProperties.getDatabasePassword());
            dbProperties.put("db.driver", esgfProperties.getProperty("db.driver","org.postgresql.Driver"));

            log.debug("Database parameters: "+dbProperties);
            DatabaseResource.init(dbProperties.getProperty("db.driver","org.postgresql.Driver")).setupDataSource(dbProperties);
            DatabaseResource.getInstance().showDriverStats();
            accessLoggingDAO = new AccessLoggingDAO(DatabaseResource.getInstance().getDataSource());

        } catch (java.io.IOException e) { 
        	e.printStackTrace(); log.error(e); 
        }
        
        serviceName = filterConfig.getInitParameter("service.name");
        
        // extensions that this filter will handle
        urlExtensionPattern = this._getExtensionsPattern("extensions", "http.*(?:"+REGEXS+")$");
        if (log.isInfoEnabled()) log.info("AccessLoggiingFilter: handled extensions:"+urlExtensionPattern.toString());

        // extensions that this filter will NOT handle
        exemptUrlPattern = this._getExtensionsPattern("exempt_extensions", "http.*(?:"+REGEXS+")$");
        if (log.isInfoEnabled()) log.info("AccessLoggiingFilter: exempt extensions:"+exemptUrlPattern.toString());
        
        // patterns that this filter will NOT handle because the output is not file based...
        exemptServicePattern = this._getExtensionsPattern("exempt_services", 
        		                                          "http[s]?://([^:/]*)(:(?:[0-9]*))?/(?:"+REGEXS+")/(.*$)");
        if (log.isInfoEnabled()) log.info("AccessLoggingFilter: exempt services:"+exemptServicePattern.toString());

    }
    
    private Pattern _getExtensionsPattern(String configPatternName, String regexTemplate) {
    	
        // retrieve configuration parameter value
        String extensionsParam = filterConfig.getInitParameter(configPatternName);
        if (extensionsParam == null) { extensionsParam=""; }    //defensive program against null for this param
        String[] extensions = (extensionsParam.toString()).split(",");
            
        // build regular expression
        StringBuffer sb = new StringBuffer();
        for (int i=0 ; i<extensions.length; i++) { 
            sb.append(extensions[i].trim());
            if (i<extensions.length-1) sb.append("|");
        }
        String regex = regexTemplate.replace(REGEXS, sb.toString());
        return Pattern.compile(regex,Pattern.CASE_INSENSITIVE);

    }

    public void destroy() { 

    	// close database connections
        DatabaseResource.getInstance().shutdownResource();
        
    }

    
    public void doFilter(ServletRequest request,
                         ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
                
        // fields to be recorded in the database
    	int id = -1;
        String userID = null;
        String email = null;
        String url = null;
        String fileID = null;
        String remoteAddress = null;
        String userAgent = null;
        long dateFetched = 0L;
        long batchUpdateTime = 0L;
        long startTime = System.currentTimeMillis();
        
        // 1) INTERCEPT REQUEST BEFORE IT IS PROCESSED BY SERVLET
        try {
        	
            HttpServletRequest req = (HttpServletRequest)request;
            url = req.getRequestURL().toString().trim();
            if (log.isInfoEnabled()) log.info("AccessLoggingFilter: intercepting HTTP request for URL="+url);
                                    	
        	// exclude specific extensions
	        Matcher exemptFilesMatcher = exemptUrlPattern.matcher(url);
	        if (exemptFilesMatcher.matches()) {
	        	if (log.isDebugEnabled()) log.debug("NOT logging url="+url+" (extension exempt)");
	            chain.doFilter(request, response);
	            return;
	        }
                
	        // exclude specific services
            Matcher exemptServiceMatcher = exemptServicePattern.matcher(url);
            if (exemptServiceMatcher.matches()) {
            	if (log.isDebugEnabled()) log.debug("NOT logging url="+url+" (service exempt)");
                chain.doFilter(request, response);
                return;
            }

            // detect extensions to be logged
            Matcher allowedFilesMatcher = urlExtensionPattern.matcher(url);
            if (!allowedFilesMatcher.matches()) {
            	if (log.isDebugEnabled()) log.debug("NOT logging url="+url+" (not matching any extension)");
                chain.doFilter(request, response);
                return;
            }

            // retrieve OpenID from previous authentication filter, if available
            userID = ((req.getAttribute("esg.openid") == null) ? 
            		 "<no-id>" : req.getAttribute("esg.openid").toString());
                
            fileID = "0A";
            remoteAddress = req.getRemoteAddr();
            userAgent = (String)req.getAttribute("userAgent");
            dateFetched = System.currentTimeMillis()/1000;

            id = accessLoggingDAO.logIngressInfo(userID,email,url,fileID,remoteAddress,userAgent,serviceName,batchUpdateTime,dateFetched);          
            if (log.isInfoEnabled()) log.info("myID: ["+id+"] = accessLoggingDAO.logIngressInfo(userID: ["+userID+"], email, url: ["+url+"], fileID, remoteAddress, userAgent, serviceName, batchUpdateTime, dateFetched)");
            
        } catch(Exception e) {
            log.error(e);
            e.printStackTrace();
        }
        
        // resume standard request processing
        chain.doFilter(request, response);
               
        // 2) INTERCEPT RESPONSE BEFORE IT IS SENT TO THE CLIENT
        try {
        	
            // retrieve the response size from the response header
            HttpServletResponse resp = (HttpServletResponse)response;
            long dataSize = -1;
            try {
            	dataSize = Integer.parseInt( resp.getHeader("Content-Length") );
            } catch(Exception e) {
            	log.warn(e.getMessage());
            }

            // fields to be recorded after response completes
	        boolean success = false;
	        if (resp.getStatus()==200) success=true;
	        long duration = System.currentTimeMillis() - startTime;
	        long xferSize = -1; // not known since we are not counting the actual bytes transferred
	        if (log.isInfoEnabled()) log.info("AccessLoggingFilter: intercepting response for URL="+url+" Status="+resp.getStatus()+" Content-Length="+dataSize+" Duration="+duration);
	        accessLoggingDAO.logEgressInfo(id, success, duration, dataSize, xferSize);
	        
        } catch(Exception e) {
            log.error(e);
            e.printStackTrace();
        }
        
    }
    
}
