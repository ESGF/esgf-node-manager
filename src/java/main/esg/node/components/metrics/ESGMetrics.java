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

    public ESGMetrics(String name) {
	super(name);
	log.info("Instantiating ESGMetrics...");
    }
    
    public void init() {
	log.info("Initializing ESGMetrics...");
	props = getDataNodeManager().getMatchingProperties("^metrics.*");
	metricsDAO = new MetricsDAO(DatabaseResource.getInstance().getDataSource(),Utils.getNodeID());
	metricsExpDAO = new MetricsExpDAO(DatabaseResource.getInstance().getDataSource(),Utils.getNodeID());
	metricsVarsDAO = new MetricsVarsDAO(DatabaseResource.getInstance().getDataSource(),Utils.getNodeID());
	metricsUsersDAO = new MetricsUsersDAO(DatabaseResource.getInstance().getDataSource(),Utils.getNodeID());
	startMetricsCollection();
    }
    
    public boolean fetchNodeStats() {
	//log.trace("metrics' fetchNodeStats() called....");
	boolean ret = true;
	//TODO
	return ret;
    }
    
    private void startMetricsCollection() {
	log.trace("launching node metrics timer");
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

    public void handleESGEvent(ESGEvent event) {
	super.handleESGEvent(event);
    }

}
