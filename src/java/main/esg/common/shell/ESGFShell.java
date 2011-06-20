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
package esg.common.shell;

/**
   Description:
   Top level class for ESGF Shell implementation...
**/

import jline.*;

import java.io.*;
import java.util.*;

import esg.common.ESGException;
import esg.common.ESGRuntimeException;
import esg.common.shell.cmds.*;
import esg.common.util.ESGFProperties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.*;

public class ESGFShell {

    private static Log log = LogFactory.getLog(ESGFShell.class);

    public static final Character mask = '*';
    public static final String pipeRe = "\\|";
    public static final String semiRe = ";";

    private Map<String,ESGFCommand> commandMap = null;
    private List completors = null;

    public ESGFShell(ESGFEnv env) {
        loadCommands();

        completors = new LinkedList();
        completors.add(new SimpleCompletor(commandMap.keySet().toArray(new String[]{})));
        env.getReader().addCompletor(new ArgumentCompletor(completors));
    }

    /**
       This is where we look through the command list and load up the
       commands made available by this shell
    */
    private void loadCommands() {
        log.info("Loading ESGF Builtin Shell Commands...");
        commandMap = new HashMap<String,ESGFCommand>();
        commandMap.put("test",new esg.common.shell.cmds.ESGFtest());
        commandMap.put("clear",new esg.common.shell.cmds.ESGFclear());
        commandMap.put("ls",new esg.common.shell.cmds.ESGFls());
        commandMap.put("realize",new esg.common.shell.cmds.ESGFrealize());
        log.info("("+commandMap.size()+") commands loaded");
    }

    public static void usage() {
        System.out.println("Usage: java " + ESGFShell.class.getName()+" yadda yadda yadda");
    }

    private void eval(String[] commands, ESGFEnv env) throws ESGException, IOException {

        if (commands[0].equalsIgnoreCase("quit") || commands[0].equalsIgnoreCase("exit")) {
            throw new ESGException("exit shell");
        }

        for(String commandLine : commands) {
            String[] commandLineParts = commandLine.trim().split(" ",2);
            String commandName = commandLineParts[0].trim();
            if((commandName == null) || (commandName.equals(""))) continue;
            
            log.trace("======> command ["+commandName+"] ");

            ESGFCommand command = commandMap.get(commandName);
            if(null == command) {
                env.getWriter().println(commandName+": command not found :-(");
                continue;
            }

            String[] args = null;
            if(commandLineParts.length == 2) {
                args = commandLineParts[1].trim().split("\\s");
                
                if(log.isTraceEnabled()) {
                    log.trace("args ("+args.length+") <");
                    for(String arg : args) {
                        log.trace("["+arg+"] ");
                    }
                    log.trace(">");
                }
            }
            command.eval(args,env);
        }
        env.getWriter().flush();

        if (commands[0].compareTo("rehash") == 0) {
            loadCommands();
        }
       
        //-------------------------Misc-------------------------
        if (commands[0].compareTo("su") == 0) {
            commands[0] = env.getReader().readLine("password> ", mask);
        }
        //------------------------------------------------------
        
    }

    public static void main(String[] args) throws IOException {
        if ( (args.length > 0) && (args[0].equals("--help")) ) {
            usage();
            return;
        }

        ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);
        reader.setDebug(new PrintWriter(new FileWriter("writer.debug", true)));                

        PrintWriter writer = new PrintWriter(System.out);
        ESGFProperties esgfProperties = null;
        try{
            esgfProperties = new ESGFProperties();
        }catch (Throwable t) {
            System.out.println(t.getMessage());
        }
        ESGFEnv env = new ESGFEnv(reader,writer,esgfProperties);
        ESGFShell shell = new ESGFShell(env);
        String prompt = System.getProperty("user.name")+"@esgf-sh> ";
        String line = null;

        while ((line = reader.readLine(prompt)) != null) {
            try{
                shell.eval(line.split(semiRe),env);
            }catch(Throwable t) {
                System.out.println(t.getMessage());
                break;
            }
        }
    }
}

