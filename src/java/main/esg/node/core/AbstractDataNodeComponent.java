/*****************************************************************

  Organization: Lawrence Livermore National Lab (LLNL)
   Directorate: Computation
    Department: Computing Applications and Research
      Division: S&T Global Security
        Matrix: Atmospheric, Earth and Energy Division
       Program: PCMDI
       Project: Earth Systems Grid
  First Author: Gavin M. Bell (gavin@llnl.gov)

   Description:

*****************************************************************/
package esg.node.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import java.util.List;
import java.util.ArrayList;

public abstract class AbstractDataNodeComponent implements DataNodeComponent {
    
    private static Log log = LogFactory.getLog(AbstractDataNodeComponent.class);

    private String myName=null;
    private DataNodeManager dataNodeManager = null;

    //Expose this list to available subclasses...
    //so that they too may be able to manage event listeners
    protected List<ESGListener> esgListeners = null;

    public AbstractDataNodeComponent(String name) {
	this.myName = name;
	this.esgListeners = new ArrayList<ESGListener>();
    }
    public AbstractDataNodeComponent() { this(DataNodeComponent.ANONYMOUS); }

    public abstract void init();
    
    protected final void setMyName(String myName) { this.myName=myName; }
    protected final void setDataNodeManager(DataNodeManager dataNodeManager) {
	this.dataNodeManager = dataNodeManager;
    }

    public String getName() { return myName; }
    public final DataNodeManager getDataNodeManager() { return dataNodeManager; }

    //To be nice this should be called upon destruction
    //(TODO: Make this a phantom reference in the manager)
    public void unregister() {
	if(dataNodeManager == null) return;
	dataNodeManager.removeComponent(this);
	dataNodeManager = null;
    }

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
    //calling their esgActionPerformed method
    //--------------------------------------------
    protected void fireESGEvent(ESGEvent esgEvent) {
	log.trace("Firing Event: "+esgEvent);
	for(ESGListener listener: esgListeners) {
	    listener.esgActionPerformed(esgEvent);
	}
    }

    //-------------------------------------------
    //ESGListener Interface Implementation...
    //-------------------------------------------
    public void esgActionPerformed(ESGEvent event) {
	//TODO:
	//Just stubbed for now...
	log.trace("Got Action Performed: ["+myName+"]:["+this.getClass().getName()+"]: Got An Event!!!!: "+event);
    }

    //We want to do what we can not to leave references laying around
    //in the data node manager.  Unregistering will explicitly take us
    //out of the data node manager.  (TODO: look into making the data
    //node manager use phatom references to components).
    protected void finalize() throws Throwable { super.finalize(); unregister(); }

}
