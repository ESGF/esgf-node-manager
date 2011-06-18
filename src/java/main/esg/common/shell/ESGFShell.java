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
import java.util.zip.*;

public class ESGFShell {
    public static void usage() {
        System.out.println("Usage: java " + ESGFShell.class.getName()
                + " [none/simple/files/dictionary [trigger mask]]");
        System.out.println("  none - no completors");
        System.out.println("  simple - a simple completor that comples "
                + "\"foo\", \"bar\", and \"baz\"");
        System.out
                .println("  files - a completor that comples " + "file names");
        System.out.println("  dictionary - a completor that comples "
                + "english dictionary words");
        System.out.println("  classes - a completor that comples "
                + "java class names");
        System.out
                .println("  trigger - a special word which causes it to assume "
                        + "the next line is a password");
        System.out.println("  mask - is the character to print in place of "
                + "the actual password character");
        System.out.println("\n  E.g - java Example simple su '*'\n"
                + "will use the simple compleator with 'su' triggering\n"
                + "the use of '*' as a password mask.");
    }

    public static void main(String[] args) throws IOException {
        Character mask = null;
        String trigger = null;

        ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);
        reader.setDebug(new PrintWriter(new FileWriter("writer.debug", true)));

        if ((args == null) || (args.length == 0)) {
            usage();

            return;
        }

        List completors = new LinkedList();

        if (args.length > 0) {
            if (args[0].equals("none")) {
            } else if (args[0].equals("files")) {
                completors.add(new FileNameCompletor());
            } else if (args[0].equals("classes")) {
                completors.add(new ClassNameCompletor());
            } else if (args[0].equals("dictionary")) {
                completors.add(new SimpleCompletor(new GZIPInputStream(
                        ESGFShell.class.getResourceAsStream("english.gz"))));
            } else if (args[0].equals("simple")) {
                completors.add(new SimpleCompletor(new String[] { "foo", "bar",
                        "baz" }));
            } else {
                usage();

                return;
            }
        }

        if (args.length == 3) {
            mask = new Character(args[2].charAt(0));
            trigger = args[1];
        }

        reader.addCompletor(new ArgumentCompletor(completors));

        String line;
        PrintWriter out = new PrintWriter(System.out);

        while ((line = reader.readLine("esgfsh> ")) != null) {
            out.println("======>\"" + line + "\"");
            out.flush();

            // If we input the special word then we will mask
            // the next line.
            if ((trigger != null) && (line.compareTo(trigger) == 0)) {
                line = reader.readLine("password> ", mask);
            }
            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                break;
            }
        }
    }
}

