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

import static org.apache.commons.codec.digest.DigestUtils.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.db.DatabaseResource;
import static esg.common.Utils.*;

public class UserInfoDAO implements Serializable {

    //-------------------
    //Selection queries
    //-------------------
    private static final String idQuery = 
        "SELECT id, openid, firstname, middlename, lastname, username, email, organization, organization_type, city, state, country "+
        "FROM esgf_security.user "+
        "WHERE openid = ?";

    private static final String groupQuery = 
        "SELECT g.name, r.name from esgf_security.group as g, esgf_security.role as r, esgf_security.permission as p, esgf_security.user as u "+
        "WHERE p.user_id = u.id and u.openid = ? and p.group_id = g.id and p.role_id = r.id "+
        "ORDER BY g.name";

    //-------------------
    //Insertion queries
    //-------------------
    
    //User Queries...
    private static final String hasUserOpenidQuery =
        "SELECT * from esgf_security.user "+
        "WHERE openid = ?";
    private static final String updateUserQuery = 
        "UPDATE esgf_security.user "+
        "SET openid = ?, firstname = ?, middlename = ?, lastname = ?, username = ?, email = ?, dn = ?, organization = ?, organization_type = ?, city = ?, state = ?, country = ? "+
        "WHERE id = ? ";
    private static final String getNextUserPrimaryKeyValQuery = 
        "SELECT NEXTVAL('esgf_security.user_id_seq')";
    private static final String addUserQuery = 
        "INSERT INTO esgf_security.user (id, openid, firstname, middlename, lastname, username, email, dn, organization, organization_type, city, state, country) "+
        "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    //Permission Queries...
    private static final String addPermissionQuery = 
        "INSERT INTO esgf_security.permission (user_id, grou_id, role_id) "+
        "VALUES ( ?, ? )";

    //Password Queries...
    private static final String setPasswordQuery = 
        "UPDATE esgf_security.user SET password = ? "+
        "WHERE openid = ?";

    private static final String getPasswordQuery = 
        "SELECT password FROM esgf_security.user WHERE openid = ?";

    //-------------------

    
    private static final Log log = LogFactory.getLog(UserInfoDAO.class);

    private Properties props = null;
    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private ResultSetHandler<UserInfo> userInfoResultSetHandler = null;
    private ResultSetHandler<Map<String,Set<String>>> userGroupsResultSetHandler = null;
    private ResultSetHandler<Integer> idResultSetHandler = null;
    private ResultSetHandler<String> passwordQueryHandler = null;
    
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
        this.idResultSetHandler = new ResultSetHandler<Integer>() {
	    public Integer handle(ResultSet rs) throws SQLException {
                if(!rs.next()) { return -1; }
                return rs.getInt(1);
	    }
	};
        
