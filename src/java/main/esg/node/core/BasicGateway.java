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
        This is essentially the wrapped stubs to the gateway.
        They are the "client" side of the rpc call where calls
        ORIGINATE. (from DataNode -to-> Gateway).

        ----------------------------------------------------
        THIS CLASS REPRESNTS *EGRESS* CALLS TO THE GATEWAY!!
        ----------------------------------------------------
           (I don't think I can make this any clearer)

*****************************************************************/
package esg.node.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import com.caucho.hessian.client.HessianRuntimeException;

import java.net.MalformedURLException;
import java.net.InetAddress;

import esg.gateway.service.ESGGatewayService;
import esg.common.service.ESGRemoteEvent;
import esg.common.Utils;

public class BasicGateway extends Gateway{

    private static final Log log = LogFactory.getLog(BasicGateway.class);
    private ESGGatewayService gatewayService = null;

    public BasicGateway(String name, String serviceURL) { super(name,serviceURL); }
    public BasicGateway(String name) { this(name,null); }

    //The idea here is to establish communication with the rpc
    //endpoint this class is a stub for. init() may be called multiple times until valid;
    public void init() {
	if(isValid) {
	    log.info("I am already valid... :-)");
	    return;
	}

	//I am not a valid object if I don't have a name
	if(getName() == null) {
	    log.warn("Cannot initialize Gateway : NO NAME!!");
	    isValid = false;
	    return;
	}

	//I am not a valid object if I don't have an endpoint URL
	if(getServiceURL() == null) {
	    log.warn("Cannot initialize Gateway ["+getName()+"]: NO SERVICE URL!!");
	    isValid = false;
	    return;
	}

	try {
	    gatewayService = (ESGGatewayService)factory.create(ESGGatewayService.class, getServiceURL());
	}catch(MalformedURLException ex) {
	    log.warn("Could not connect to serviceURL ["+getServiceURL()+"]",ex);
	    isAvailable = false;
	}
	
	if(gatewayService != null) {
	    log.trace(getName()+" Proxy properly initialized");
	    isValid = true;
	}else {
	    log.warn(getName()+" Proxy NOT properly initialized");
	    isValid = false;
	}
    }
    
    
    //-----------------------------------------------
    //Delgated remote methods...
    //-----------------------------------------------

    //The simplest of communications to make sure folks are alive and
    //that the RPC works at a basic level... If and only if I can ping
    //you and you respond (return true) that I can consider you a
    //VALID end point, and therefore, my proxy ;-), *I* am valid to
    //use for communicating to you, mr. gateway. gyot it? :-) 

    //A return true of false indicate that you are able to be
    //communicated with, however a return of false is you responding
    //that you are open to being talked to and thus I, by proxy am not
    //going to perform calls on your behalf, and therefore not valid.

    //This semantically expected to be the START of the communication
    //between data node and gateway.  (called by the bootstrapping
    //service: ESGDataNodeService)
    public boolean ping() { 
	log.trace("ping -->> ["+getName()+"]");
	boolean response = false;
	try {
	    //TODO see about changing the timeout so don't have to wait forever to fail!	    
	    response = gatewayService.ping();
	    System.out.println( (response ? "[OK]" : "[BUSY]") );
	}catch (HessianRuntimeException ex) {
	    System.out.println("[FAIL]");
	    log.error("Problem calling \"ping\" on ["+getServiceURL()+"] "+ex.getMessage());
	    response = false;
	}
	isAvailable = (response && isValid);
	log.trace("isValid = "+isValid);
	log.trace("response = "+response);
	log.trace("isAvailable = "+isAvailable);
	return isAvailable;
	//FYI: the isAvailable() method is defined in superclass)
    }
    
    //Present the gateway with this client's identity and thus
    //callback address for making calls back to the data node
    //services for sending notifications
    public boolean registerToGateway() { 
	if(!isValid  || !isAvailable) {
	    log.warn("May not issue \"register\" rpc call unless object has been initialized to be made valid and ping has been issued to make sure I am available!!!");
	    return false;
	}

	String myLocation = null;
	try{
	    myLocation = "http://"+InetAddress.getLocalHost().getCanonicalHostName()+"/esg-node/datanode";
	}catch (java.net.UnknownHostException ex) {
	    log.error("Could not build proper location string for myself",ex);
	}
	
	ESGRemoteEvent registrationEvent = new ESGRemoteEvent(myLocation,ESGRemoteEvent.REGISTER,Utils.nextSeq());
	
	try {
	    log.trace("Making Remote Call to \"register\" method, sending: "+registrationEvent);
	    gatewayService.register(registrationEvent);
	    return true;
	}catch (HessianRuntimeException ex) {
	    log.error("Problem calling \"register\" on ["+getServiceURL()+"] "+ex.getMessage());
	    return false;
	}
    }
    
}
