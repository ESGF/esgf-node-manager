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
 *   For details, see http://esgf.org/esg-node/                             *
 *   Please also read this link                                             *
 *    http://esgf.org/LICENSE                                               *
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

import java.util.Properties;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

//TODO: think about if we want this dependence on an outside package...
//may want to move the datasource managing to the ESGDataNodeManager subclass
//to keep this class prestine w.r.t. dependencies... think about it... -gmb
import esg.common.db.DatabaseResource; 
import esg.common.util.ESGFProperties;

public abstract class AbstractDataNodeManager implements DataNodeManager {

    private static Log log = LogFactory.getLog(AbstractDataNodeManager.class);

    private Map<String,ESGPeer> peers = null;
    private Map<String,DataNodeComponent> components = null;
    private Map<String,Properties> propCache = null;
    private Properties props = null;
    private String myName=null;
    
    public AbstractDataNodeManager() {
        myName="DN_MGR";
        //NOTE: May want to create these as either Synchronized Maps or ConcurrentHashMaps
        peers = new HashMap<String,ESGPeer>();
        components = new HashMap<String,DataNodeComponent>();
        propCache = new HashMap<String,Properties>();
        loadProperties();

        //NOTE: A quick lil short circuit to take database out of loop
        //so can test without having a database installed -gavin
        if(Boolean.valueOf(props.getProperty("esgf.test.no_db"))) {
            System.out.println("HEADS UP!!! Detected property esgf.test.no_db is "+props.getProperty("esgf.test.no_db")+" so NOT initializing database resources");
            return;
        }

        //Parcel out the database properties... and setup database connection pool.
        DatabaseResource.init(props.getProperty("db.driver")).setupDataSource(props);
    }
    
    public abstract void init();
    public String getName() { return myName; }
    
    //-------------------------------------------
    //Property Loading and providing...
    //-------------------------------------------
    private void loadProperties() {
        log.trace("Loading Properties");
        InputStream in = null;
        try {
            propCache.clear();
            props = new ESGFProperties();
            log.trace("Properties of esgf.properties file: "+props);
            log.trace("Loaded "+props.size()+" Properties");
        }catch(FileNotFoundException ex) {
            log.error("Could not find esgf.properties file!!!");
        }catch(IOException ex) {
            log.error("Problem loading node's property file!", ex);
        }catch(NullPointerException ex) {
            log.error("PROPERTY FILE INPUT STREAM IS NULL!!",ex);
            throw ex;
        }finally {
            try { if(in != null) in.close(); } catch (IOException ex) { log.error(ex); }
        }
    }
    
    public String getNodeProperty(String key) {
        return getNodeProperty(key,null);
    }
    public String getNodeProperty(String key, String defaultValue) {
        return props.getProperty(key,defaultValue);
    }
    
    
    //NOTE: the caching is a space vs speed trade off I think it is
    //cleaner to worry about such things (optimizations) at the root
    //of the issue than to make the interested parties (callers) worry
    //about how to parismoniously use this call. right? :-).  The
    //memory trade off is not so concrete since we are saving on "new"
    //property object creation, so if many calls are made it is
    //efficient.  If few calls are made, well... oh well... Just as
    //important, we are giving our callers less to worry about! :-)

    //Regex filtering the global properties by regex on keys
    public Properties getMatchingProperties(String regex) {
        log.trace("getting matching properties for ["+regex+"]");
        Properties matchProps = null;
        if( (matchProps = propCache.get(regex)) != null) {
            return matchProps;
        }
        matchProps = new Properties();
        String key = null;
        for(Enumeration keys = props.propertyNames(); keys.hasMoreElements();) {
            key = (String)keys.nextElement();
            //log.trace("inspecting: "+key);
            try{
                if(key.matches(regex)) {
                    //log.trace("matched: adding...");
                    matchProps.put(key, props.getProperty(key));
                }
            }catch(PatternSyntaxException ex) {
                log.error(ex.getMessage(),ex);
                break;
            }
        }
        propCache.put(regex,matchProps);
        log.trace("["+regex+"] => ("+matchProps.size()+" entries)");
        log.trace("propCache size = "+propCache.size());
        return matchProps;
    }
    
