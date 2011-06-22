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

public class ESGFgroupmod extends ESGFCommand {

private static Log log = LogFactory.getLog(ESGFgroupmod.class);

    public ESGFgroupmod() {
        super();
        getOptions().addOption("auto", "auto-approve", false, "Set auto approval for joining this group");
        getOptions().addOption("no_auto", "no-auto-approve", false, "Set auto approval for joining this group");

        getOptions().addOption("vis", "visible", false, "Sets whether this group is visible to registry");
        getOptions().addOption("no_vis", "not-visible", false, "Sets whether this group is visible to registry");

        Option description = 
            OptionBuilder.withArgName("description")
            .hasArgs()
            .withDescription("Description of group")
            .withLongOpt("description")
            .create("d");
        getOptions().addOption(description);

    }

    public String getCommandName() { return "groupmod"; }

    public ESGFEnv doEval(CommandLine line, ESGFEnv env) {
        log.trace("inside the \"groupmod\" command's doEval");
        //TODO: Query for options and perform execution logic

        String description = null;
        if(line.hasOption( "d" )) {
            description = line.getOptionValue( "d" );
            env.getWriter().println("description: ["+description+"]");
        }
        
        boolean autoapprove = true;
        if(line.hasOption( "auto" )) { autoapprove = true; }
        if(line.hasOption( "no_auto" )) { autoapprove = false; }
        env.getWriter().println("auto approval: ["+autoapprove+"]");

        boolean visible = true; //default
        if(line.hasOption( "vis" )) { visible = true; }
        if(line.hasOption( "no_vis" )) { visible = false; }
        env.getWriter().println("visible: ["+visible+"]");

        
        int i=0;
        for(String arg : line.getArgs()) {
            log.info("arg("+(i++)+"): "+arg);
        }
        
        //Scrubbing... (need to go into cli code and toss in some regex's to clean this type of shit up)
        java.util.List<String> argsList = new java.util.ArrayList<String>();
        String[] args = null;
        for(String arg : line.getArgs()) {
            if(!arg.isEmpty()) {
                argsList.add(arg);
            }
        }
        args = argsList.toArray(new String[]{});

        String groupname = null;
        if(args.length > 0) {
            groupname = args[0];
            env.getWriter().println("group to create is: ["+groupname+"]");
        }
        
        //if(groupname == null) throw new ESGFParseException("no group name specified");
        //------------------
        //NOW DO SOME LOGIC
        //------------------
        
        

        //------------------
        return env;
    }
}
