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
package esg.node.components.monitoring;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Properties;
import java.util.regex.*;
import java.io.Serializable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;


import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.Utils;
import esg.common.ESGInvalidObjectStateException;


public class MonitorDAO implements Serializable {

    private static final String markTimeQuery      = "UPDATE monitor_run_log SET last_run_time = ? WHERE id = ?";
    private static final String regCheckEntryQuery = "SELECT COUNT(*) FROM monitor_run_log WHERE id = ?";
    private static final String regAddEntryQuery   = "INSERT INTO monitor_run_log (id, last_run_time) VALUES ( ? , ? )";
    
    private static final Log log = LogFactory.getLog(MonitorDAO.class);

    private Properties props = null;
    private DataSource dataSource = null;
    private QueryRunner queryRunner = null;
    private String nodeID = null;
    private Pattern disk_info_dsroot_pat = null;
    private Pattern disk_info_dsroot_keyval_pat = null;
    private ByteBuffer disk_info_byte_buffer = null;

    public MonitorDAO(DataSource dataSource,String nodeID,Properties props) {
	this.setDataSource(dataSource);
	this.setNodeID(nodeID);
	this.setProperties(props);
	init();
    }

    /**
       Not preferred constructor.  Uses default node id value...
     */
    public MonitorDAO(DataSource dataSource) {
	this(dataSource,Utils.getNodeID(),new Properties());
    }

    /**
       Not preferred constructor but here for serialization requirement.
    */
    public MonitorDAO() { this(null,null,new Properties()); }
    
    public void setProperties(Properties props) { this.props = props; }

    //Initialize result set handlers...
    public void init() {
	log.trace("Setting up result handlers");
	registerWithMonitorRunLog();
	loadDiskInfoResource();
    }

    public void setDataSource(DataSource dataSource) {
	log.trace("Setting Up Monitor DAO's Pooled Data Source");
	this.dataSource = dataSource;
	this.queryRunner = new QueryRunner(dataSource);
    }
    
    private void setNodeID(String nodeID) { 
	log.trace("Monitor DAO's nodeID: "+nodeID);
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

    private int registerWithMonitorRunLog() {
	int ret = -1;
	try{
	    log.trace("Registering this node ["+getNodeID()+"] into database");
	    int count = queryRunner.query(regCheckEntryQuery, new ResultSetHandler<Integer>() {
		    public Integer handle(ResultSet rs) throws SQLException {
			if(!rs.next()) { return -1; }
			return rs.getInt(1);
		    }
		},MonitorDAO.this.getNodeID());
	    
	    if(count > 0) {
		log.info("Yes, "+MonitorDAO.this.getNodeID()+" exists in monitor run log table");
	    }else {
		log.info("No, "+MonitorDAO.this.getNodeID()+" does NOT exist in monitor run log table");
		ret = queryRunner.update(regAddEntryQuery,MonitorDAO.this.getNodeID(),System.currentTimeMillis()/1000);
	    }
	    
	}catch(SQLException ex) {
	    log.error(ex);	    
	}
	return ret;
    }

    //------------------------------------

    //slightly less GC and memory friendly...
    public  MonitorInfo getMonitorInfo() { return this.setMonitorInfo(null); }

    //more GC and memory friendly if info is != null
    public  MonitorInfo setMonitorInfo(MonitorInfo info) {
	if(null == info) info = new MonitorInfo();
	log.trace("setting up monitor information");
	this.setDiskInfo(info);
	this.setMemInfo(info);
	this.setCPUInfo(info);
	this.setUptime(info);
	this.setXferInfo(info);
	infoAsString(info);
	return info;
    }

    //This method should be called once during initialization 
    //This sets up the resourcess used for the setDiskInfo call (below)
    private void loadDiskInfoResource() {
	disk_info_dsroot_pat  = Pattern.compile("thredds_dataset_roots\\s*=\\s*(.*?)\\s*\\w+\\s*?=");
	disk_info_dsroot_keyval_pat = Pattern.compile("\\s*(\\w+)\\s*\\|\\s*(\\S+)\\s*");

	String filename = props.getProperty("monitor.esg.ini","~/.esgcet/esg.ini");
	File iniFile = new File(filename);
	if(!iniFile.exists()) {
	    log.warn("ESG publisher config file ["+filename+"] not found! Cannot provide disk info!");
	    return;
	}

	log.debug("Scanning for drives specified in: "+filename);

	try{
	    FileInputStream fis = new FileInputStream(iniFile);
	    FileChannel fc = fis.getChannel();
	    disk_info_byte_buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, (int)fc.size());
	}catch(FileNotFoundException e) {
	    log.error(e);
	}catch(IOException e) {
	    log.error(e);
	}
    }
        