    //-------------------------------------------
    //DataNodeManager Interface Implementations...
    //-------------------------------------------
    public boolean registerComponent(DataNodeComponent component) {
        if (component == null) return false; 
        if ((component.getName() == null) || (component.getName().equals(DataNodeComponent.ANONYMOUS)) ) {
            log.warn("Will not register a component without a name... call setMyName(<name>)");
            return false;
        }
    
        log.trace("Registering Component: "+component.getName());
        if (component instanceof ESGPeer) {
            log.warn("WARNING: Will not register peer ["+component.getName()+"] as a component!");
            return false;
        }
        components.put(component.getName(), component);

        //Note: Casting because this method is not exposed by
        //interface but by the AbstractDataNodeComponent abstract
        //class)
        ((AbstractDataNodeComponent)component).setDataNodeManager(this);

        log.trace("Initializing newly registered component: "+component.getName());
        component.init();

        sendJoinNotification(component);
        component.addESGListener(this);
        return true;
    }
    
    public boolean hasComponent(String componentName) { return peers.containsKey(componentName); }

    public void removeComponent(String componentName) {
        DataNodeComponent component = components.remove(componentName);
        if(component == null) {
            log.trace("No component mapping to "+componentName+" (nothing to remove)");
            return;
        }
        log.trace("Removing Component: ["+componentName+"]");

        component.removeAllESGQueueListeners();
        component.removeAllESGListeners();
        sendUnjoinNotification(component);
    
        //TODO: add a call for the component to shut itself down
        //      accordingly.  Either here or in the component itself
        //      (see TODO comment in
        //      AbstractDataNodeComponent.unregister()).
    }
    
    //overloaded delegation of above
    public void removeComponent(DataNodeComponent component) {
        removeComponent(component.getName());
    }

    //TODO: Maybe think about the visibility of this method.
    public DataNodeComponent getComponent(String name) {
        return components.get(name);
    }
    
    public int numOfComponents() { return components.size(); }
    public String[] getComponentNames() { 
        return components.keySet().toArray(new String[] {""}); 
    }
    
    //-------------------------------------------
    //The Peer Proxy object is a type of DataNodeComponent but with
    //special logic for handling it's connectivity and life cycle.  It
    //is treated separely here for more *semantic* reasons than
    //anything else.  I want to enforce the thinking that peer
    //stubs represent a different type of beast than "ordinary"
    //components.
    //-------------------------------------------
    public boolean registerPeer(ESGPeer peer) {
        if (peer == null) return false;
        log.trace("2)) Registering Peer in node manager: "+peer.getName());
        ((AbstractDataNodeComponent)peer).setDataNodeManager(this);
        log.trace("3)) Initializing newly registered peer component: "+peer.getName());
        peer.init();
        if(peer.isValid()) {
            peers.put(peer.getName(), peer);
            log.trace("5)) Sending Queued Join Notification...");
            sendQueuedJoinNotification(peer);
            peer.addESGListener(this);
            return true;
        }
        log.warn("Sorry Not Able To Register This Peer: "+peer);
        peers.remove(peer); //just to be extra extra sure it isn't there
        return false;
    }
    
    public boolean hasPeer(String peerName) { return peers.containsKey(peerName); }

    public void removePeer(String peerName) {
        ESGPeer peer = peers.remove(peerName);  
        if(peer == null) {
            log.trace("No peer mapping to "+peerName+" (nothing to remove)");
            return;
        }
        log.trace("Removing Peer: ["+peerName+"]");
        peer.removeESGListener(this);
        sendQueuedUnjoinNotification(peer);
    }

    //overloaded delegation of above
    public void removePeer(ESGPeer peer) {
        removePeer(peer.getName());
    }

    //For communicating with a specific peer...
    public ESGPeer getPeer(String peerName) {
        return peers.get(peerName);
    }

    //For getting the list of peers...
    public List<ESGPeer> getPeers() {
        return Collections.unmodifiableList((List<ESGPeer>)peers.values());
    }

    public int numOfPeers() { return peers.size(); }
    public String[] getPeerNames() { return peers.keySet().toArray(new String[] {""}); }


    //--------------------------------------------
    //The code the sews together the path of events through the system.
    //--------------------------------------------

    public void connect(String parentName, String childName) {
        log.info("Connecting: "+parentName+" -to-> "+childName);
        try{
            //For the moment there is a 1:1 with name and component.
            getComponent(parentName).addESGQueueListener(getComponent(childName));
        }catch(Exception e) {
            log.warn("Could not establish connection: "+parentName+" -to-> "+childName);
        }
    }


