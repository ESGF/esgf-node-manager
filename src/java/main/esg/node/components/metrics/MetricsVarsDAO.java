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
   Description: Perform sql query to collect information about
   variables.  The idea is to provide the raw data for building a
   histogram of variable "usage" as it pertains to downloads.
   
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

public class MetricsVarsDAO extends ESGDAO {
    
    private static final Log log = LogFactory.getLog(MetricsVarsDAO.class);
    private Properties props = null;
    private StringBuilder str = null;

    public MetricsVarsDAO(DataSource dataSource, String nodeID, Properties props) {
	super(dataSource,nodeID);
	this.setProperties(props);
    }
    public MetricsVarsDAO(DataSource dataSource) { 
	this(dataSource,null, new Properties()); 
    }
    public MetricsVarsDAO() { super(); }
    
    public void init() { buildResultSetHandler(); }

    public void setProperties(Properties props) { this.props = props; }
    //------------------------------------
    //Query...
    //------------------------------------
    private static final String query = "select d.project, d.model, count(*), sum(lver.size) from ("+MetricsDAO.SUBQ+") as lver, file as f, dataset as d where lver.file_id=f.id and f.dataset_id=d.id group by project, model";

    private static final String downloadQuery = "select d.project, d.model, count(*), sum(size) from ("+MetricsDAO.SUBQ+") as lver, access_logging as dl, file as f, dataset as d where dl.url=lver.url and lver.file_id=f.id and f.dataset_id=d.id group by project, model";
    
    public List<MetricsVarsDAO.VarInfo> getMetricsInfo() { return performQuery(query); }
    public List<MetricsVarsDAO.VarInfo> getDownloadMetricsInfo() { return performQuery(downloadQuery); }

    private List<MetricsVarsDAO.VarInfo> performQuery(String query) {

	if(this.dataSource == null) {
	    log.error("The datasource ["+dataSource+"] is not valid, Please call setDataSource(...) first!!!");
	    return null;
	}
	log.trace("Getting Metrics: Variable Information... \n Query = "+query);
	
	List<VarInfo> varInfos = null;
	try{
	    varInfos = getQueryRunner().query(query, metricsVarsHandler);
	}catch(SQLException ex) {
	    log.error(ex);
	}
	
	return varInfos;
    }
    
    //------------------------------------
    //Result Handling...
    //------------------------------------
    private ResultSetHandler<List <MetricsVarsDAO.VarInfo>> metricsVarsHandler = null;
    protected void buildResultSetHandler() {
	metricsVarsHandler = new ResultSetHandler<List<MetricsVarsDAO.VarInfo>> () {
	    public List<MetricsVarsDAO.VarInfo> handle(ResultSet rs) throws SQLException {
		List<MetricsVarsDAO.VarInfo> varInfos = new Vector<VarInfo>();
		VarInfo varInfo = new VarInfo();
		if(!rs.next()) return varInfos;
		do{
		    varInfo.project = rs.getString(1);
		    varInfo.model = rs.getString(2);
		    varInfo.count = rs.getInt(3);
		    varInfo.sum = rs.getLong(4);
		    varInfo = new VarInfo();
		}while(rs.next());
		return varInfos;
	    }
	};
    }
    
    //------------------------------------
    //Result Encapsulation...
    //------------------------------------
    public class VarInfo {
	public String project = null;
	public String model = null;
	public int count = -1;
	public long sum = -1L;

	public String toString() { return "project: ["+project+"] model: ["+model+"] count: ["+count+"] sum: ["+sum+"]"; }
    }

    public String toString() {
	if(str == null) str = new StringBuilder();
	if(str.length() != 0) return str.toString();;
	str.append(this.getClass().getName()+":(1)["+this.getClass().getName()+"] - [Q:"+query+"] "+((dataSource == null) ? "[OK]\n" : "[INVALID]\n"));	
	str.append(this.getClass().getName()+":(1)["+this.getClass().getName()+"] - [Q:"+downloadQuery+"] "+((dataSource == null) ? "[OK]\n" : "[INVALID]\n"));	
	return str.toString();
    }

}
