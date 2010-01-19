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
   Scratch code for playing with queries and exorcizing jdbc 
**/

package esg.node.components.notification;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.core.Resource;
import esg.common.db.DatabaseResource;

public class SQLTest {

    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private ResultSetHandler<Object[]> handler = null;

    private static final Log log = LogFactory.getLog(SQLTest.class);

    public SQLTest() { init(); }

    private void init() {

	handler = new ResultSetHandler<Object[]>() {
	    public Object[] handle(ResultSet rs) throws SQLException {
		int recordCount = 0;
		System.out.println("\n\n");
		if(!rs.next()) { return null; }
		
		ResultSetMetaData meta = rs.getMetaData();
		int cols = meta.getColumnCount();
		Object[] result = new Object[cols];
		
		do{
		    System.out.print("#"+(++recordCount)+" -> ");
		    for (int i = 0; i < cols; i++) {
			result[i] = rs.getObject(i + 1);
			System.out.print("["+result[i]+(i == cols ? "" : "\t")+"]");
		    }
		    System.out.println();
		}while(rs.next());
		
		return result;
	    }
	};

	dataSource = DatabaseResource.init("org.postgresql.Driver").setupDataSource(loadProperties()).getDataSource();
	this.queryRunner = new QueryRunner(dataSource);
    }

    private Properties loadProperties() {
	log.trace("Loading Properties");
	Properties props = null;
	InputStream in = null;
	try {
	    Resource config = new Resource("node.properties");
	    in = config.getInputStream();
	    props = new Properties();
	    props.load(in);
	    log.trace("Properties of node.properties file: "+props);
	}catch(IOException ex) {
	    log.error("Problem loading datanode's property file!", ex);
	}catch(NullPointerException ex) {
	    log.error("PROPERTY FILE INPUT STREAM IS NULL!!",ex);
	    throw ex;
	}finally {
	    try { if(in != null) in.close(); } catch (IOException ex) { log.error(ex); }
	}
	log.trace("Loaded "+(props == null ? -1 : props.size())+" Properties");
	return props;
    }

    public Object[] test() {
	Object[] ret = null;
	try{
	    
	    //Works...
	    String query= "select * from notification_run_log";
	    ret = queryRunner.query(query,handler);

	}catch(SQLException ex) {
	    log.error(ex);
	}
	return ret;
    }

    public static void main(String[] args) {
	System.out.println((new SQLTest()).test().length+" returned records");
    }
}