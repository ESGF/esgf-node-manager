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
package esg.gateway.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import java.net.MalformedURLException;
import java.net.InetAddress;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.HessianRuntimeException;

import esg.common.Utils;
import esg.common.service.ESGRemoteEvent;
import esg.node.service.ESGDataNodeService;



public class ESGGatewayServiceImpl implements ESGGatewayService {

    private static final Log log = LogFactory.getLog(ESGGatewayServiceImpl.class);
    private HessianProxyFactory factory = null;
    private ESGDataNodeService datanodeService = null;

    public ESGGatewayServiceImpl() {
	factory = new HessianProxyFactory();
    }
    
    public boolean ping() {
	log.trace("Gateway service got \"ping\"");
	return true;
    }
    
    public void register(ESGRemoteEvent evt) {
	log.trace("Gateway service got \"register\" call from datanode with event: ["+evt+"]");
	
	//Triage incoming revent...
	if(evt.getMessageType() != ESGRemoteEvent.REGISTER) {
	    log.trace("Registration called with wrong event type... dropping on floor...");
	    return;
	}

	//Create the string for *our* callback address...
	String myLocation = null;
	try{
	    myLocation = "http://"+InetAddress.getLocalHost().getCanonicalHostName()+"/esg-node/gateway";
	}catch (java.net.UnknownHostException ex) {
	    log.error("Could not build proper location string for myself",ex);
	}


	//Create proxy endpoint for data node service that sent us this event.
	try {
	    datanodeService = (ESGDataNodeService)factory.create(ESGDataNodeService.class, evt.getSource());
	}catch(MalformedURLException ex) {
	    log.warn("Could not connect to serviceURL ["+evt.getSource()+"]",ex);
	}
	
	//Assemble info to create an event and send it to data node in response to this 'register' call.
	try {
	    ESGRemoteEvent registrationResponseEvent = new ESGRemoteEvent(myLocation,ESGRemoteEvent.REGISTER,Utils.nextSeq());
	    log.trace("Completing Registration By Making Remote Call to \"notify\" method, sending: "+registrationResponseEvent);
	    datanodeService.notify(registrationResponseEvent);
	    log.trace("Registration Request Successfully Sent...");
	}catch (HessianRuntimeException ex) {
	    log.error("Problem calling \"register\" on ["+evt.getSource()+"] "+ex.getMessage());
	}

	
    }

}
