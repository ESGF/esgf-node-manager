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
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.Utils;
import esg.common.ESGInvalidObjectStateException;


public class NotificationDAO implements Serializable {

    private static final String notificationQuery = "SELECT DISTINCT d.userid, d.email, ds.name, d.url, fv.mod_time FROM dataset as ds, file as f, access_logging as d, file_version as fv WHERE ds.id=f.dataset_id and fv.file_id=f.id and d.url=fv.url and fv.mod_time>d.date_fetched AND d.date_fetched > (SELECT distinct MAX(notify_time) FROM notification_run_log where id = ? ) ORDER BY d.email";
    private static final String markTimeQuery      = "UPDATE notification_run_log SET notify_time = ? WHERE id = ?";
    private static final String regCheckEntryQuery = "SELECT COUNT(*) FROM notification_run_log WHERE id = ?";
    private static final String regAddEntryQuery   = "INSERT INTO notification_run_log (id, notify_time) VALUES ( ? , ? )";

    private static final Log log = LogFactory.getLog(NotificationDAO.class);

    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private ResultSetHandler<List<NotificationDAO.NotificationRecipientInfo> > handler = null;
    private String nodeID = null;

    public NotificationDAO(DataSource dataSource,String nodeID) {
	this.setDataSource(dataSource);
	this.setNodeID(nodeID);
	init();
    }

    /**
       Not preferred constructor.  Uses default node id value...
     */
    public NotificationDAO(DataSource dataSource) {
	this(dataSource,Utils.getNodeID());
    }

    /**
       Not preferred constructor but here for serialization requirement.
    */
    public NotificationDAO() { 
	this(null,null); 
    }

    //Initialize result set handlers...
    public void init() {
	log.trace("Setting up result handler");
	handler = new ResultSetHandler<List<NotificationDAO.NotificationRecipientInfo> > () {
	    public List<NotificationDAO.NotificationRecipientInfo> handle(ResultSet rs) throws SQLException {
		List<NotificationDAO.NotificationRecipientInfo> nris = new Vector<NotificationDAO.NotificationRecipientInfo>();
		NotificationDAO.NotificationRecipientInfo nri = new NotificationDAO.NotificationRecipientInfo(); 
		String lastUserid = null;
		String lastUserAddress = null;
		if(!rs.next()) { return null; }
		do{

		    String userid = rs.getString(1);
		    String userAddress = rs.getString(2);
		    
		    //Create a new object PER NEW EMAIL ADDRESS...
		    if( (lastUserAddress == null) || (!lastUserAddress.equals(userAddress)) ) {
			//NOTE - Memory Optimization: See comment section of ESGNotifier...
			//(memory optimization place to put callback to notifier.generateNotification(lastUserid,lastUserAddress,nri);)
			//(and then remove the call to nris.add(nri))
			//(also would have to make this object take a [final] 'notifier' obj, prob via the call getNotificationInfo)
			nri = new NotificationDAO.NotificationRecipientInfo();
			nri.withValues(userid,userAddress);
		    }

		    //NOTE: As a refinement take a look at using a
		    //"BeanListHandler" I think it would provide
		    //flexibility with ordering of columns, but has
		    //the additional stipulation of having to have to
		    //name attributes the same as column headings or
		    //alias the result column names from the query.
		    //At the moment I am going with what I
		    //know. (K.I.S.S.)
		    
		    //For more info on bean handling in dbUtil see: http://commons.apache.org/dbutils/examples.html
		    //For reference on jdbc/sql type mapping: //http://java.sun.com/j2se/1.3/docs/guide/jdbc/getstart/mapping.html

		    nri.withDatasetInfo(rs.getString(3),rs.getString(4),rs.getDouble(5));
		    nris.add(nri);

		    lastUserid = userid;
		    lastUserAddress = userAddress;

		}while(rs.next());
		return nris;
	    }
	};
	
	//inserts entry into table for this node
	registerWithNotificationRunLog();
	
    }

