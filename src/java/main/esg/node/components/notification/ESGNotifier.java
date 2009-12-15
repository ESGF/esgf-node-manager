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

   This class provides the notification logic for alerting users when
   specific events occur

**/
package esg.node.components.notification;

import java.util.Properties;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.Session;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.db.DatabaseResource;
import esg.node.core.*;

public class ESGNotifier extends AbstractDataNodeComponent {
    
    private static final Log log = LogFactory.getLog(ESGNotifier.class);

    private static final String subject = "ESG DataNode Notification: ";

    private Properties props = null;
    private Session session = null;
    private boolean isBusy = false;
    private boolean dataAvailable = true; //zoiks make false, just true for testing!
    private StringBuilder endusers = null;
    private String messageTemplate = null;
    private NotificationDAO notificationDAO = null;

    private Pattern dataset_pattern = null;
    private Pattern files_pattern = null;
    

    public ESGNotifier(String name) { 
	super(name); 
	log.info("Instantiating ESGNotifier...");
    }
    
    public void init() {
	log.info("Initializing ESGNotifier...");

	log.trace("getDataNodeManager() = "+getDataNodeManager());
	props = getDataNodeManager().getMatchingProperties("^mail.*");

	messageTemplate = loadMessage(props.getProperty("mail.notification.messageTemplateFile"));
	session = Session.getInstance(props, null);
	notificationDAO = new NotificationDAO(DatabaseResource.getInstance().getDataSource());

	dataset_pattern = Pattern.compile("@@dataset@@");
	files_pattern   = Pattern.compile("@@updated_files@@");

	performNextNotification();
    }

    private String loadMessage(String filename) {
	StringBuilder message = new StringBuilder();
	String line = null;
	InputStream is = null;
	try {
	    Resource messageTemplateResource = new Resource(filename);
	    is = messageTemplateResource.getInputStream();
	    BufferedReader reader = new BufferedReader( new InputStreamReader(is,"UTF-8"));
	    while((line = reader.readLine()) != null) {
		message.append(line).append("\n");
	    }
	}catch(IOException ex) {
	    log.error("Problem loading email message template!", ex);
	}finally {
	    try { if(is != null) is.close(); } catch (IOException iox) { log.error(iox); }
	}
	return message.toString();
    }

    private boolean sendNotification(String dataset_id, String[] endusers, String[] changedFiles) {
	Message msg = null;
	boolean success = false;
	String messageText = "";
	String myAddress = null;
	Matcher matcher = null;
	try {
	    msg=new MimeMessage(session);
	    msg.setHeader("X-Mailer","ESG DataNode IshMailer");
	    msg.setSentDate(new Date());
	    myAddress = props.getProperty("mail.admin.address");
	    msg.setFrom(new InternetAddress(myAddress));
	    msg.setSubject(subject+"("+dataset_id+")");
	    
	    //Create our comma separated list of email addresses...
	    boolean notfirst = false;
	    StringBuilder recipientAddresses = new StringBuilder();
	    for(String enduser : endusers) {
		if(notfirst) recipientAddresses.append(",");
		recipientAddresses.append(enduser);
		notfirst=true;
	    }
	    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(myAddress));
	    msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(recipientAddresses.toString()));
	    
	    notfirst = false;
	    StringBuilder files = new StringBuilder();
	    for(String file : changedFiles) {
		if(notfirst) files.append("\n");
		files.append(file);
		notfirst=true;
	    }
	    
	    //NOTE: This is a two pass replacement... This could
	    //probably be done more elegantly in one, read up about
	    //regex and make it happen.
	    matcher = dataset_pattern.matcher(messageTemplate);
	    String tmp = matcher.replaceAll(dataset_id);
	    matcher = files_pattern.matcher(tmp);
	    messageText = matcher.replaceAll(files.toString());
	    
	    msg.setText(messageText);
	    
	    Transport.send(msg);
	    success = true;
	}catch(MessagingException ex) {
	    log.error("Problem Sending Email Notification: ("+subject+")\n"+messageText+"\n",ex);
	}
	
	msg = null; //gc niceness
	
	return success;
    }
    
    protected boolean fetchNextUpdates() {
	log.trace("Fetching Next set of notification updates");
	NotificationDAO.NotificationRecipientInfo nri = notificationDAO.getNotificationRecipientInfo();
	return sendNotification(nri.dataset_id,nri.endusers,nri.changedFiles);
    }

    protected void markTime() {
	log.trace("Marking Time of notification completion");
	notificationDAO.markLastCompletionTime();
    }

    private void performNextNotification() {
	log.trace("launching notification timer");
	long delay  = Long.parseLong(props.getProperty("mail.notification.initialDelay"));
	long period = Long.parseLong(props.getProperty("mail.notification.period"));
	log.trace("notification delay: "+delay+" sec");
	log.trace("notification period: "+period+" sec");
	
	Timer timer = new Timer();
	timer.schedule(new TimerTask() {
		public final void run() {
		    //log.trace("Checking for new notification updates...");
		    if(ESGNotifier.this.dataAvailable && !ESGNotifier.this.isBusy) {
			ESGNotifier.this.isBusy = true;
			if(fetchNextUpdates()) {
			    markTime();
			    ESGNotifier.this.dataAvailable = false;
			}
			ESGNotifier.this.isBusy = false;
		    }
		}
	    },delay*1000,period*1000);
    }
    

    //Triggers the "sweep" in our mark-and-sweep notification
    //mechanism.  At the next timer interval we will fetch the
    //next bach of updates and send them out if it has been
    //indicated to us that data is available (i.e. this method has
    //been called)
    public void handleESGEvent(ESGEvent event) {
	super.handleESGEvent(event);
	
	//TODO: Put in code to be more discerning regarding what
	//events we choose to handle.
	
	dataAvailable = true;
	log.trace("Setting dataAvailable to : "+dataAvailable);

    }

    
}