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

/**
   Description:

   The web.xml entry...  (NOTE: must appear AFTER all Authorization
   Filters because they put additional information in the request that
   we need like email address and/or userid)

  <!-- Filter for token-based authorization -->
  <filter>
    <filter-name>UrlResolvingFilter</filter-name>
    <filter-class>esg.node.filters.UrlResolvingFilter</filter-class>
    <init-param>
      <param-name>db.driver</param-name>
      <param-value>org.postgresql.Driver</param-value>
      <param-name>db.protocol</param-name>
      <param-value>jdbc:postgresql:</param-value>
      <param-name>db.host</param-name>
      <param-value>localhost</param-value>
      <param-name>db.port</param-name>
      <param-value>5432</param-value>
      <param-name>db.database</param-name>
      <param-value>esgcet</param-value>
      <param-name>db.user</param-name>
      <param-value>dbsuper</param-value>
      <param-name>db.password</param-name>
      <param-value>***</param-value>
      <param-name>extensions</param-name>
      <param-value>.nc,.foo,.bar</param-value>
    </init-param>
  </filter>
  <filter-mapping>
    <filter-name>UrlResolvingFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>

**/
package esg.node.filters;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.util.Properties;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
import org.apache.commons.logging.impl.*;

import esg.common.db.DatabaseResource;

public class UrlResolvingFilter implements Filter {

    private static Log log = LogFactory.getLog(UrlResolvingFilter.class);
    
    FilterConfig filterConfig = null;
    UrlResolvingDAO urlResolvingDAO = null;
    Properties dbProperties = null;
    private Pattern urlPattern = null;


    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("Initializing filter: "+this.getClass().getName());
        this.filterConfig = filterConfig;
        dbProperties = new Properties();
        System.out.println("FilterConfig is : "+filterConfig);
        System.out.println("db.protocol is  : "+filterConfig.getInitParameter("db.protocol"));
        dbProperties.put("db.protocol",filterConfig.getInitParameter("db.protocol"));
        dbProperties.put("db.host",filterConfig.getInitParameter("db.host"));
        dbProperties.put("db.port",filterConfig.getInitParameter("db.port"));
        dbProperties.put("db.database",filterConfig.getInitParameter("db.database"));
        dbProperties.put("db.user",filterConfig.getInitParameter("db.user"));
        dbProperties.put("db.password",filterConfig.getInitParameter("db.password"));

        log.trace("Database parameters: "+dbProperties);

        DatabaseResource.init(filterConfig.getInitParameter("db.driver")).setupDataSource(dbProperties);
        DatabaseResource.getInstance().showDriverStats();
        urlResolvingDAO = new UrlResolvingDAO(DatabaseResource.getInstance().getDataSource());
        
        
        String extensionsParam = filterConfig.getInitParameter("extensions");
        if (extensionsParam == null) { extensionsParam=""; } //defensive program against null for this param
        String[] extensions = (".nc,"+extensionsParam.toString()).split(",");
            
        StringBuffer sb = new StringBuffer();
        for(int i=0 ; i<extensions.length; i++) { 
            sb.append(extensions[i].trim());
            if(i<extensions.length-1) sb.append("|");
        }
        System.out.println("looking for extensions: "+sb.toString());
        String regex = "http.*(?:"+sb.toString()+")$";
        System.out.println("Regex = "+regex);
        
        urlPattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
        
        log.trace(urlResolvingDAO.toString());
    }

    public void destroy() { 
        this.filterConfig = null; 
        this.dbProperties.clear();
        this.urlResolvingDAO = null;
        
        //Shutting down this resource under the assuption that no one
        //else is using this resource but us
        DatabaseResource.getInstance().shutdownResource();
    }

    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest request,
                         ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        
        if(filterConfig == null) return;
        
        boolean success = false;
        
        //firewall off any errors so that nothing stops the show...
        try {
            if(urlResolvingDAO != null) {
                
                //This filter should only appy to specific requests
                //in particular requests for data files (*.nc)
                
                HttpServletResponse res = (HttpServletResponse)response;
                HttpServletRequest  req = (HttpServletRequest)request;
                String url = req.getRequestURL().toString().trim();
                Matcher m = urlPattern.matcher(url);
                
                if(m.matches()) {
                    
                    System.out.println("Executing Url Filter For: "+url);
                    String resourcePath = urlResolvingDAO.resolveDRSUrl(url);
                    //if(null != resourcePath) {
                    //    File resolvedResource = new File(resourcePath);
                    //    if(!resolvedResource.exists()) { 
                    //        log.error("The resolved resource does not exist! ["+url+" -> "+resourcePath+"]");
                    //        ((HttpServletResponse)response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    //                                                  "Requested Resource Not Present! ["+url+"]");
                    //        return;
                    //    }
                    //    String contentType = "application/x-netcdf";
                    //    //ServletUtil.returnFile(req, res, resolvedResource, contentType);
                    //    
                    //}
                    //This would not work... threw IllegalStateException...
                    //filterConfig.getServletContext().getRequestDispatcher(urlResolvingDAO.resolveUrl(url)).forward(request,response);

                    log.debug("*Passing through chain...");
                    chain.doFilter(request, response);
                    
                }else {
                    log.debug("No url resolving for: "+url);
                }
                
            }else{
                log.error("DAO is null :["+urlResolvingDAO+"]");
                HttpServletResponse resp = (HttpServletResponse)response;
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid State Of ESG Url Resolving Filter");
            }
            
        }catch(Throwable t) {
            log.error(t);
            HttpServletResponse resp = (HttpServletResponse)response;
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Caught unforseen Exception in ESG Url Resolving Filter");
        }
        
        log.debug("Passing through chain...");
        chain.doFilter(request, response);

    }
    
}
