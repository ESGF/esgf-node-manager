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

public class GroupRoleDAO implements Serializable {

    //-------------------
    //Insertion queries
    //-------------------
    
    //Group Queries...
    private static final String hasGroupNameQuery =
        "SELECT * form esgf_security.group "+
        "WHERE name = ?";
    private static final String updateGroupQuery = 
        "UPDATE esgf_security.group "+
        "SET name=? "+
        "WHERE id=?";
    private static final String getNextGroupPrimaryKeyValQuery = 
        "SELECT NEXTVAL('esgf_security.group_id_seq')";
    private static final String addGroupQuery = 
        "INSERT INTO esgf_security.group (id, name) "+
        "VALUES ( ?, ? )";

    //Role Queries...
    private static final String hasRoleNameQuery =
        "SELECT * form esgf_security.role "+
        "WHERE name = ?";
    private static final String updateRoleQuery = 
        "UPDATE esgf_security.role "+
        "SET name=? "+
        "WHERE id=?";
    private static final String getNextRolePrimaryKeyValQuery  = 
        "SELECT NEXTVAL('esgf_security.role_id_seq')";
    private static final String addRoleQuery = 
        "INSERT INTO esgf_security.role (id, name) "+
        "VALUES ( ?, ? )";

    //-------------------

    
    private static final Log log = LogFactory.getLog(GroupRoleDAO.class);

    private Properties props = null;
    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private ResultSetHandler<Map<String,Set<String>>> userGroupsResultSetHandler = null;
    private ResultSetHandler<Integer> idResultSetHandler = null;

    //uses default values in the DatabaseResource to connect to database
    public GroupRoleDAO() {
        this(new Properties());
    }

    public GroupRoleDAO(Properties props) {
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
        this.idResultSetHandler = new ResultSetHandler<Integer>() {
		    public Integer handle(ResultSet rs) throws SQLException {
                if(!rs.next()) { return -1; }
                return rs.getInt(1);
		    }
		};
        
        userGroupsResultSetHandler = new ResultSetHandler<Map<String,Set<String>>>() {
            Map<String,Set<String>> groups = null;    
            Set<String> roleSet = null;
            
            public Map<String,Set<String>> handle(ResultSet rs) throws SQLException{
                while(rs.next()) {
                    addGroup(rs.getString(1),rs.getString(2));
                }
                return groups;
            }
            
            public void addGroup(String name, String value) {
                //lazily instantiate groups map
                if(groups == null) {
                    groups = new HashMap<String,Set<String>>();
                }
                
                //lazily instantiate the set of values for group if not
                //there
                if((roleSet = groups.get(name)) == null) {
                    roleSet = new HashSet<String>();
                }
                
                //enter group associated with group value set
                roleSet.add(value);
                groups.put(name, roleSet);
            }


        };
    }
    
    public void setProperties(Properties props) { this.props = props; }

    public void setDataSource(DataSource dataSource) {
        log.trace("Setting Up GroupRoalDAO's Pooled Data Source");
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
    }
    
    public synchronized int addGroup(String groupName) {
        int groupid = -1;
        int numRowsAffected = -1;
        
        try{
            //Check to see if there is an entry by this name already....
            groupid = queryRunner.query(hasGroupNameQuery,idResultSetHandler,groupName);
            
            //If there *is*... then UPDATE that record
            if(groupid > 0) {
                numRowsAffected = queryRunner.update(updateGroupQuery,
                                                     groupName, groupid);
                return numRowsAffected;
            }
            
            //If this group does not exist in the database then add (INSERT) a new one
            groupid = queryRunner.query(getNextGroupPrimaryKeyValQuery, idResultSetHandler);
            numRowsAffected = queryRunner.update(addGroupQuery,groupid,groupName);
        }catch(SQLException ex) {
            log.error(ex);
        }
        return numRowsAffected;
    }
    
    public synchronized int addRole(String roleName) {
        int roleid = -1;
        int numRowsAffected = -1;
        try{
            //Check to see if there is an entry by this name already....
            roleid = queryRunner.query(hasRoleNameQuery,idResultSetHandler,roleName);
            
            //If there *is*... then UPDATE that record
            if(roleid > 0) {
                numRowsAffected = queryRunner.update(updateRoleQuery,
                                                     roleName, roleid);
                return numRowsAffected;
            }
            
            //If this role does not exist in the database then add (INSERT) a new one
            roleid = queryRunner.query(getNextRolePrimaryKeyValQuery,idResultSetHandler);
            numRowsAffected = queryRunner.update(addRoleQuery,roleid,roleName);
        }catch(SQLException ex) {
            log.error(ex);
        }
        return numRowsAffected;
    }

    
    //------------------------------------
    
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("DAO:["+this.getClass().getName()+"] - "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
        return out.toString();
    }
}
