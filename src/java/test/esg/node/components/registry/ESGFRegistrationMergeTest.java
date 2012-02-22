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
package esg.node.components.registry;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

import esg.node.core.*;
import esg.common.generated.registration.*;


/**
   Description:

**/
public class ESGFRegistrationMergeTest {
    private static final Log log = LogFactory.getLog(ESGFRegistrationMergeTest.class);

    private static ESGFRegistry registry = null;
    private static Registration registrationA = null;
    private static Registration registrationB = null;

    public ESGFRegistrationMergeTest() {
        log.trace("Instantiating Test Case for ESGFRegistrationMergeTest");
    }

    //@BeforeClass
    public static void initialSetup() {
        registry = new ESGFRegistry("Test Registry");
        registry.init();
        registrationA = new Registration();
        registrationB = new Registration();
    }

    //@Before
    public void setup() { 
        Node node1 = new Node();
        node1.setHostname("test_host_1");
        node1.setTimeStamp(1L);
        node1.setNamespace("org.esgf");
        node1.setShortName("node_1_host_1");

        Node node2 = new Node();
        node1.setHostname("test_host_1");
        node1.setTimeStamp(2L);
        node1.setNamespace("org.esgf");
        node1.setShortName("node_2_host_1");

        Node node3 = new Node();
        node1.setHostname("test_host_1");
        node1.setTimeStamp(3L);
        node1.setNamespace("org.esgf");
        node1.setShortName("node_3_host_1");

        Node node4 = new Node();
        node1.setHostname("test_host_1");
        node1.setTimeStamp(4L);
        node1.setNamespace("org.esgf");
        node1.setShortName("node_4_host_1");

        Node node5 = new Node();
        node1.setHostname("test_host_1");
        node1.setTimeStamp(5L);
        node1.setNamespace("org.esgf");
        node1.setShortName("node_5_host_1");

        registrationA.getNode().add(node1);
        registrationA.getNode().add(node3);
        registrationA.getNode().add(node5);

        registrationB.getNode().add(node2);
        registrationA.getNode().add(node4);
    }

    @Ignore
    public void testRegistrationMerging() {
        Set<Node> updatedNodes = registry.mergeNodes(registrationA,registrationB);
        
    }

    //@After
    public void tearDown() { }

    //@AfterClass
    public static void finalTearDown() {
        //gc niceness...
        registry = null;
        registrationA = null;
        registrationB = null;
    }
}
