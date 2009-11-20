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
        originate. (from DataNode -to-> Gateway).

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

public class BasicGateway extends Gateway{

    private static final Log log = LogFactory.getLog(BasicGateway.class);
    private ESGGatewayService gatewayService = null;

    public BasicGateway(String name, String serviceURL) { super(name,serviceURL); }
    public BasicGateway(String name) { this(name,null); }

    //The idea here is to establish communication with the rpc
    //endpoint this class is a stub for.
    public void init() {
	if(getServiceURL() == null) {
	    log.warn("Cannot initialize Gateway ["+getName()+"]: NO SERVICE URL!!");
	    isValid = false;
	    return;
	}

	try {
	    gatewayService = (ESGGatewayService)factory.create(ESGGatewayService.class, getServiceURL());
	}catch(MalformedURLException ex) {
	    log.warn("Could not connect to serviceURL ["+getServiceURL()+"]",ex);
	    isValid = false;
	}
	
	if(gatewayService != null) {
	    log.trace(getName()+" Proxy initialized");
	    isValid = true;
	}
    }



    //-----------------------------------------------
    //Delgated remote methods...
    //-----------------------------------------------

    //The simplest of communications to make sure folks are alive and
    //that the RPC works at a basic level
    public boolean ping() { 
	log.trace("ping... ");
	if(!isValid) {
	    log.warn("May not issue \"ping\" rpc call unless object has been initialized and made valid!!!");
	    return false;
	}

	try {
	    //TODO see about changing the timeout so don't have to wait forever to fail!
	    System.out.println( (gatewayService.ping() ? "[OK]" : "[FAIL]"));
	    return true;
	}catch (HessianRuntimeException ex) {
	    log.error("Problem calling \"ping\" on ["+getServiceURL()+"] "+ex.getMessage());
	    return false;
	}
    }
    
    //Present the gateway with this client's identity and thus
    //callback address for making calls back to the data node
    //services for sending notifications
    public boolean registerToGateway() { 
	if(!isValid) {
	    log.warn("May not issue \"register\" rpc call unless object has been initialized and made valid!!!");
	    return false;
	}

	String myLocation = null;
	try{
	    myLocation = "http://"+InetAddress.getLocalHost().getCanonicalHostName()+"/esg-node/datanode";
	}catch (java.net.UnknownHostException ex) {
	    log.error("Could not build proper location string for myself",ex);
	}
	
	ESGRemoteEvent registrationEvent = new ESGRemoteEvent(myLocation,ESGRemoteEvent.REGISTER);
	
	try {
	    log.trace("Making Remote Call to \"register\" method, sending: "+registrationEvent);
	    gatewayService.register(registrationEvent);
	    log.trace("successful");
	    return true;
	}catch (HessianRuntimeException ex) {
	    log.error("Problem calling \"register\" on ["+getServiceURL()+"] "+ex.getMessage());
	    return false;
	}
    }
    
}
