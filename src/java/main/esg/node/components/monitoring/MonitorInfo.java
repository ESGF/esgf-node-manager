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
   Description: This is solely a data aggregation / encapsulation
   object.  It is kept as simple as possible because it may be put on
   the wire and we want the serialization to be as simple as possible.
   
**/
package esg.node.components.monitoring;

import java.util.List;
import java.util.Map;
import java.io.Serializable;

public class MonitorInfo implements Serializable {

    //---------
    //MAP KEYS:
    //---------

    //Disk Info
    public static final String TOTAL_SPACE = "TOTAL_SPACE";
    public static final String FREE_SPACE  = "FREE_SPACE";
    public static final String USED_SPACE  = "USED_SPACE";
    //Memory Info
    public static final String TOTAL_MEMORY = "TOTAL_MEMORY";
    public static final String FREE_MEMORY  = "FREE_MEMORY";
    public static final String USED_MEMORY  = "USED_MEMORY";
    public static final String TOTAL_SWAP   = "TOTAL_SWAP";
    public static final String FREE_SWAP    = "FREE_SWAP";
    public static final String USED_SWAP    = "USED_SWAP";
    //CPU Info
    public static final String CORES       = "CORES";
    public static final String CLOCK_SPEED = "CLOCK_SPEED";
    //UPTIME
    public static final String HOST_UPTIME = "HOST_UPTIME";
    public static final String DNM_UPTIME  = "DNM_UPTIME";
    public static final String LOAD_AVG1 = "LOAD_AVG1";
    public static final String LOAD_AVG2 = "LOAD_AVG2";
    public static final String LOAD_AVG3 = "LOAD_AVG3";
    //XFER Info
    public static final String XFER_AVG = "XFER_AVG";

    //---------
    //MAPS
    //---------
    public Map<String,Map<String,String>> diskInfo = null;
    public Map<String,String> memInfo = null;
    public Map<String,String> cpuInfo = null; 
    public Map<String,String> uptimeInfo = null; 
    public Map<String,String> xferInfo = null;
    public String[] componentList = null;

}