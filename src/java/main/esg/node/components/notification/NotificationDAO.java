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
*   For details, see http://esg-repo.llnl.gov/esg-node/                    *
*   Please also read this link                                             *
*    http://esg-repo.llnl.gov/LICENSE                                      *
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
package esg.node.components.notification;

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

public class NotificationDAO implements Serializable {

    //TODO figure out what these queries should be!
    private static final String notificationQuery = "SELECT DISTINCT d.userid, d.email, ds.name, d.url, fv.location FROM dataset as ds, file as f, access_logging as d, file_version as fv WHERE ds.id=f.dataset_id and fv.file_id=f.id and d.url=fv.url and fv.mod_time>d.date_fetched AND d.date_fetched > (SELECT distinct MAX(notify_time) FROM notification_run_log where id = ? )";
    private static final String markTimeQuery      = "UPDATE notification_run_log SET notify_time = ? WHERE id = ?";
    private static final String regCheckEntryQuery = "SELECT COUNT(*) FROM notification_run_log WHERE id = ?";
    private static final String regAddEntryQuery   = "INSERT INTO notification_run_log (id, notify_time) VALUES ( ? , ? )";

    private static final Log log = LogFactory.getLog(NotificationDAO.class);

    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private ResultSetHandler<List<NotificationDAO.NotificationRecipientInfo> > handler = null;
    private String nodeID = null;

    public NotificationDAO(DataSource dataSource) {
	this.setDataSource(dataSource);
	init();
    }
    
    /**
       Not preferred constructor but here for serialization requirement.
    */
    public NotificationDAO() { this(null); }

    //Initialize result set handlers...
    public void init() {
	handler = new ResultSetHandler<List<NotificationDAO.NotificationRecipientInfo> > () {
	    public List<NotificationDAO.NotificationRecipientInfo> handle(ResultSet rs) throws SQLException {
		List<NotificationDAO.NotificationRecipientInfo> nris = new Vector<NotificationDAO.NotificationRecipientInfo>();
		NotificationRecipientInfo nri = new NotificationRecipientInfo();
		if(!rs.next()) { return null; }
		
		//TODO...
		//Wrestle results into nri objects...
		
		return nris;
	    }
	};
	
	//inserts entry into table for this node
	registerWithNotificationRunLog();
	
    }

    public void setDataSource(DataSource dataSource) {
	this.dataSource = dataSource;
	this.queryRunner = new QueryRunner(dataSource);
    }
    
    public void setNodeID(String nodeID) { this.nodeID = nodeID; }
    private String getNodeID() {
	if(null != nodeID) { return nodeID; }
	try{
	    nodeID = java.net.InetAddress.getLocalHost().getHostAddress();
	}catch(java.net.UnknownHostException ex) {
	    log.error(ex);
	}
	return nodeID;
    }
    
    //TODO: May have to take a list of update datasets here...
    public List<NotificationRecipientInfo> getNotificationRecipientInfo() {
	if(this.dataSource == null) {
	    log.error("The datasource ["+dataSource+"] is not valid, Please call setDataSource(...) first!!!");
	    return null;
	}
	log.trace("Getting Notification Recipient Info... \n Query = "+notificationQuery);
	List<NotificationRecipientInfo> nris = null;
	try{
	    nris = queryRunner.query(notificationQuery, handler,getNodeID());
	}catch(SQLException ex) {
	    log.error(ex);
	}
	
	//TODO: fake data...
	//nri = new NotificationRecipientInfo();
	//nri.dataset_id="faux_dataset_id_v1";
	//nri.endusers = new String[] {"gavin@llnl.gov","williams13@llnl.gov","drach1@llnl.gov"};
	//nri.changedFiles = new String[] {"faux_file1","faux_file2","faux_file3","faux_file4"};
	
	return nris;
    }
    
    public int markLastCompletionTime(){
	int ret = -1;
	try{
	    ret = queryRunner.update(markTimeQuery,System.currentTimeMillis()/1000,getNodeID());
	}catch(SQLException ex) {
	    log.error(ex);
	}
	return ret;
    }

    private int registerWithNotificationRunLog() {
	int ret = -1;
	try{
	    int count = queryRunner.query(regCheckEntryQuery, new ResultSetHandler<Integer>() {
		    public Integer handle(ResultSet rs) throws SQLException {
			if(!rs.next()) { return -1; }
			return rs.getInt(1);
		    }
		},NotificationDAO.this.getNodeID());
	    
	    if(count > 0) {
		log.info("Yes, "+NotificationDAO.this.getNodeID()+" exists in notification run log table");
	    }else {
		log.info("No, "+NotificationDAO.this.getNodeID()+" does NOT exist in notification run log table");
		ret = queryRunner.update(regAddEntryQuery,NotificationDAO.this.getNodeID(),System.currentTimeMillis()/1000);
	    }

	}catch(SQLException ex) {
	    log.error(ex);	    
	}
	return ret;
    }

    //Result data holder object....
    public static class NotificationRecipientInfo {
	String dataset_id = null;
	String[] endusers = null;
	String[] changedFiles = null;
    }

    public String toString() {
	StringBuilder out = new StringBuilder();
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+notificationQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+regCheckEntryQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+regAddEntryQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+markTimeQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]"));
	return out.toString();
    }
}