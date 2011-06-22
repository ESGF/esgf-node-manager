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

public class ESGFusermod extends ESGFCommand {

private static Log log = LogFactory.getLog(ESGFusermod.class);

    public ESGFusermod() {
        super();
        Option firstname = 
            OptionBuilder.withArgName("firstname")
            .hasArg(true)
            .withDescription("First name of user")
            .withLongOpt("firstname")
            .create("fn");
        getOptions().addOption(firstname);

        Option middlename = 
            OptionBuilder.withArgName("middlename")
            .hasArg(true)
            .withDescription("Middle name of user")
            .withLongOpt("middlename")
            .create("mn");
        getOptions().addOption(middlename);

        Option lastname = 
            OptionBuilder.withArgName("lastname")
            .hasArg(true)
            .withDescription("Last name of user")
            .withLongOpt("lastname")
            .create("ln");
        getOptions().addOption(lastname);

        Option email = 
            OptionBuilder.withArgName("email")
            .hasArg(true)
            .withDescription("Email address of user")
            .withLongOpt("email")
            .create("e");
        getOptions().addOption(email);

        Option organization = 
            OptionBuilder.withArgName("organization")
            .hasArg(true)
            .withDescription("Organization name of user")
            .withLongOpt("organization")
            .create("o");
        getOptions().addOption(organization);

        Option city = 
            OptionBuilder.withArgName("city")
            .hasArg(true)
            .withDescription("City of user")
            .withLongOpt("city")
            .create("c");
        getOptions().addOption(city);

        Option state = 
            OptionBuilder.withArgName("state")
            .hasArgs()
            .withDescription("State of user")
            .withLongOpt("state")
            .create("st");
        getOptions().addOption(state);

        Option country = 
            OptionBuilder.withArgName("country")
            .hasArg(true)
            .withDescription("First name of user")
            .withLongOpt("country")
            .create("cn");
        getOptions().addOption(country);

        Option openid = 
            OptionBuilder.withArgName("openid")
            .hasArg(true)
            .withDescription("OpenID of user")
            .withLongOpt("openid")
            .create("oid");
        getOptions().addOption(openid);
    }

    public String getCommandName() { return "usermod"; }

    public ESGFEnv doEval(CommandLine line, ESGFEnv env) {
        log.trace("inside the \"usermod\" command's doEval");
        //TODO: Query for options and perform execution logic

        String firstname = null;
        if(line.hasOption( "fn" )) {
            firstname = line.getOptionValue( "fn" );
            env.getWriter().println("firstname: ["+firstname+"]");
        }

        String middlename = null;
        if(line.hasOption( "mn" )) {
            middlename = line.getOptionValue( "mn" );
            env.getWriter().println("middlename: ["+middlename+"]");
        }

        String lastname = null;
        if(line.hasOption( "ln" )) {
            lastname = line.getOptionValue( "ln" );
            env.getWriter().println("lastname: ["+lastname+"]");
        }

        String email = null;
        if(line.hasOption( "e" )) {
            email = line.getOptionValue( "e" );
            env.getWriter().println("email: ["+email+"]");
        }

        String organization = null;
        if(line.hasOption( "o" )) {
            organization = line.getOptionValue( "o" );
            env.getWriter().println("organization: ["+organization+"]");
        }

        String city = null;
        if(line.hasOption( "c" )) {
            city = line.getOptionValue( "c" );
            env.getWriter().println("city: ["+city+"]");
        }

        String state = null;
        if(line.hasOption( "st" )) {
            state = line.getOptionValue( "st" );
            env.getWriter().println("state: ["+state+"]");
        }

        String country = null;
        if(line.hasOption( "cn" )) {
            country = line.getOptionValue( "cn" );
            env.getWriter().println("country: ["+country+"]");
        }

        String openid = null;
        if(line.hasOption( "oid" )) {
            openid = line.getOptionValue( "oid" );
            env.getWriter().println("openid: ["+openid+"]");
        }

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

        String username = null;
        if(args.length > 0) {
            username = args[0];
            env.getWriter().println("user to create is: ["+username+"]");
        }
        //------------------
        //NOW DO SOME LOGIC
        //------------------

        

        //------------------
        return env;
    }
}
