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
package esg.node.filters;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
   Description:
   JUnit test code for UrlResolvingDAO
*/
public class TestUrlResolvingDAO {
    
    private static final Log log = LogFactory.getLog(TestUrlResolvingDAO.class);
    private static UrlResolvingDAO  resolver = null;
    
    /**
       Initialize *ONCE* static DAO instance...
    */
    @BeforeClass    
    public static void initTest() {
        resolver = new UrlResolvingDAO(); //No database involved
    }
    

    @Test
    public void testUrls() {
        String[] inputs = {"http://fauxserver.llnl.gov/fileService/root/one/two/three/four/five/six/seven/eight/nine/ten/eleven",
                           "http://fauxserver.llnl.gov/fauxService?one=1&two=2&three=3&four=4&five=5&six=6&seven=7&eight=8&nine=9&ten=10&eleven=11",
                           "one/two/three/four/five/six/seven/eight/nine/ten/eleven",
                           "/one/two/three/four/five/six/seven/eight/nine/ten/eleven"};
        String output = null;
        for(String input : inputs) {
            log.info(input+" -> "+(output = resolver.resolve(input)));
            assertTrue(output.equals());
        }
    }

    @Test
    public void testBadUrls() {
        String[] inputs = {"http://fauxserver.llnl.gov/fileService/root/one/../two/three/four/five/six/seven/eight/nine/ten/eleven",
                           "http://fauxserver.llnl.gov/fauxService?one=1&?two=2&three=3&four=4&five=5&six=6&seven=7&eight=8&nine=9&ten=10&eleven=11",
                           "one/two/three/four/five/six/seven/eight/nine/ten/",
                           "/one/two/../three/four/five/six/seven/eight/nine/ten/eleven"};
        String output = null;
        for(String input : inputs) {
            log.info(input+" -> "+(output = resolver.resolve(input)));
            assertTrue(output.equals());
        }
    }

    @Test
    public void testPathUrls() {
        String[] inputs = {"http://one/two/three/four/five/six/seven/eight/nine/ten,eleven"};
        String output = null;
        for(String input : inputs) {
            log.info(input+" -> "+(output = resolver.resolveDRSUrl(input)));
            assertTrue(output.equals());
        }
    }

    @Test
    public void testPaths() {
        String[] inputs = {"one/two/three/four/five/six/seven/eight/nine/ten/eleven"};
        String output = null;
        for(String input : inputs) {
            log.info(input+" -> "+(output = resolver.resolveDRSPath()));
            assertTrue(output.equals());
            
        }

    }

    @Test
    public void testUrlQueries(String urlQuery) {
        String[] inputs = {"?one=1&two=2&three=3&four=4&five=5&six=6&seven=7&eight=8&nine=9&ten=10&eleven=11"};
        String output = null;
        for(String input : inputs) {
            log.info(input+" -> "+(output = resolver.resolveDRSQuery(input)));
            assertTrue(output.equals());
        }
        
    }
    
    @AfterClass
        public static void finalTearDown() {
        resolver = null;
    }
}