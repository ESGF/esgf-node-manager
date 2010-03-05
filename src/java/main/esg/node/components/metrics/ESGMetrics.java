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

   This class is a component implementation that is responsible for
   collecting and disseminating host and system wide information.

**/
package esg.node.components.metrics;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.db.DatabaseResource;
import esg.common.Utils;
import esg.node.core.*;

public class ESGMetrics extends AbstractDataNodeComponent {
    
    private static Log log = LogFactory.getLog(ESGMetrics.class);
    private Properties props = null;
    private boolean isBusy = false;
    private MetricsDAO metricsDAO = null;
    private MetricsExpDAO metricsExpDAO = null;
    private MetricsVarsDAO metricsVarsDAO = null;
    private MetricsUsersDAO metricsUsersDAO = null;

    //Local cache objects for results
    private List<MetricsExpDAO.ExpInfo>    expInfos    = null;
    private List<MetricsVarsDAO.VarInfo>   varInfos    = null;
    private List<MetricsExpDAO.ExpInfo>    expInfosDL  = null; 
    private List<MetricsVarsDAO.VarInfo>   varInfosDL  = null;
    private List<MetricsUsersDAO.UserInfo> userInfosDL = null;

    public ESGMetrics(String name) {
	super(name);
	log.info("Instantiating ESGMetrics...");
    }
    
    public void init() {
	log.info("Initializing ESGMetrics...");
	props = getDataNodeManager().getMatchingProperties("^metrics.*");
	metricsDAO = new MetricsDAO(DatabaseResource.getInstance().getDataSource(),Utils.getNodeID(),props);
	metricsExpDAO = new MetricsExpDAO(DatabaseResource.getInstance().getDataSource(),Utils.getNodeID(),props);
	metricsVarsDAO = new MetricsVarsDAO(DatabaseResource.getInstance().getDataSource(),Utils.getNodeID(),props);
	metricsUsersDAO = new MetricsUsersDAO(DatabaseResource.getInstance().getDataSource(),Utils.getNodeID(),props);
	startMetricsCollection();
    }

    //Note: The idea for the "open to the user" method calls is that
    //they will lazily issue the initial query to be executed and then
    //subsequently calls will get the cached values till the next time cycle.

    //Methods for callers to use to get basic statistics...
    public List<MetricsExpDAO.ExpInfo>   getExperimentStats() { 
	return (expInfos == null) ? expInfos = metricsExpDAO.getMetricsInfo() : expInfos;
    }
    public List<MetricsVarsDAO.VarInfo> getVariableStats() { 
	return (varInfos == null) ? varInfos = metricsVarsDAO.getMetricsInfo() : varInfos;	
    }

    //Methods for callers to use to get download statistics...
    public List<MetricsExpDAO.ExpInfo>   getDownloadExperimentStats() { 
	return (expInfosDL == null) ? expInfosDL = metricsExpDAO.getDownloadMetricsInfo() : expInfosDL;
    }
    public List<MetricsVarsDAO.VarInfo> getDownloadVariableStats() { 
	return (varInfosDL == null) ? varInfosDL = metricsVarsDAO.getDownloadMetricsInfo() : varInfosDL;
    }
    public List<MetricsUsersDAO.UserInfo> getDownloadUserStats() {
	return (userInfosDL == null) ? userInfosDL = metricsUsersDAO.getDownloadMetricsInfo() : userInfosDL;
    }

    //Note: This is where the cache is replenished based on the cycle
    //of the timer not based on calling environment calls.  Except in
    //the initial case.  We only want to replenish the cache of
    //objects that have had interest shown in them... i.e. objects
    //that have already been requested at least once.

    //replenish cache objects...
    private synchronized boolean fetchNodeStats() {
	log.trace("metrics' fetchNodeStats() called....");
	boolean ret = true;

	//General statistics...
	if(expInfos  != null) expInfos  = metricsExpDAO.getMetricsInfo();
	if(varInfos  != null) varInfos  = metricsVarsDAO.getMetricsInfo();

	//Download statistics...
	if(expInfosDL  != null)	expInfosDL  = metricsExpDAO.getDownloadMetricsInfo();
	if(varInfosDL  != null)	varInfosDL  = metricsVarsDAO.getDownloadMetricsInfo();
	if(userInfosDL != null)	userInfosDL = metricsUsersDAO.getDownloadMetricsInfo();

	return ret;
    }
    
    private void startMetricsCollection() {
	log.trace("Launching Node Metrics Timer");
	long delay  = Long.parseLong(props.getProperty("metrics.initialDelay"));
	long period = Long.parseLong(props.getProperty("metrics.period"));
	log.trace("metrics delay: "+delay+" sec");
	log.trace("metrics period: "+period+" sec");
	
	Timer timer = new Timer();
	timer.schedule(new TimerTask() {
		public final void run() {
		    //log.trace("Checking for new datanode information... [busy? "+ESGMetrics.this.isBusy+"]");
		    if(!ESGMetrics.this.isBusy) {
			ESGMetrics.this.isBusy = true;
			if(fetchNodeStats()) {
			    metricsDAO.markLastCompletionTime();
			}
			ESGMetrics.this.isBusy = false;
		    }
		}
	    },delay*1000,period*1000);
    }

    //TODO:
    //Put in code that will eventually timeout the cache objects and free the memory up via GC.

    public boolean handleESGQueuedEvent(ESGEvent event) {
	log.trace("handling enqueued event ["+getName()+"]:["+this.getClass().getName()+"]: Got A QueuedEvent!!!!: "+event);
	//TODO
	//Fill this event with data from calling:
	StringBuilder sb = new StringBuilder();
	sb.append(getExperimentStats().toString()+"\n");
	sb.append(getVariableStats().toString()+"\n");
	sb.append(getDownloadExperimentStats().toString()+"\n");
	sb.append(getDownloadVariableStats().toString()+"\n");
	sb.append(getDownloadUserStats().toString()+"\n");
	
	event.setData(sb.toString());
	
	log.trace("Enqueuing event with Metrics info: "+event);
	
	enqueueESGEvent(event);
	return true;
    }
   
    public void handleESGEvent(ESGEvent event) {
	super.handleESGEvent(event);
    }

}
