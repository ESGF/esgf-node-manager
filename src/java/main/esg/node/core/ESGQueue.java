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

**/
package esg.node.core;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

public class ESGQueue {

    private static Log log = LogFactory.getLog(AbstractDataNodeComponent.class);
    private DataNodeComponent handler = null;
    private ThreadPoolExecutor pool = null;
    private ESGQueueController qController = null;
    private ESGBatchController bController = null;
    private Runnable command = null;

    public ESGQueue(DataNodeComponent handler) {
	this(handler,null,null);
    }

    public ESGQueue(DataNodeComponent handler, 
		    ESGQueueController qController,
		    ESGBatchController bController) {
	this(handler,
	     new ThreadPoolExecutor(2,20,10L,TimeUnit.MILLISECONDS,
				    new LinkedBlockingQueue<Runnable>(100),
				    new ESGGroupedThreadFactory(handler.getName()),
				    new ESGRejectPolicy(handler.getName())),
	     qController,
	     bController);
    }
    public ESGQueue(DataNodeComponent handler,
		    ThreadPoolExecutor pool, 
		    ESGQueueController qController, 
		    ESGBatchController bController) {
	
	this.handler = handler;
	this.pool = pool;
	if(qController == null) {
	    //TODO create default controller
	    this.qController = new ESGQueueController(handler.getName());
	}else {
	    this.qController = qController;
	}
	
	if(bController == null) {
	    this.bController = new ESGBatchController(handler.getName(), handler);
	}else {
	    this.bController = bController;
	}
	
    }
    
    public void init()  { }
    public String getName() { return handler.getName()+"_QUEUE"; }
    
    public ESGQueueController getQueueController() { return qController; }
    public ESGBatchController getBatchController() { return bController; }
    
    
    //Events are put on the eventQueue (BlockingQueue)
    public void enqueueEvent(final ESGEvent event) {
	log.trace(": Enqueuing event - "+event+" for component: "+handler.getName());
	pool.execute(new Runnable() {
		public void run() {
		    //TODO: make this actually a call to the batch
		    //controller that then will be able to rack up
		    //(batch) and distribute the event.  So basically
		    //this calls the batch controller and when the
		    //batch controller has reached its batch threshold
		    //then it will call
		    //handler.handleESGQueuedEvent(List<ESGEvent>s)
		    //For now... let's short that and call the single
		    //event handling method directly.
		    log.trace(ESGQueue.this.getName()+" - Dispatching event to component: "+handler.getName());
		    bController.handleESGQueuedEvent(event);
		}
	    });
    }


    //----

    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the <tt>execute</tt> method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     */
    static class ESGRejectPolicy implements RejectedExecutionHandler {
	private static Log log = LogFactory.getLog(ESGRejectPolicy.class);
	
	private String myName = null;

        /**
         * Creates a <tt>ESGRejectPolicy</tt>.
         */
        public ESGRejectPolicy(String name) { this.myName = name; }
		
        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
	    log.trace("Rejected Execution...");
            if (!e.isShutdown()) {
                r.run();
            }
        }

	public String getName() { return myName; }
	public String toString() { return myName+":"+this; }

    }

    //----

    static class ESGGroupedThreadFactory implements ThreadFactory {
	private static Log log = LogFactory.getLog(ESGGroupedThreadFactory.class);

        static final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;
	private String myName;

        ESGGroupedThreadFactory(String name) {
	    this.myName = name;
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();

	    //TODO: May have to make my own explicity thread group with name "name"

            namePrefix = "ESG-pool-["+myName+"]" +
                          poolNumber.getAndIncrement() +
                         "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }

	public String getName() { return myName; }
	public String toString() { return myName+":"+this; }
    }
    
}