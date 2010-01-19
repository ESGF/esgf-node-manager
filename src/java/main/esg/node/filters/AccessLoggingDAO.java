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
   Perform sql query to log visitors' access to data files
   
**/
package esg.node.filters;

import java.io.Serializable;
import java.util.Properties;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

public class AccessLoggingDAO implements Serializable {

    //TODO figure out what these queries should be!
    private static final String accessLoggingQuery = 
	"insert into access_logging (id, userid, email, url, remote_addr, file_id, date_fetched, success) "+
	"values ( nextval('seq_access_logging'), ?, ?, ?, ?, ?, ?, ?)";

    private static final Log log = LogFactory.getLog(AccessLoggingDAO.class);

    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;

    public AccessLoggingDAO(DataSource dataSource) {
	this.setDataSource(dataSource);
    }
    
    //Not preferred constructor but here for serialization requirement.
    public AccessLoggingDAO() { this(null); }

    //Initialize result set handlers...
    public void init() { }
    
    public void setDataSource(DataSource dataSource) {
	this.dataSource = dataSource;
	this.queryRunner = new QueryRunner(dataSource);
    }
    
    //TODO: put in args and setup query!!!
    public int log(String userid,  String email, String url, String remoteAddress, String fileID) {
	int ret = -1;
	try{
	    //TODO: Perhaps the url can be used to resolve the dataset???
	    //That is the bit of information we really want to also have.
	    //What we really need is an absolute id for a file!!!
	    ret = queryRunner.update(accessLoggingQuery,
				     userid,email,url,remoteAddress,fileID,System.currentTimeMillis()/1000,true);
	}catch(SQLException ex) {
	    log.error(ex);
	}
	return ret;
    }
    
    public String toString() {
	return "DAO:(1)["+this.getClass().getName()+"] - [Q:"+accessLoggingQuery+"] "+((dataSource == null) ? "[OK]" : "[INVALID]\n");
    }

}