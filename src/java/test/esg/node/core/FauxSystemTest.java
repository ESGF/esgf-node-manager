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

**/
package esg.node.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.components.replication.ReplicationAgent;
import esg.node.components.replication.BasicReplicationAgent;

public class FauxSystemTest {

    private static Log log = LogFactory.getLog(FauxSystemTest.class.getName());

    private static DataNodeManager dnmgr;

    public FauxSystemTest() {
	log.info("Instantiating FauxSystemTest...");
	dnmgr = new ESGDataNodeManager();
    }

    public void doIt() {
	ReplicationAgent ra0  = new BasicReplicationAgent("Faux Replication Agent 0");
	ra0.init();
	Gateway gateway0      = new BasicGateway("Faux Gateway 0");
	gateway0.init();
	dnmgr.registerGateway(gateway0);
	dnmgr.registerComponent(ra0);
	
	System.out.println("1) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0 and gateway 0]\n----\n");

	ReplicationAgent ra1 = new BasicReplicationAgent("Faux Replication Agent 1");
	ReplicationAgent ra2 = new BasicReplicationAgent("Faux Replication Agent 2");
	Gateway gateway1     = new BasicGateway("Faux Gateway 1");
	dnmgr.registerComponent(ra1);
	dnmgr.registerGateway(gateway1);
	dnmgr.registerComponent(ra2);

	System.out.println("2) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0,1,2 and gateway 0,1]\n----\n");

	dnmgr.removeComponent(ra2);
	dnmgr.removeGateway(gateway1);

	System.out.println("3) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0,1 and gateway 0]\n----\n");

	ra2.unregister(); //This should do nothing as it was already removed from dnmgr
	ra1.unregister();

	System.out.println("4) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0 and gateway 0]\n----\n");

	ra1.publishDataSet();
	ra0.publishDataSet();
	
	ReplicationAgent ra3 = new BasicReplicationAgent("Faux Replication Agent 3");
	dnmgr.registerComponent(ra3);
	Gateway gateway2     = new BasicGateway("Faux Gateway 2");
	dnmgr.registerComponent(gateway2); //this will be ignored and a warning issued.
	System.out.println("\n5) The state of the dnmgr = \n"+dnmgr);
	System.out.println("[should have replication agent 0,3 and gateway 0]\n----\n");

	System.out.println("---");
    }

    public static void main(String[] args) {
	FauxSystemTest fst = new FauxSystemTest();
	fst.doIt();
	System.gc();
	//at this point ra3 should have fallen out of scope and the
	//finalize should have cleaned it up and unregistererd it from
	//the data node manager... do we see it?
	System.out.println(dnmgr+"\n---");
    }
}