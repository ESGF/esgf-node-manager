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
package esg.common.shell.cmds;

/**
   Description:
   ESGF's "realize" command..."

   This command takes a dataset directory and inspects its catalog to
   find missing files (files listed in the dataset catalog but not
   locally present on the filesystem) and brings them local. The
   second half of the 'replication' process - for a single dataset.
**/

import esg.common.shell.*;

import org.apache.commons.cli.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

public class ESGFdel_user_from_group extends ESGFCommand {

private static Log log = LogFactory.getLog(ESGFdel_user_from_group.class);

    public ESGFdel_user_from_group() {
        super();
        init();
    }
    
    private void init() {
        Option user = 
            OptionBuilder.withArgName("user")
            .hasArg(true)
            .withDescription("User to inspect")
            .withLongOpt("user")
            .create("u");
        getOptions().addOption(user);
        
        Option all_group = new Option("ag", "all-groups", false, "del_user_from_group all groups on the system");
        getOptions().addOption(all_group);
        
        Option group = 
            OptionBuilder.withArgName("group")
            .hasArg(true)
            .withDescription("Group to inspect")
            .withLongOpt("group")
            .create("g");
        getOptions().addOption(group);
    }
    
    public String getCommandName() { return "del_user_from_group"; }
    
    public ESGFEnv doEval(CommandLine line, ESGFEnv env) {
        log.trace("inside the \"del_user_from_group\" command's doEval");
        //TODO: Query for options and perform execution logic
        
        String user = null;
        if(line.hasOption( "u" )) {
            user = line.getOptionValue( "u" );
            env.getWriter().println("user: ["+user+"]");
        }
        
        //semantic sanity checking....
        if(user == null) {
            env.getWriter().println("user: ["+user+"]");            
            return env;
        }
        
        //target group info...

        boolean all_groups = false;
        if(line.hasOption( "all-groups" )) { all_groups = true; }
        env.getWriter().println("all-groups: ["+all_groups+"]");


        String group = null;
        if(line.hasOption( "g" )) {
            group = line.getOptionValue( "g" );
            env.getWriter().println("group: ["+group+"]");
        }

        //semantic sanity checking....
        //Cannot have them both set...
        if(all_groups && (group != null)) {
            env.getWriter().println("Semantic Group Problem");
            return env;
        }
        
        if(all_groups) {
            //------------------
            //NOW DO SOME LOGIC (all logic)
            //------------------
            
            env.getWriter().println("doing all logic: ["+all_groups+"]");
            
            
            //------------------
        } else {
            //------------------
            //NOW DO SOME LOGIC (per user or group)
            //------------------
            
            env.getWriter().println("doing some logic: ["+all_groups+"]");
            
            
            //------------------
        }
        init();
        return env;
    }
}
