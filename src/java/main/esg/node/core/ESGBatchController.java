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
*   For details, see http://esgf.org/esg-node/                    *
*   Please also read this link                                             *
*    http://esgf.org/LICENSE                                      *
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
   
   The controller is meant to be the conduit of communication from the
   manager to the associated queue for a given component.  Through
   this controller information can be sent to, and received by; the
   manager.

   (I will be the first to admit that this whole class' purpose as
   constructed is a bit bogus.  There is a "real" way to do this but
   at the moment not sure of the best plan to get there.  Curse you
   SEDA paper! :-) See NOTE below) -gmb
   
**/
package esg.node.core;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

public class ESGBatchController implements ESGListener {

    private static Log log = LogFactory.getLog(ESGBatchController.class);    
    private String myName = null;
    private ESGQueueListener handler = null;
    private int batchSize = 1;
    private int collectSize = 0;
    private List<ESGEvent> events = null;

    //The default is no batching...
    public ESGBatchController(String name, ESGQueueListener handler) {
	this(name,handler,1);
    }

    public ESGBatchController(String name, ESGQueueListener handler, int initBatchSize) { 
	this.myName = name; 
	this.handler = handler;
	this.events = new ArrayList<ESGEvent>();
	this.batchSize = initBatchSize;
    }

    public void setBatchSize(int newBatchSize) { this.batchSize = newBatchSize; }


    //--------------------------------------------------------
    //****WHERE EVENT IS (Finally) SENT TO THE HANDLER!!!*****
    //--------------------------------------------------------

    //continually called by ESGQueue... in it's enqueued runnable
    //delegating to handler...
    public void handleESGQueuedEvent(ESGEvent event) {
	//if batch size is 1 simply delegate through to handler
	//calling the singular version of the function...  (don't have
	//to go through doing extra collection work for no reason).
	if(batchSize == 1) {
	    handler.handleESGQueuedEvent(event);
	    return;
	}
	
	//NOTE: This is kinda kokey to me... Does not seem that this
	//will buy us anything... batching events after they have
	//already been singularly displached here.  I will try this in
	//a performance tuning effort, however, I honestly think this
	//is not the way to do batching.  I would have to go directly
	//to the workqueue held by the ThreadPoolExecutor and "drain"
	//the pool and dispatched what was drained.  The only issue
	//with that is that I can't really modify the workqueue from
	//under the ThreadPoolExecutor may put the thread pool in a
	//weird state and cause us to doubly dispatch events since
	//even when we get the queue it is still active.  These are
	//all optimizations that can be investigated later.  I just
	//wanted to put in a basic implementation (even though I don't
	//believe in it). :-)

	++collectSize;
	if(collectSize < batchSize) {
	    events.add(event);
	}else if(collectSize >= batchSize) {
	    handler.handleESGQueuedEvents(events);
	    collectSize = 0;
	    events.clear();
	}
	
	//NOTE: This should never happen...  
	//(perhaps should get rid of collectSize altogether and just use .size()? - oops in a rush...)
	if(collectSize != events.size()) {
	    log.warn("There is a size inconsistency here ["+collectSize+"] != ["+events.size()+"]");
	    //quick fix: but not solving the real reason...
	    collectSize = events.size();
	}
    }

    
    //Get messages from manager or other control object...
    public void handleESGEvent(ESGEvent event) { 
	log.trace("Batch Controller ["+myName+"] Handling ingress control event...");
	//TODO: handle events pertaining to batch control
    }    

    public String getName() { return myName; }
    public String toString() { return myName+":"+this; }

}