    //--------------------------------------------
    //ESGListener dispatch methods: to all registered ESGListeners
    //calling their handleESGEvent method
    //--------------------------------------------

    private void sendJoinNotification(DataNodeComponent component) {
        log.trace("Sending Join Notifications for: "+component.getName());
        ESGJoinEvent joinEvent = new ESGJoinEvent(this,
                                                  component.getName(),
                                                  component,
                                                  ESGJoinEvent.JOIN);  
        fireESGEvent(joinEvent);
    }

    private void sendUnjoinNotification(DataNodeComponent component) {
        log.trace("Sending UN-Join Notifications for :"+component.getName());
        ESGJoinEvent unjoinEvent = new ESGJoinEvent(this,
                                                    component.getName(),
                                                    component,
                                                    ESGJoinEvent.UNJOIN);    
        fireESGEvent(unjoinEvent);
    }

    //TODO think about the visibility of this method... was protected
    //but making public so can be called.  Have to trust that the caller
    //is truly only calling when things are indeed all done being loaded!!
    //revisit this issue!!
    public void sendAllLoadedNotification() {
        log.trace("Sending All-Loaded Notification Broadcast...");
        ESGSystemEvent allLoadedEvent = new ESGSystemEvent(this,ESGSystemEvent.ALL_LOADED);
        fireESGEvent(allLoadedEvent);
    }


    //TODO: Think about how I could get a subset view of the
    //subscription list to use for firing events to a subset of
    //entities that adhere to a particular interface.  I.E. get all
    //the objects in the list that implement the FooListener interface
    //for me to call back to.  In the mean time we push events to
    //everyone and let them deal with proper selection for handling

    protected void fireESGEvent(ESGEvent esgEvent) {
        Collection<? extends ESGListener> esgListeners = components.values();
        log.trace("Firing ESGEvent: "+esgEvent);
        for(ESGListener listener: esgListeners) {
            listener.handleESGEvent(esgEvent);
        }
    }

    //-------------------------------------------
    //ESGQueueListener Dispatch methods to: ESGQueueListeners
    //calling their handleESGQueueEvent method via enqueueEvent()
    //putting on on their own event handling thread!
    //-------------------------------------------

    private void sendQueuedJoinNotification(DataNodeComponent component) {
        log.trace("Sending Queued Join Notifications for: "+component.getName());
        ESGJoinEvent joinEvent = new ESGJoinEvent(this,
                                                  component.getName(),
                                                  component,
                                                  ESGJoinEvent.JOIN);
        fireQueuedESGEvent(joinEvent);
    }

    private void sendQueuedUnjoinNotification(DataNodeComponent component) {
        log.trace("Sending Queued UN-Join Notifications for :"+component.getName());
        ESGJoinEvent unjoinEvent = new ESGJoinEvent(this,
                                                    component.getName(),
                                                    component,
                                                    ESGJoinEvent.UNJOIN);
        fireQueuedESGEvent(unjoinEvent);
    }

    /**
       Puts the event on the event queues of all registered
       (listening) components.
     */
    protected void fireQueuedESGEvent(ESGEvent esgEvent) {
        Collection<? extends ESGQueueListener> esgListeners = components.values();
        log.trace("Firing ESGQueuedEvent: "+esgEvent);
        for(ESGQueueListener listener: esgListeners) {
            listener.getESGEventQueue().enqueueEvent(esgEvent);
        }
    }

    //-------------------------------------------
    //ESGListener Interface Implementation...
    //-------------------------------------------
    public void handleESGEvents(List<ESGEvent> events) {
        for(ESGEvent event : events) {
            handleESGEvent(event);
        }
    }
    
    public void handleESGEvent(ESGEvent event) {
        //TODO:
        //Just stubbed for now...
        log.debug("DNM: Got An Event!!!!: "+event+"\nmessage: "+event.getMessage());
    }

    //TODO: YES!!! this is horrendous, I will use string builder and
    //be a bit nicer of memory and time when cleaning up ;-)
    //Promise.... Seriously! :-)
    public String toString() {
        String[] names;
        String out = ""; 

        out += this.getClass().getName()+":\n";
        out += "Number of Components: "+numOfComponents()+"\n";
        names = getComponentNames();
        for(String dncName : names) { out +="\t"+dncName+"\n"; }
        out += "Number of Peers  : "+numOfPeers()+"\n";
        names = getPeerNames();
        for(String peerName : names) { out +="\t"+peerName+"\n"; }
        return out;
    }

}
