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
   ESGF's "ls" command..."
**/

import esg.common.shell.*;

import org.apache.commons.cli.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

public class ESGFls extends ESGFCommand {

private static Log log = LogFactory.getLog(ESGFls.class);

    public ESGFls() {
        super();
        getOptions().addOption("d", "datasets", false, "lists All Datasets in CWD");
        Option listfiles   = OptionBuilder.withArgName("datasetdir")
            .hasArg()
            .withDescription("lists the files of a particular dataset")
            .create("files");
        getOptions().addOption(listfiles);
        Option missingfiles   = OptionBuilder.withArgName("datasetdir")
            .hasArg()
            .withDescription("lists the missing files of a particular dataset")
            .create("missing");
        getOptions().addOption(missingfiles);
        Option localfiles   = OptionBuilder.withArgName("datasetdir")
            .hasArg()
            .withDescription("lists the files of a particular dataset")
            .create("local");
        getOptions().addOption(localfiles);
    }

    public String getCommandName() { return "ls"; }

    public ESGFEnv doEval(CommandLine line, ESGFEnv env) {
        log.trace("inside the \"ls\" command's doEval");
        //TODO: Query for options and perform execution logic

        if(line.hasOption("datasets")) {
            env.getWriter().println("Scanning for datasets... :-)");
        }

        String datasetdir = null;
        if(line.hasOption( "files" )) {
            datasetdir = line.getOptionValue( "files" );
            env.getWriter().println("files option value is: "+datasetdir);
        }

        if(line.hasOption( "missing" )) {
            datasetdir = line.getOptionValue( "missing" );
            env.getWriter().println("missing option value is: "+datasetdir);
        }

        if(line.hasOption( "local" )) {
            datasetdir = line.getOptionValue( "local" );
            env.getWriter().println("local option value is: "+datasetdir);
        }

        int i=0;
        for(String arg : line.getArgs()) {
            log.trace("arg("+(i++)+"): "+arg);
        }
        
        return env;
    }
}
