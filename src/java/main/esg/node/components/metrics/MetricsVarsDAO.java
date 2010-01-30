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

    public MetricsVarsDAO(DataSource dataSource, String nodeID) {
	super(dataSource,nodeID);
    }
    public MetricsVarsDAO(DataSource dataSource) { super(dataSource); }
    public MetricsVarsDAO() { super(); }
    
    public void init() {
	buildResultSetHandler();
    }
    

    //------------------------------------
    //Query...
    //------------------------------------
    //TODO What's the query?
    private static final String query = "";
    
    public List<MetricsVarsDAO.VarInfo> getMetricsInfo() {
	if(this.dataSource == null) {
	    log.error("The datasource ["+dataSource+"] is not valid, Please call setDataSource(...) first!!!");
	    return null;
	}
	log.trace("Getting Metrics: Var Infos... \n Query = "+query);
	
	List<VarInfo> varInfos = null;
	try{
	    varInfos = getQueryRunner().query(query, metricsVarsHandler, getNodeID());
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
		    //TODO pull out results...
		}while(rs.next());
		return varInfos;
	    }
	};
    }
    
    //------------------------------------
    //Result Encapsulation...
    //------------------------------------
    public class VarInfo {
	//TODO what the results look like...	
    }

    public String toString() {
	return this.getClass().getName()+":(1)["+this.getClass().getName()+"] - [Q:"+query+"] "+((dataSource == null) ? "[OK]\n" : "[INVALID]\n");	
    }

}
