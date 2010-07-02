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
package esg.gateway.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.net.MalformedURLException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import esg.common.db.DatabaseResource;

/**
   Description:

   The implementation class providing the substance behind the
   interface being exposed to the world via RPC (hessian)
*/
public class ESGAccessLogServiceImpl implements ESGAccessLogService {
    private static final Log log = LogFactory.getLog(ESGAccessLogService.class);
    private QueryRunner queryRunner = null;
    private ResultSetHandler<List<String[]>> resultSetHandler = null;
    private static final String accessLogQuery = "SELECT * FROM access_logging WHERE date_fetched >= ? AND date_fetched < ? ORDER BY date_fetched ASC";
        
    public ESGAccessLogServiceImpl() {
	log.trace("Instantiating ESGAccessLogService implementation");
	init();
    }

    public boolean ping() {
	log.trace("Access Log Service got \"ping\"");
	return true;
    }

    /**
       Initializes the service by setting up the database connection and result handling.
     */
    public void init() {
	Properties props = new Properties();
	props.setProperty("db.protocol","jdbc:postgresql:");
	props.setProperty("db.host","localhost");
	props.setProperty("db.port","5432");
	props.setProperty("db.database","esgcet");
	props.setProperty("db.user","dbsuper");
	props.setProperty("db.password","changeme");
	queryRunner = new QueryRunner(DatabaseResource.init("org.postgresql.Driver").setupDataSource(props).getDataSource());
	
	resultSetHandler = new ResultSetHandler<List<String[]>>() {
	    public List<String[]> handle(ResultSet rs) throws SQLException {
		ArrayList<String[]> results = new ArrayList<String[]>();
		String[] record = null;
		assert (null!=results);

		ResultSetMetaData meta = rs.getMetaData();
		int cols = meta.getColumnCount();
		log.trace("Number of fields: "+cols);

		log.trace("adding column data...");
		record = new String[cols];
		for(int i=0;i<cols;i++) {
		    try{
			record[i]=meta.getColumnLabel(i+1)+"|"+meta.getColumnType(i+1);
		    }catch (SQLException e) {
			log.error(e);
		    }
		}
		results.add(record);

		
		for(int i=0;rs.next();i++) {
		    log.trace("Looking at record "+(i+1));
		    record = new String[cols];
		    for (int j = 0; j < cols; j++) {
			record[j] = rs.getString(j + 1);
			log.trace("gathering result record column "+(j+1)+" -> "+record[j]);
		    }
		    log.trace("adding result record "+i);
		    results.add(record);
		    record = null; //gc courtesy
		}
		return results;
	    }
	};
	log.trace("initialization complete");
    }
    
    /**
       Remote method implementation that returns records from the
       "access_logging" database table This method always returns
       something, even if that is just an empty result.  

       @param startTime the earliest date record to return (inclusive)
       @param endTime latest date record to return (exclusive)
     */
    public List<String[]> fetchAccessLogData(long startTime, long endTime) {
	try{
	    log.trace("Fetching raw access_logging data from active database table");
	    List<String[]> results = queryRunner.query(accessLogQuery, resultSetHandler,startTime,endTime);
	    log.trace("Query is: "+accessLogQuery);
	    log.trace("Results = "+results);
	    if(results != null) { log.info("Retrieved "+(results.size()-1)+" records"); }
	    return results;
	}catch(SQLException ex) {
	    log.error(ex);	    
	}catch(Throwable t) {
	    log.error(t);
	}
	return new ArrayList<String[]>();
    }

}