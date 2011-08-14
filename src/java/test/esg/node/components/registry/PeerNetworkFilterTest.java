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
package esg.node.components.registry;

import java.util.Properties;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

/**
   Description:
   Test the PeerNetworkFilter code...
**/
public class PeerNetworkFilterTest {

    private static final Log log = LogFactory.getLog(PeerNetworkFilterTest.class);

    public PeerNetworkFilterTest() {
        log.trace("Instantiating Test Case for PeerNetworkFilterTest");
    }

    @Test
    public void testVariousPeerNetworkEntries() {
        String[] goodCandidates = {"org.esgf",
                                   "org.esgf.test",
                                   "org.esgf/test",
                                   "org.esgf:test",
                                   "org.esgf|test",
                                   "org.esgf,test",
                                   "org.esgf test",
                                   "gov.llnl"};
        
        String[] badCandidates = {"test-org.esgf",
                                  "test-gov.llnl",
                                  "foo",
                                  "org.esg",
                                  "gov.llnlgroup1",
                                  "gov.lln-group1, org.esggroup1",
                                  "gov.lln",
                                  "gov.lln.group1"};
        
        String networkNames = "org.esgf, gov.llnl";
        
        Properties p = new Properties();
        p.setProperty("node.namespace",networkNames);
        System.out.println("Network Names = "+p);

        PeerNetworkFilter pnf = new PeerNetworkFilter(p);
        boolean result = false;
        System.out.println("\nThe Good:");
        for(String candidate : goodCandidates) {
            result = pnf.isInNetwork(candidate);
            System.out.println(candidate+" -> ["+result+"]");
            assertTrue(result);
        }
        System.out.println("\nThe Bad:");
        for(String candidate : badCandidates) {
            result = pnf.isInNetwork(candidate);
            System.out.println(candidate+" -> ["+result+"]");
            assertFalse(result);
        }
    }
}