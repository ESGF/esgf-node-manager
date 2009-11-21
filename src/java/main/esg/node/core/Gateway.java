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

import com.caucho.hessian.client.HessianProxyFactory;

public abstract class Gateway extends AbstractDataNodeComponent{

    private static final Log log = LogFactory.getLog(Gateway.class);
    private String serviceURL = null;
    private Object proxyObject;

    protected boolean isValid = false;
    protected boolean isAvailable = false;
    protected HessianProxyFactory factory;
    
    public Gateway(String name, String serviceURL) { 
	super(name); 
	this.serviceURL = serviceURL;
	this.factory = new HessianProxyFactory();
    }

    public Gateway(String name) { this(name,null); }

    public abstract void init();
    protected void setServiceURL(String serviceURL) { this.serviceURL = serviceURL; }
    public String getServiceURL() { return serviceURL; }
    public boolean isValid() { return isValid; }
    public boolean isAvailable() { return isAvailable; }

    
    //-----------------------------------------------------------------
    //Delgating: remote method wrappers.
    //-----------------------------------------------------------------
    //override me (all services should have a "ping" remote method! according to me! :-)
    public abstract boolean ping();

    //Present the gateway with a token and callback address for 
    //making calls back to the data node services.
    public abstract boolean registerToGateway();
    //-----------------------------------------------------------------
    


    //-----------------------------------------------------------------
    //Overriding superclass to perform gateway object specific unregistration
    //from the data node manager. (unrelated to RPC, internal management)
    //-----------------------------------------------------------------
    public void unregister() {
	DataNodeManager dataNodeManager = getDataNodeManager();
	if(dataNodeManager == null) return;
	if(this instanceof Gateway) { dataNodeManager.removeGateway(this); }
	//Note: don't have to null out mgr like in the super class
	//because I am never holding on to a mgr handle directly ;-)
    }
    //-----------------------------------------------------------------


    public String toString() {
	return "Gateway: ["+getName()+"] -> ["+(serviceURL == null ? "NULL" : serviceURL)+"] ("+isValid+")";
    }
    
}
