/***************************************************************************
*                                                                          *
*  Organization: Lawrence Livermore National Lab (LLNL)                    *
*   Directorate: Computation                                               *
*    Department: Computing Applications and Research                       *
*      Division: S&T Global Security                                       *
*        Matrix: Atmospheric, Earth and Energy Division                    *
*       Program: PCMDI                                                     *
*       Project: Earth Systems Grid Federation (ESGF) Data Node Software   *
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
*   Earth System Grid Federation (ESGF) Data Node Software Stack           *
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

   A simple helper/utility class to be a central location for common
   Event related manipulations tasks.

**/
package esg.node.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.common.Utils;
import esg.common.service.ESGRemoteEvent;

public class ESGEventHelper {

    private static final Log log = LogFactory.getLog(ESGEventHelper.class);    

    public ESGEventHelper() { }


    //For ESGEvents with ESGRemoteEvents embedded...
    //this unwraps the remote event but setting it up to be
    //resent from HERE.  This allows for masquarading events
    //essentially NAT'ong for events.
    public static ESGRemoteEvent asMyRemoteEvent(ESGEvent in) {
        ESGRemoteEvent rEvent = null;
        if((rEvent = in.getRemoteEvent()) == null) {
            log.warn("The encountered event does not contain a remote event");
            return null;
        }
        return new ESGRemoteEvent(Utils.getMyServiceUrl(),rEvent.getMessageType(),in.getData(),rEvent.getSeqNum());
    }

    //Here we create an event that is suitable as a response to the
    //input remote event.  the request originator is preserved as the
    //"origin" value of the event, the originator's sequence number is
    //reflected back as well as the message type. The payload is what
    //we specify
    public static ESGRemoteEvent createResponseOutboundEvent(ESGEvent in) {
        if(in.getRemoteEvent() == null) {
            log.warn("This event does not contain a remote event... ILLEGAL");
            return null;
        }
        return createResponseOutboundEvent(in.getRemoteEvent(), in.getData());
    }
    public static ESGRemoteEvent createResponseOutboundEvent(ESGRemoteEvent in, Object payload) {
        in.copy(new ESGRemoteEvent(Utils.getMyServiceUrl(),in.getMessageType(),payload,in.getSeqNum()));
        return in;
    }

    //Here we create a remote event that respects the input remote
    //event's TTL, that has been properly decremented by the
    //DataNodeService on initial ingress. and because of the copy we
    //are respecting the "origin" value associated with this remote
    //event - very important
    public static ESGRemoteEvent createRelayedOutboundEvent(ESGRemoteEvent in) {
        in.copy(new ESGRemoteEvent(Utils.getMyServiceUrl(),in.getMessageType(),in.getPayload(),Utils.nextSeq()));
        return in;
    }

    //Here we take an input remote event and create and event that
    //makes it look like the data came from THIS node - our version of
    //NAT'ing or Spoofing.  (note: when instantiating a new remote
    //event you claim this node as the "origin" - very important make
    //sure that's what you want) Also the TTL is set to initial value
    public static ESGRemoteEvent createProxiedOutboundEvent(ESGRemoteEvent in) {
        //Create the string for *our* callback address...
        return new ESGRemoteEvent(Utils.getMyServiceUrl(),in.getMessageType(),in.getPayload(),in.getSeqNum());
    }

}