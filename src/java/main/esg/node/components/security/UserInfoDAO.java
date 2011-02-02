/***************************************************************************
 *                                                                          *
 *  Organization: Lawrence Livermore National Lab (LLNL)                    *
 *   Directorate: Computation                                               *
 *    Department: Computing Applications and Research                       *
 *      Division: S&T Global Security                                       *
 *        Matrix: Atmospheric, Earth and Energy Division                    *
 *       Program: PCMDI                                                     *
 *       Project: Earth Systems Grid (ESG) Data Node Software Stack         *
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
 *   Earth System Grid (ESG) Data Node Software Stack, Version 1.0          *
 *                                                                          *
 *   For details, see http://esgf.org/esg-node/                    *
 *   Please also read this link                                             *
 *    http://esgf.org/LICENSE                                      *
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
   Perform sql query to find out all the people who
   Return Tuple of info needed (dataset_id, recipients/(user), names of updated files)
   
**/
package esg.node.components.security;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.db.DatabaseResource;

public class UserInfoDAO implements Serializable {

    private static final String idQuery = "SELECT firstname, lastname, openid, email FROM user WHERE openid = ?";
    private static final String attributeQuery = "SELECT g.name, r.name from group as g, role as r, permission as p WHERE p.user_id = ? and p.group_id = g.id and p.role_id = r.id ORDER BY g.name"

    
    private static final Log log = LogFactory.getLog(UserInfoDAO.class);

    private Properties props = null;
    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private ResultSetHandler<UserInfo> userInfoResultSetHandler = null;
    private ResultSetHandler<Map<String,Set<String>>> userAttributesResultSetHandler = null;

    //uses default values in the DatabaseResource to connect to database
    public UserInfoDAO() {
        this(new Properties());
    }

    public UserInfoDAO(Properties props) {
        if (props == null) {
            log.warn("Input Properties parameter is: ["+props+"] - creating empty Properties obj");
            props = new Properties();
        }
        
        //This is kind of tricky because the DatabaseResource is meant
        //to be set up once in the earliest part of this application.
        //Subsequent to it's initialziation and setup then any program
        //that needs to use the database can call getInstance.  Since
        //I am not sure where in the codebase I can initialize the
        //DatabaseResource I am doing it here but guarding repeated
        //calls to setupDatasource so that it is ostensibly in the
        //singleton as well.
        
        if (DatabaseResource.getInstance() == null) {
            DatabaseResource.init(props.getProperty("db.driver","org.postgresql.Driver")).setupDataSource(props);
        }
        
        this.setDataSource(DatabaseResource.getInstance().getDataSource());
        this.setProperties(props);
        init();
    }

    public void init() {
        //To handle the single record result
        userInfoResultSetHandler =  new ResultSetHandler<UserInfo>() {
            public UserInfo handle(ResultSet rs) throws SQLException {
                UserInfo userInfo = null;
                while(rs.next()) {
                    userInfo = new UserInfo();
                    userInfo.setFirstName(rs.getString(1))
                        .setLastName(rs.getString(2))
                        .setOpenid(rs.getString(3))
                        .setEmail(rs.getString(4));
                }
                return userInfo;
            }
        };
        
        userAttributesResultSetHandler = new ResultSetHandler<Map<String,Set<String>>>() {
            Map<String,Set<String>> attributes = null;    
            Set<String> attributeValueSet = null;
            
            public Map<String,Set<String>> handle(ResultSet rs) throws SQLException{
                while(rs.next()) {
                    addAttribute(rs.getString(1),rs.getString(2));
                }
                return attributes;
            }
            
            public void addAttribute(String name, String value) {
                //lazily instantiate attributes map
                if(attributes == null) {
                    attributes = new HashMap<String,Set<String>>();
                }
                
                //lazily instantiate the set of values for attribute if not
                //there
                if((attributeValueSet = attributes.get(name)) == null) {
                    attributeValueSet = new HashSet<String>();
                }
                
                //enter attribute associated with attribute value set
                attributeValueSet.add(value);
                attributes.put(name, attributeValueSet);
            }


        };
    }
    
    public void setProperties(Properties props) { this.props = props; }

    public void setDataSource(DataSource dataSource) {
        log.trace("Setting Up SAMLDAO's Pooled Data Source");
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
    }
    
    //------------------------------------
    //Query function calls... 
    //(NOTE: synchronized since there are two calls to database - can optimize around later)
    //------------------------------------
    public synchronized UserInfo getAttributesForId(String identifier) {
        UserInfo userInfo = null;
        try{
            log.trace("Issuing Query for info associated with id: ["+identifier+"], from database");
            if (identifier==null) { return null; }
            userInfo = queryRunner.query(idQuery,userInfoResultSetHandler,identifier);
            userInfo.setAttributes(queryRunner.query(idQuery,userAttributesResultSetHandler,identifier));
            
            //A bit of debugging and sanity checking...
            System.out.println(userInfo);
            
        }catch(SQLException ex) {
            log.error(ex);      
        }
        return userInfo;
    }
    
    //------------------------------------

    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+idQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
        return out.toString();
    }
}
