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

package esg.node.components.simple;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.core.*;

public class ESGSimpleComponentTest {
    private static final Log log = LogFactory.getLog(ESGSimpleComponentTest.class);

    private static AbstractDataNodeManager testDnm = null;
    
    public ESGSimpleComponentTest() {
	log.trace("Instantiating Test Case for ESGSimpleComponentTest");
    }
    
    @BeforeClass
    public static void initialSetup() {
        testDnm = new AbstractDataNodeManager() {
                public void init() {
                    System.out.println("Initializing anon test node manager class");
                }
            };
    }

    @Before
    public void setup() { }

    @Test
    public void testEventPassingThroughComponents() {
        System.out.println("TEST SIMPLE COMPONENTS");

        //Create components... and name them (unique names)...
        System.out.print("making thing1: ");
	ESGSimpleComponent thing1 = new ESGSimpleComponent("THING1") {
                public void handleESGEvent(ESGEvent esgEvent) {
                    //we only care about system all loaded events
                    if(!(esgEvent instanceof ESGSystemEvent)) return;
                    ESGSystemEvent event = (ESGSystemEvent)esgEvent;
                    if(event.getEventType() == ESGSystemEvent.ALL_LOADED) {
                        System.out.println(getName()+" Got All Aboard Signal...");
                        startGeneratingEvents(30);
                    }
                }

                //Doing some work...
                private void startGeneratingEvents(int numOfEvents) {
                    System.out.println("Generating "+numOfEvents+" events to push through system...");
                    ESGEvent testEvent = null;
                    for(int i=0; i < numOfEvents; i++) {
                        testEvent = new ESGEvent(this, "THIS IS MY DATA", "TestEvent #"+i);
                        System.out.println(getName()+" Sending "+testEvent);
                        enqueueESGEvent(testEvent);
                    }
                }
            };
        System.out.println("[OK]");

        System.out.print("making thing2: ");
        ESGSimpleComponent thing2 = new ESGSimpleComponent("THING2");
        System.out.println("[OK]");

        System.out.print("making thing3: ");
        ESGSimpleComponent thing3 = new ESGSimpleComponent("THING3") {
                int count = 0;
                //HERE We show that we can listen for "system" events
                //and based on that decide to add a component to
                //ourselves to route messages to.  In this example we
                //listen for JOIN events and then discern which
                //components have joined and attach to for pushing
                //queued events to. (Notice: No "connect" call from us to
                //thing4 but we can do it ourselves here)
                public void handleESGEvent(ESGEvent esgEvent) {
                    //we only care about join events
                    if(!(esgEvent instanceof ESGJoinEvent)) return;

                    ESGJoinEvent event = (ESGJoinEvent)esgEvent;

                    if(!(event.getJoiner() instanceof ESGSimpleComponent)) return;

                    ESGSimpleComponent component = (ESGSimpleComponent)event.getJoiner();
                    if(event.hasJoined()) {
                        System.out.println(getName()+" Detected That A Simple Component Has Joined: ["+component.getName()+"]");
                        if(component.getName().equals("THING4")) {
                            System.out.println(this.getName()+" says, YES! I have been waiting for you!! ["+component.getName()+"]");
                            //This is what "connect" does under the cover :-)
                            addESGQueueListener(component);
                            //------------------------
                            //Dispatch and do stuff from here...
                            //------------------------
                        }else {
                            System.out.println(getName()+" says, Nah... I Don't want ["+component.getName()+"]");
                        }
                    }else {
                        System.out.println("Detected That Simple Component ["+component.getName()+"] Has Left :-(");
                        removeESGQueueListener(component);
                    }
                    component = null;
                }

                //This is where we deal with message flows through the system.
                public boolean handleESGQueuedEvent(ESGEvent event) {
                    System.out.println(getName()+" one more stop to end...! "+event);
                    super.handleESGQueuedEvent(event);
                    return true;
                }
            };
        System.out.println("[OK]");

        System.out.print("making thing4: ");
        ESGSimpleComponent thing4 = new ESGSimpleComponent("THING4") {
                public boolean handleESGQueuedEvent(ESGEvent event) {
                    System.out.println(getName()+" End of the road... "+event);
                    super.handleESGQueuedEvent(event);
                    return true;
                }
            };
        System.out.println("[OK]");

        //Register components to data node manager...
        System.out.println("Registering things 1-4... ");
        testDnm.registerComponent(thing1);
        testDnm.registerComponent(thing2);
        testDnm.registerComponent(thing3);
        testDnm.registerComponent(thing4);
        System.out.println("[OK]");

        //Connect components together so that they may pass messages...
        System.out.println("Connecting things 1-3...");
        testDnm.connect("THING1","THING2");
        testDnm.connect("THING2","THING3");

        //Broadcast to all that all components have been connected...
        testDnm.sendAllLoadedNotification();
        System.out.println("Test Node Manager has "+testDnm.numOfComponents()+"components registered");
    }

    @Test
    public void testMessagePassing() { }

    @After
    public void tearDown() { }

    @AfterClass
    public static void finalTearDown() { }
    
    
}
