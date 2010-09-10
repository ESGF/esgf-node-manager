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
package esg.node.filters;

import java.io.Serializable;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.Resolver;

//TODO: As a performance feature, perhaps put in a caching mechanism
// you know, path -> targetResource
// The tricky part is putting in the mechanism to invalidate cached items.
// (hey maybe use the java interface to memcache!? :-) sexy...)
// Could be nice use it with the access logging system to histogram access pattern on the fly!

/**
   Utility for Resolving URLs (that adhere to the DRS taxonomy) - to the local resource path, using sql database lookup.
 */
public class UrlResolvingDAO implements Resolver, Serializable {

    private static final String urlResolutionQuery = "select filename from access_logging where ? ? ? ? ? ? ? ? ? ? ?";

    private static final Log log = LogFactory.getLog(UrlResolvingDAO.class);
    
    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private ResultSetHandler<String> resolutionResultSetHandler = null;
    
    private String urlTestRegex="(https?)://(.*)$";
    private Pattern urlTestPattern = null;
    
    private String splitPathRegex="/";
    private Pattern splitPathPattern = null;
    
    private String splitQueryRegex="&";
    private Pattern splitQueryPattern= null;
    
    public UrlResolvingDAO(DataSource dataSource) {
        this.setDataSource(dataSource);
        init();
    }
    
    //Not preferred constructor but here for serialization requirement.
    public UrlResolvingDAO() { init(); }
    
    //Initialize result set handlers...
    public void init() { 
        splitPathPattern  = Pattern.compile(splitPathRegex);
        splitQueryPattern = Pattern.compile(splitQueryRegex);
        urlTestPattern = Pattern.compile(urlTestRegex);
    }
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
        this.resolutionResultSetHandler = new ResultSetHandler<String>() {
            public String handle(ResultSet rs) throws SQLException {
                if(!rs.next()) { return null; }
                return rs.getString(1);
            }
        };
        
    }

    /**
       Resolves the property items to the file resource they represent on the "local" system.
       @param drsProps Property object holding key/value pairs prescribed by the DRS syntax 
       @return The string value referencing the resource the DRS syntax resolves to.
     */
    public String resolveDRSProperties(Properties drsProps) {
        String targetResource = null;
        try{
            //Issue query to resolve the parsed DRS parameters into where the target resource resides on this data-node
            targetResource = queryRunner.query(urlResolutionQuery,resolutionResultSetHandler,
                                               drsProps.getProperty(DRSConstants.PRODUCT),
                                               drsProps.getProperty(DRSConstants.INSTITUTION),
                                               drsProps.getProperty(DRSConstants.MODEL),
                                               drsProps.getProperty(DRSConstants.EXPERIMENT),
                                               drsProps.getProperty(DRSConstants.FREQUENCY),
                                               drsProps.getProperty(DRSConstants.REALM),
                                               drsProps.getProperty(DRSConstants.ENSEMBLE),
                                               drsProps.getProperty(DRSConstants.VERSION),
                                               drsProps.getProperty(DRSConstants.VARIABLE),
                                               drsProps.getProperty(DRSConstants.TABLE,""),
                                               drsProps.getProperty(DRSConstants.FILE));
            
            log.debug("Resolved Resource: "+targetResource);
            return targetResource; 
            
        }catch(SQLException ex) {
            log.error(ex);
        }     
        return targetResource;
    }

    /**
       Resolves a given string for a (virtual) resource to the "local" resource location
       @param input Path or Url to (virtual) resource (described by the DRS taxonomy)
       @return Path to local resource referenced
     */
    public String resolve(String input) {
        try{
            return (urlTestPattern.matcher(input).matches()) ? resolveDRSUrl(input) : resolveDRSPath(input);
        }catch(Throwable t) {
            return null;
        }
    }

    /**
       Helper method for parsing a full cannonoical DRS URL (or the
       query string version) and resolves it to it's "local" resource
       location.
       <p>
       <code>
       Taxonomy - http://product/institution/model/experiment/frequency/realm/ensemble/version/variable_table/file
       </code>

       @param inputUrlString Full cannonical DRS URL to resource (or URL query styled key/value)
       @return The string value referencing the resource the DRS syntax resolves to.
       @see UrlResolvingDAO#resolveDRSPath(String)
    */
    public String resolveDRSUrl(String inputUrlString) throws java.net.MalformedURLException {
        URL inputURL = new URL(inputUrlString);
        return resolveDRSUrl(inputURL);
    }

    /**
       Resolves the given URL object described by the DRS Taxonomy to "local" resource location
       @param inputUrl URL object to (virtual) resource
       @return The string value referencing the resource the DRS syntax resolves to.
     */
    public String resolveDRSUrl(URL inputUrl) {
        String urlQuery = null;
        String result = null;
        if(null == (urlQuery = inputUrl.getQuery())) {
            result = resolveDRSPath(inputUrl.getPath());
        }else {
            result = resolveDRSQuery(urlQuery);
        }
        return result;
    }

    /**
       Parses the path and turns it into a property object suitable
       for resolving. This is the only method that is sensitive to
       field order!<br> The spec on this can be found at the GO-ESSP
       wiki:<br>
       
       <p>

       <code>
       DRS Taxonomy: product/institution/model/experiment/frequency/realm/table/ensemble/version/variable/file
       NOTE: The expected path is intended to NOT begin with a (/)
       </code>

       @param path DRS specified "cannonical" path to this data resource (file)
       @return The string value referencing the resource the DRS syntax resolves to.<p>
       @see  UrlResolvingDAO#resolveDRSProperties(Properties)
    */
    public String resolveDRSPath(String path) {
        //make sure truncate starting "/"
        if((path.length() > 0) && path.startsWith("/")) {
            path = path.substring(1);
        }
        
        //TODO: Do more scrubbing... no "/../" etc...

        log.debug("Resolving Input Path: "+path);

        Properties drsProps = new Properties();
        String[] drsPathElements = splitPathPattern.split(path);
        if(drsPathElements.length != 11) { return null; } //TODO make an exception here and throw it!

        int i = -1;
        drsProps.setProperty(DRSConstants.PRODUCT,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.INSTITUTION,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.MODEL,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.EXPERIMENT,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.FREQUENCY,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.REALM,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.TABLE,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.ENSEMBLE,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.VERSION,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.VARIABLE,drsPathElements[++i]);
        drsProps.setProperty(DRSConstants.FILE,drsPathElements[++i]);
        
        return resolveDRSProperties(drsProps);
    }
    

    /**
       Helper method for parsing a full cannonoical DRS URL and resolves it to it's "local" resource location
       @param inputUrlQuery URL Query values (Ex: ?product=foobar&institution=pcmdi&variable=v&table=t...)
       @return The string value referencing the resource the DRS syntax resolves to.
       @see UrlResolvingDAO#resolveDRSUrl(String)
     */
    String resolveDRSQuery(String inputUrlQuery) {
        if((inputUrlQuery.length() > 0) && inputUrlQuery.startsWith("?")) {
            inputUrlQuery = inputUrlQuery.substring(1);
        }

        Properties drsProps = new Properties();
        String[] queryPairs = splitQueryPattern.split(inputUrlQuery);
        int idx=0;
        for(String drsPair : queryPairs) {
            //Key=Value
            drsProps.setProperty(drsPair.substring(0,(idx=drsPair.indexOf("="))), drsPair.substring(idx+1));
            idx=0;
        }
        return resolveDRSProperties(drsProps);
    }

}