    private void setDiskInfo(MonitorInfo info) { 
	if(info.diskInfo == null) {
	    info.diskInfo = new HashMap<String,Map<String,String>>();
	}
	
	info.diskInfo.clear();

	if(disk_info_byte_buffer == null) {
	    log.warn("Disk Info Byte Buffer is : ["+disk_info_byte_buffer+"] cannot provide disk information!");
	    return;
	}

	//"thredds_dataset_roots"
	Map<String,File> datasetRoots =  new HashMap<String,File>();

	try{
	    Charset cs = Charset.forName("8859_1");
	    CharBuffer cb = cs.newDecoder().decode(disk_info_byte_buffer);
	    
	    //Flatten the file...
	    String data = cb.toString().replaceAll("[\n]+"," ");
	    
	    Matcher m = disk_info_dsroot_pat.matcher(data);
	    String key_vals = null;
	    Matcher m2 = null;
	    
	    while(m.find()) {
		key_vals = m.group(1);
		m2 = disk_info_dsroot_keyval_pat.matcher(key_vals);
		while (m2.find()) {
		    log.debug("Checking... dataset_root ["+m2.group(1)+"] dir ["+m2.group(2)+"]");
		    datasetRoots.put(m2.group(1),new File(m2.group(2)));
		}
	    }
	    
	    Map<String,String> statsMap = null;
	    for (String rootLabel : datasetRoots.keySet()) {
		Long total, free = 0L;
		statsMap = new HashMap<String,String>();
		log.trace("rootLabel: "+rootLabel);
		File f = datasetRoots.get(rootLabel);
		log.trace("value : "+f);
		statsMap.put(MonitorInfo.TOTAL_SPACE, ""+(total=(datasetRoots.get(rootLabel).getTotalSpace())/1024));
		statsMap.put(MonitorInfo.FREE_SPACE,  ""+(free=(datasetRoots.get(rootLabel).getFreeSpace())/1024));
		statsMap.put(MonitorInfo.USED_SPACE,  ""+(total-free));
		info.diskInfo.put(rootLabel,statsMap);
	    }
	}catch(java.nio.charset.CharacterCodingException e) {
	    log.error(e);
	}
    }
    
    private void setMemInfo(MonitorInfo info) { 
	if(info.memInfo == null) {
	    info.memInfo = new HashMap<String,String>();
	}
	
	//TODO read the /proc/meminfo file pull out values

	info.memInfo.put(MonitorInfo.TOTAL_MEMORY,"1");
	info.memInfo.put(MonitorInfo.FREE_MEMORY,"2");
	info.memInfo.put(MonitorInfo.TOTAL_SWAP,"3");
	info.memInfo.put(MonitorInfo.FREE_SWAP,"4");
	info.memInfo.put(MonitorInfo.USED_MEMORY,"5");
	
    }
    private void setCPUInfo(MonitorInfo info) { 
	if(info.cpuInfo == null) {
	    info.cpuInfo = new HashMap<String,String>();
	}

	//TODO read the /proc/cpuinfo file pull out values

	info.cpuInfo.put(MonitorInfo.CORES, ""+Runtime.getRuntime().availableProcessors());
	info.cpuInfo.put(MonitorInfo.CLOCK_SPEED,"2");
    }
    private void setUptime(MonitorInfo info) {
	if(info.uptimeInfo == null) {
	    info.uptimeInfo = new HashMap<String,String>();
	}

	//TODO runthe /proc/meminfo command and pull out values

	info.uptimeInfo.put(MonitorInfo.UPTIME,"1");
	info.uptimeInfo.put(MonitorInfo.USERS,"2");
	info.uptimeInfo.put(MonitorInfo.LOAD_AVG1,"3");
	info.uptimeInfo.put(MonitorInfo.LOAD_AVG2,"4");
	info.uptimeInfo.put(MonitorInfo.LOAD_AVG3,"5");
    }
    private void setXferInfo(MonitorInfo info) { 
	if(info.xferInfo == null) {
	    info.xferInfo = new HashMap<String,String>();
	}
	info.xferInfo.put(MonitorInfo.XFER_AVG,"-1");	
    }

    private void infoAsString(MonitorInfo info) {
	if(info == null) {
	    log.warn("MonitorInfo object is null: ["+info+"]");
	    return;
	}
	
	StringBuilder out = new StringBuilder();
	out.append("MonitorInfo Object:\n");
	out.append(" diskInfo: "+info.diskInfo+"\n");
	out.append(" memInfo: "+info.memInfo+"\n");
	out.append(" cpuInfo: "+info.cpuInfo+"\n");
	out.append(" uptime: "+info.uptimeInfo+"\n");
	out.append(" xfer: "+info.xferInfo+"\n");
	out.append(" components: "+info.componentList+"\n");
	System.out.println(out.toString());
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
