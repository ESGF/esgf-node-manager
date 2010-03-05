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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public abstract class AbstractDataNodeComponent implements DataNodeComponent {
    
    private static Log log = LogFactory.getLog(AbstractDataNodeComponent.class);

    private String myName=null;
    private DataNodeManager dataNodeManager = null;
    private ESGQueue eventQueue = null;

    //Expose this list to available subclasses...
    //so that they too may be able to manage event listeners
    protected List<ESGListener> esgListeners = null;
    protected List<ESGQueueListener> esgQueueListeners = null;
    protected Map<String,ESGQueueListener> esgQueueListenersMap = null;

    public AbstractDataNodeComponent(String name) {
	this.myName = name;
	this.esgListeners = new ArrayList<ESGListener>();
	//this.esgQueueListeners = new ArrayList<ESGQueueListener>();
	this.esgQueueListenersMap = new HashMap<String,ESGQueueListener>();
	this.eventQueue = new ESGQueue(this);
    }
    public AbstractDataNodeComponent() { this(DataNodeComponent.ANONYMOUS); }

    public void start() { log.info("Starting... "+this.getClass().getName()); init(); }
    public abstract void init();

    
    protected final void setMyName(String myName) { this.myName=myName; }
    protected final void setDataNodeManager(DataNodeManager dataNodeManager) {
	this.dataNodeManager = dataNodeManager;
    }

    public String getName() { return myName; }
    public final DataNodeManager getDataNodeManager() { return dataNodeManager; }

    //To be nice this should be called upon destruction
    //(TODO: Make this a phantom reference in the manager)
    public void stop()  { log.info("Stopping... "+this.getClass().getName()); unregister();}
    public void unregister() {
	if(dataNodeManager == null) return;
	dataNodeManager.removeComponent(this);
	dataNodeManager = null;
    }


    //*******************************************
    //-------------------------------------------
    //Subscribe/Unsubscribe ESGListeners ("system" events)
    //-------------------------------------------
    public void addESGListener(ESGListener listener) {
	if(listener == null) return;
	log.trace("Adding Listener: "+listener);
	esgListeners.add(listener);
    }
    
    public void removeESGListener(ESGListener listener) {
	log.trace("Removing Listener: "+listener);
	esgListeners.remove(listener);
    }

    //--------------------------------------------
    //Event dispatching to all registered ESGListeners
    //calling their handleESGEvent method
    //--------------------------------------------
    protected void fireESGEvent(ESGEvent esgEvent) {
	log.trace("Firing Event: "+esgEvent);
	for(ESGListener listener: esgListeners) {
	    listener.handleESGEvent(esgEvent);
	}
    }
    
    //-------------------------------------------
    //ESGListener Interface Implementation...
    //-------------------------------------------
    public void handleESGEvent(ESGEvent event) {
	//TODO:
	//Just stubbed for now...
	log.trace("(no-op) handling event -["+myName+"]:["+this.getClass().getName()+"]: Got An Event!!!!: "+event);
    }
    //*******************************************
    


    //*******************************************
    //-------------------------------------------
    //Subscribe/Unsubscribe ESGQueueListeners ("application" events)
    //-------------------------------------------
    public void addESGQueueListener(ESGQueueListener listener) {
	if(listener == null) return;
	log.trace("Adding Queue Listener: "+listener);
	esgQueueListenersMap.put(listener.getName(),listener);
    }
    
    public void removeESGQueueListener(ESGQueueListener listener) {
	log.trace("Removing Listener: "+listener);
	esgQueueListenersMap.remove(listener.getName());
    }

    public void removeESGQueueListener(String listenerName) {
	log.trace("Removing Queue Listener named: "+listenerName);
	esgQueueListenersMap.remove(listenerName);
    }

    //--------------------------------------------
    //Event dispatching to all registered ESGQueuListeners
    //calling their Queue's handleEvent method
    //(The queued version of the "fire" method)
    //--------------------------------------------
    protected void enqueueESGEvent(ESGEvent esgEvent) {
	log.trace("Enqueuing Event (to all listeners): "+esgEvent);
	for(ESGQueueListener listener: esgQueueListenersMap.values()) {
	    listener.getESGEventQueue().enqueueEvent(esgEvent);
	}
    }
    protected void enqueueESGEvent(String destinationListenerName, ESGEvent esgEvent) {
	log.trace("Enqueuing Event: "+esgEvent+" --to--> "+destinationListenerName);
	esgQueueListenersMap.get(destinationListenerName).getESGEventQueue().enqueueEvent(esgEvent);
    }

    //-------------------------------------------
    //ESGQueueListener Interface Implementation... (Called by my ESGQueue)
    //-------------------------------------------
    public boolean handleESGQueuedEvents(List<ESGEvent> events) {
	boolean ret = true;
	log.trace("(no-op) handling enqueued events ["+myName+"]:["+this.getClass().getName()+"]: Got Batch of ["+events.size()+"] QueuedEvents! ");
	for(ESGEvent event : events) {
	    ret &= handleESGQueuedEvent(event);
	}
	return ret;
    }
    public boolean handleESGQueuedEvent(ESGEvent event) {
	//TODO
	log.trace("(no-op) handling enqueued event ["+myName+"]:["+this.getClass().getName()+"]: Got A QueuedEvent!!!!: "+event);
	return false;
    }
    
    public ESGQueue getESGEventQueue() { return eventQueue; }
    //*******************************************

    //We want to do what we can not to leave references laying around
    //in the data node manager.  Unregistering will explicitly take us
    //out of the data node manager.  (TODO: look into making the data
    //node manager use phatom references to components).
    protected void finalize() throws Throwable { super.finalize(); unregister(); }

}