        //To handle the single record result
        userInfoResultSetHandler =  new ResultSetHandler<UserInfo>() {
            public UserInfo handle(ResultSet rs) throws SQLException {
                UserInfo userInfo = null;
                while(rs.next()) {
                    userInfo = new UserInfo();
                    userInfo.setid(rs.getInt(1))
                        .setOpenid(rs.getString(2))
                        .setFirstName(rs.getString(3))
                        .setMiddleName(rs.getString(4))
                        .setLastName(rs.getString(5))
                        .setUserName(rs.getString(6))
                        .setEmail(rs.getString(7))
                        .setOrganization(rs.getString(8))
                        .setOrgType(rs.getString(9))
                        .setCity(rs.getString(10))
                        .setState(rs.getString(11))
                        .setCountry(rs.getString(12));
                }
                return userInfo;
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
	
	passwordQueryHandler = new ResultSetHandler<String>() {
	    public String handle(ResultSet rs) throws SQLException {
		String password = null;
		while(rs.next()) {
		    password = rs.getString(1);
		}
		return password;
	    }
	};
    }
    
    public void setProperties(Properties props) { this.props = props; }
    
    public void setDataSource(DataSource dataSource) {
        log.trace("Setting Up UserInfoDAO's Pooled Data Source");
        this.dataSource = dataSource;
        this.queryRunner = new QueryRunner(dataSource);
    }
    
    //------------------------------------
    //Query function calls... 
    //(NOTE: synchronized since there are two calls to database - can optimize around later)
    //------------------------------------
    public synchronized UserInfo getUserById(String openid) {
        UserInfo userInfo = null;
        int affectedRecords = 0;
        try{
            log.trace("Issuing Query for info associated with id: ["+openid+"], from database");
            if (openid==null) { return null; }
            userInfo = queryRunner.query(idQuery,userInfoResultSetHandler,openid);
            userInfo.setGroups(queryRunner.query(groupQuery,userGroupsResultSetHandler,openid));
            
            //A bit of debugging and sanity checking...
            System.out.println(userInfo);
            
        }catch(SQLException ex) {
            log.error(ex);      
        }
        return userInfo;
    }

    synchronized boolean addUserInfo(UserInfo userInfo) {
        int userid = -1;
        int groupid = -1;
        int roleid = -1;
        int numRowsAffected = -1;
        try{
            log.trace("Inserting UserInfo associated with username: ["+userInfo.getUserName()+"], into database");
            //THE ONLY PLACE OPEN ID IS SET!!! UPON INITIAL SUBMISSION ONLY!!!
            if(userInfo.getOpenid() == null) {
                userInfo.setOpenid("https://"+getFQDN()+"/esgf-idp/openid/"+userInfo.getUserName());
            }
            System.out.println("Openid is ["+userInfo.getOpenid()+"]");
            
            //Check to see if there is an entry by this openid already....
            userid = queryRunner.query(hasUserOpenidQuery,idResultSetHandler,userInfo.getOpenid());
            
            //If there *is*... then UPDATE that record
            if(userid > 0) {
                System.out.println("I HAVE A USERID: "+userid);
                assert (userid == userInfo.getid()) : "The database id ("+userid+") for this openid ("+userInfo.getOpenid()+") does NOT match this object's ("+userInfo.getid()+")";
                numRowsAffected = queryRunner.update(updateUserQuery,
                                                     userInfo.getOpenid(),
                                                     userInfo.getFirstName(),
                                                     userInfo.getMiddleName(),
                                                     userInfo.getLastName(),
                                                     userInfo.getUserName(),
                                                     userInfo.getEmail(),
                                                     userInfo.getDn(),
                                                     userInfo.getOrganization(),
                                                     userInfo.getOrgType(),
                                                     userInfo.getCity(),
                                                     userInfo.getState(),
                                                     userInfo.getCountry(),
                                                     userid
                                                     );
                return (numRowsAffected > 0);
            }
            
            //If this user does not exist in the database then add (INSERT) a new one
            System.out.println("Whole new user: "+userInfo.getFirstName());
            userid = queryRunner.query(getNextUserPrimaryKeyValQuery ,idResultSetHandler);
            System.out.println("New ID to be assigned: "+userid);
            
            numRowsAffected = queryRunner.update(addUserQuery,
                                                 userid,
                                                 userInfo.getOpenid(),
                                                 userInfo.getFirstName(),
                                                 userInfo.getMiddleName(),
                                                 userInfo.getLastName(),
                                                 userInfo.getUserName(),
                                                 userInfo.getEmail(),
                                                 userInfo.getDn(),
                                                 userInfo.getOrganization(),
                                                 userInfo.getOrgType(),
                                                 userInfo.getCity(),
                                                 userInfo.getState(),
                                                 userInfo.getCountry()
                                                 );
            
            
            //A bit of debugging and sanity checking...
            System.out.println(userInfo);
            
        }catch(SQLException ex) {
            log.error(ex);
        }
        return (numRowsAffected > 0);
    }
    
    //Sets the password value for a given user (openid)
    synchronized boolean setPassword(String openid, String newPassword) {
        if((newPassword == null) || (newPassword.equals(""))) return false; //should throw and esgf exception here with meaningful message
        int numRowsAffected = -1;
        try{
            numRowsAffected = queryRunner.update(setPasswordQuery, md5Hex(newPassword), openid);
        }catch(SQLException ex) {
            log.error(ex);
        }
        return (numRowsAffected > 0);
    }
    
    //Given a password, check to see if that password matches what is
    //in the database for this user (openid)
    public boolean checkPassword(String openid, String queryPassword) {
        boolean isMatch = false;
        try{
            isMatch = (md5Hex(queryPassword)).equals(queryRunner.query(getPasswordQuery, passwordQueryHandler, openid));
        }catch(SQLException ex) {
            log.error(ex);
        }
        return isMatch;
    }
    
    //Given the old password and the new password for a given user
    //(openid) update the password, only if the old password matches
    public synchronized boolean changePassword(String openid, String queryPassword, String newPassword) {
        boolean isSuccessful = false;
        if(checkPassword(openid,queryPassword)){
            isSuccessful = setPassword(openid,newPassword);
        }
        return isSuccessful;
    }
    
    synchronized int addPermission(int userid, int groupid,int roleid) {
        int numRowsAffected = -1;
        try{
            numRowsAffected = queryRunner.update(addPermissionQuery,
                                                 userid, groupid, roleid);
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
