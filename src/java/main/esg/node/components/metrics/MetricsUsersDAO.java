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
   Description: Perform sql query to find out all the people who have
   downloaded data and how many times.
   
**/
package esg.node.components.metrics;

import java.util.Properties;
import java.util.List;
import java.util.Vector;
import java.io.Serializable;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.core.*;

public class MetricsUsersDAO extends ESGDAO {
    
    private static final Log log = LogFactory.getLog(MetricsUsersDAO.class);
    private Properties props = null;

    public MetricsUsersDAO(DataSource dataSource, String nodeID, Properties props) {
	super(dataSource,nodeID);
	this.setProperties(props);
    }
    public MetricsUsersDAO(DataSource dataSource) { this(dataSource,null,new Properties()); }
    public MetricsUsersDAO() { super(); }
    
    public void init() { buildResultSetHandler(); }

    public void setProperties(Properties props) { this.props = props; }

    //------------------------------------
    //Query...
    //------------------------------------
    private static final String query = "select dl.user_id, count(*), sum(size) from ("+MetricsDAO.SUBQ+") as lver, esgf.access_logging as dl where lver.url=dl.url group by user_id";

    public List<MetricsUsersDAO.UserInfo> getDownloadMetricsInfo() {
	if(this.dataSource == null) {
	    log.error("The datasource ["+dataSource+"] is not valid, Please call setDataSource(...) first!!!");
	    return null;
	}
	log.trace("Getting Metrics: User Infos... \n Query = "+query);
	
	List<UserInfo> userInfos = null;
	try{
	    userInfos = getQueryRunner().query(query, metricsUsersHandler, getNodeID());
	}catch(SQLException ex) {
	    log.error(ex);
	}
	
	return userInfos;
    }

    //------------------------------------
    //Result Handling...
    //------------------------------------
    private ResultSetHandler<List <MetricsUsersDAO.UserInfo>> metricsUsersHandler = null;
    protected void buildResultSetHandler() {
	log.trace("Setting up result handler");
	metricsUsersHandler = new ResultSetHandler<List<MetricsUsersDAO.UserInfo>> () {
	    public List<MetricsUsersDAO.UserInfo> handle(ResultSet rs) throws SQLException {
		List<MetricsUsersDAO.UserInfo> userInfos = new Vector<UserInfo>();
		UserInfo userInfo = new UserInfo();
		if(!rs.next()) return userInfos;
		do{
		    userInfo.userid = rs.getString(1);
		    userInfo.count = rs.getInt(2);
		    userInfo.sum = rs.getLong(3);
		    userInfo = new UserInfo();
		}while(rs.next());
		return userInfos;
	    }
	};
    }
        
    //------------------------------------
    //Result Encapsulation...
    //------------------------------------
    public class UserInfo {
	public String userid = null;
	public int count = -1;
	public long sum = -1L;

	public String toString() { return "userid: ["+userid+"] count: ["+count+"] sum: ["+sum+"]"; }
    }

    public String toString() {
	return this.getClass().getName()+":(1)["+this.getClass().getName()+"] - [Q:"+query+"] "+((dataSource == null) ? "[OK]\n" : "[INVALID]\n");	
    }

}
