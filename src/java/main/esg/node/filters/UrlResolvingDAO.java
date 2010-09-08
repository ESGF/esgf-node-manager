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
   Perform sql query to log visitors' access to data files
   
**/
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

public class UrlResolvingDAO implements Serializable {

    private static final String urlResolutionQuery = "select filename from access_logging where ? ? ? ? ? ? ? ? ? ? ?";

    private static final Log log = LogFactory.getLog(UrlResolvingDAO.class);
    
    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private ResultSetHandler<String> resolutionResultSetHandler = null;
    
    private String resolveUrlRegex = "";
    private Pattern resolveUrlPattern = null;
 
    private String splitPathRegex="/";
    private Pattern splitPathPattern = null;

    public UrlResolvingDAO(DataSource dataSource) {
        this.setDataSource(dataSource);
        resolveUrlPattern = Pattern.compile(resolveUrlRegex,Pattern.CASE_INSENSITIVE);
        splitPathPattern  = Pattern.compile(splitPathRegex);
    }
    
    //Not preferred constructor but here for serialization requirement.
    public UrlResolvingDAO() { this(null); }

    //Initialize result set handlers...
    public void init() { }
    
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
                                               drsProps.getProperty(DRSConstants.TABLE),
                                               drsProps.getProperty(DRSConstants.FILE));
            
            log.debug("Resolved Resource: "+targetResource);
            return targetResource; 
            
        }catch(SQLException ex) {
            log.error(ex);
        }     
        return targetResource;
    }

    //Parses the path and turns it into a property object suitable for resolving
    //NOTE: The expected path is intended to NOT begin with a "/"
    public String resolveDRSPath(String path) {
        //make sure truncate starting "/"
        if((path.length() > 0) && path.startsWith("/")) {
            path = path.substring(1);
        }
        
        log.debug("Resolving Input Path: "+path);

        Properties drsProps = new Properties();
        String[] drsPathElements = splitPathPattern.split(path);

        drsProps.setProperty(DRSConstants.PRODUCT,drsPathElements[0]);
        drsProps.setProperty(DRSConstants.INSTITUTION,drsPathElements[1]);
        drsProps.setProperty(DRSConstants.MODEL,drsPathElements[2]);
        drsProps.setProperty(DRSConstants.EXPERIMENT,drsPathElements[3]);
        drsProps.setProperty(DRSConstants.FREQUENCY,drsPathElements[4]);
        drsProps.setProperty(DRSConstants.REALM,drsPathElements[5]);
        drsProps.setProperty(DRSConstants.ENSEMBLE,drsPathElements[6]);
        drsProps.setProperty(DRSConstants.VERSION,drsPathElements[7]);
        drsProps.setProperty(DRSConstants.VARIABLE,drsPathElements[8]);
        drsProps.setProperty(DRSConstants.TABLE,drsPathElements[9]);
        drsProps.setProperty(DRSConstants.FILE,drsPathElements[10]);
        
        return resolveDRSProperties(drsProps);
    }
    
    //URL related methods...

    public String resolveUrl(String inputUrlString) throws java.net.MalformedURLException {
        URL inputURL = new URL(inputUrlString);
        String urlQuery = null;
        if(null == (urlQuery = inputURL.getQuery())) {
            return resolveDRSPath(inputURL.getPath());
        }else {
            return resolveDRSQuery(urlQuery);
        }
    }

    //Knows how to parse query urls
    //Ex: http://pcmdi3.llnl.gov/thredds/fileServer?product=foobar@institution=pcmdi....
    public String resolveDRSQuery(String inputUrlStringQuery) {
        Properties drsProps = new Properties();
        //TODO
        log.error("Implement me! passing in empty properties to be resolved!!!!");
        return resolveDRSProperties(drsProps);
    }

}