    public void setDataSource(DataSource dataSource) {
	log.trace("Setting Up Notification DAO's Pooled Data Source");
	this.dataSource = dataSource;
	this.queryRunner = new QueryRunner(dataSource);
    }
    
    //NOTE: I made this private because I can't think of any
    //circumstance where an instance of a DAO would need to change
    //it's nodeID.  The nodeID is a intrinsic identifying
    //characteristic of a DAO that qualifies many of the queries it
    //performs.  It is especially critical when nodes may use existing
    //postgres installations that is shared by other nodes. (just
    //thinking ahead into the state preserving cloud-ish model of
    //operation.
    private void setNodeID(String nodeID) { 
	log.trace("Notification DAO's nodeID: "+nodeID);
	this.nodeID = nodeID; 
    }
    private String getNodeID() { 
	if(nodeID == null) throw new ESGInvalidObjectStateException("NodeID cannot be NULL!");
	return nodeID; 
    }
    
    public List<NotificationRecipientInfo> getNotificationRecipientInfo() {
	if(this.dataSource == null) {
	    log.error("The datasource ["+dataSource+"] is not valid, Please call setDataSource(...) first!!!");
	    return null;
	}
	//log.trace("Getting Notification Recipient Info... \n Query = "+notificationQuery);
	log.trace("Getting Notification Recipient Info...");
	List<NotificationRecipientInfo> nris = null;
	try{
	    nris = queryRunner.query(notificationQuery, handler,getNodeID());
	}catch(SQLException ex) {
	    log.error(ex);
	}
	
	return nris;
    }
    
    public int markLastCompletionTime(){
	int ret = -1;
	try{
	    long now = System.currentTimeMillis()/1000;
	    log.trace("marking completion time: "+now);
	    ret = queryRunner.update(markTimeQuery,now,getNodeID());
	}catch(SQLException ex) {
	    log.error(ex);
	}
	return ret;
    }

    private int registerWithNotificationRunLog() {
	int ret = -1;
	try{
	    log.trace("Registering this node ["+getNodeID()+"] into database");
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

    //----------------------------------------------
    //Result data holder objects....
    //----------------------------------------------
    public static class NotificationRecipientInfo {
	String userid = null;
	String userAddress = null;
	Map<String,Set<FileInfo>>  datasets = new HashMap<String,Set<FileInfo>>();

	//Using a more "fluent" model of setting vars: convention is to use "withXXX"
	public NotificationRecipientInfo withValues(String userid, String userAddress) {
	    this.userid = userid;
	    this.userAddress = userAddress;
	    return this;
	}
	
	public NotificationRecipientInfo withDatasetInfo(String datasetName,String fileUrl,double modTime) {
	    Set<FileInfo> fileInfoSet = datasets.get(datasetName);
	    if(fileInfoSet == null) {
		datasets.put(datasetName, fileInfoSet = new HashSet<FileInfo>());
	    }
	    fileInfoSet.add(new FileInfo(fileUrl,modTime));
	    return this;
	}

	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    String datasetName = null;
	    for(Iterator<String> dsIt = datasets.keySet().iterator(); dsIt.hasNext();) {
		datasetName = dsIt.next();
		sb.append("Dataset: "+datasetName+"\n");
		for(Iterator<FileInfo> fiIt = datasets.get(datasetName).iterator(); fiIt.hasNext();) { 
		    sb.append("   File URL: "+fiIt.next());
		}
	    }
	    return sb.toString();
	}
    }

    public static class FileInfo {
	String url = null;
	double modTime = 0L;
	
	FileInfo(String url, double modTime) {
	    this.url = url;
	    this.modTime = modTime;
	}

	//FileInfos are equal if the urls are the same
	//(Not counting mod time)
	public boolean equals(Object obj) {
	    return this.url.equals(((FileInfo)obj).url);
	}

	public String toString() {
	    return url+" : "+modTime; //TODO: turn modTime into a human readable date
	}
	
    }
    //----------------------------------------------


    public String toString() {
	StringBuilder out = new StringBuilder();
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+notificationQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+regCheckEntryQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+regAddEntryQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+markTimeQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]"));
	return out.toString();
    }
}