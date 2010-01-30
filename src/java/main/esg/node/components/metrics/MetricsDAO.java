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
package esg.node.components.metrics;

import java.util.List;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Calendar;
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


public class MetricsDAO implements Serializable {

    private static final String markTimeQuery      = "UPDATE metrics_run_log SET last_run_time = ? WHERE id = ?";
    private static final String regCheckEntryQuery = "SELECT COUNT(*) FROM metrics_run_log WHERE id = ?";
    private static final String regAddEntryQuery   = "INSERT INTO metrics_run_log (id, last_run_time) VALUES ( ? , ? )";
    
    private static final Log log = LogFactory.getLog(MetricsDAO.class);

    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private String nodeID = null;

    public MetricsDAO(DataSource dataSource,String nodeID) {
	this.setDataSource(dataSource);
	this.setNodeID(nodeID);
	init();
    }

    /**
       Not preferred constructor.  Uses default node id value...
     */
    public MetricsDAO(DataSource dataSource) {
	this(dataSource,Utils.getNodeID());
    }

    /**
       Not preferred constructor but here for serialization requirement.
    */
    public MetricsDAO() { 
	this(null,null); 
    }

    //Initialize result set handlers...
    public void init() {
	log.trace("Setting up result handlers");
	registerWithMetricsRunLog();
    }

    public void setDataSource(DataSource dataSource) {
	log.trace("Setting Up Metrics DAO's Pooled Data Source");
	this.dataSource = dataSource;
	this.queryRunner = new QueryRunner(dataSource);
    }
    
    private void setNodeID(String nodeID) { 
	log.trace("Metrics DAO's nodeID: "+nodeID);
	this.nodeID = nodeID; 
    }
    
    private String getNodeID() { 
	if(nodeID == null) throw new ESGInvalidObjectStateException("NodeID cannot be NULL!");
	return nodeID; 
    }
        

    //------------------------------------
    //Query function calls...
    //------------------------------------
    
    public int markLastCompletionTime(){
	int ret = -1;
	try{
	    long now = System.currentTimeMillis()/1000;
	    ret = queryRunner.update(markTimeQuery,now,getNodeID());
	}catch(SQLException ex) {
	    log.error(ex);
	}
	return ret;
    }

    private int registerWithMetricsRunLog() {
	int ret = -1;
	try{
	    log.trace("Registering this node ["+getNodeID()+"] into database");
	    int count = queryRunner.query(regCheckEntryQuery, new ResultSetHandler<Integer>() {
		    public Integer handle(ResultSet rs) throws SQLException {
			if(!rs.next()) { return -1; }
			return rs.getInt(1);
		    }
		},MetricsDAO.this.getNodeID());
	    
	    if(count > 0) {
		log.info("Yes, "+MetricsDAO.this.getNodeID()+" exists in metrics run log table");
	    }else {
		log.info("No, "+MetricsDAO.this.getNodeID()+" does NOT exist in metrics run log table");
		ret = queryRunner.update(regAddEntryQuery,MetricsDAO.this.getNodeID(),System.currentTimeMillis()/1000);
	    }
	    
	}catch(SQLException ex) {
	    log.error(ex);	    
	}
	return ret;
    }

    //------------------------------------

    public String toString() {
	StringBuilder out = new StringBuilder();
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+regCheckEntryQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+regAddEntryQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n"));
	out.append("DAO:(1)["+this.getClass().getName()+"] - [Q:"+markTimeQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]"));
	return out.toString();
    }
}
