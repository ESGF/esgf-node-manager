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
*   For details, see http://esg-repo.llnl.gov/esg-node/                    *
*   Please also read this link                                             *
*    http://esg-repo.llnl.gov/LICENSE                                      *
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
   Description:

   The basic idea behind this class is to have a class that can read
   in data and then quickly rescan that data over and over again in
   the most efficient way possible.  In the context of this package
   the idea is the read the /proc pseudo-filesystem over and over
   again to pull out bits of system information.  The key here is to
   be optimal with resource usage.  So once resources are set up they
   stay open and allocated for the lifetime of this class or until
   close() is explicitly called.  Since we take care to read the proc
   file in question in one big gulp, it minimizes kernel effort in
   creating the proc pseud-files.  Also we keep the buffers open and
   we allocate them only once.

   Also note that the files in the /proc pseudo-filesystem are of 0
   size because they are all memory mapped files generated by the
   kernel on demand whenever someone looks at them so it is hard to
   pre-allocate the proper size to read in all the data presented in
   one big "gulp" (Ex /proc/meminfo is about 771 bytes but an ls will
   show it as 0).  The default size is 1K... We will see.  

   (caveat) The sub optimal things... Hmmm... well the encoding from
   bytes to chars is something that takes time and I am not sure if we
   need to do this.  Also note that since we are always attempting to
   allocate memory to fit the entire file in one sitting, this code is
   ont suitable for LARGE FILES!!!! unless you want to run out of
   memory ;-)

   Keep the scope of this class' instance larger than that of the call
   to scan() to get the benefit.

**/
package esg.node.components.monitoring;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.channels.FileChannel;

//Note: this class is package scope on purpose.
class InfoResources {
    RandomAccessFile raf;
    FileChannel fc;
    ByteBuffer bb;
    CharBuffer cb;
    CharsetDecoder decoder;
    
    int guestimateSize = 1024;
    String filename = null;
    
    InfoResources(String filename) { this(filename,-1); }

    InfoResources(String filename,int buffSize) {
	this.filename = filename;
	System.out.println("InfoResource initializing for "+filename);
	try{
	    File procFile = new File(filename);
	    if(buffSize < 0) {
		buffSize = (int)procFile.length();
		if(buffSize == 0) { buffSize = guestimateSize; }
	    }
	    raf = new RandomAccessFile(procFile, "r");
	    fc = raf.getChannel();
	    bb = ByteBuffer.allocateDirect(buffSize);
	    
	    Charset cs = Charset.forName("8859_1");
	    decoder = cs.newDecoder();
	    cb = CharBuffer.allocate(buffSize);
	    
	    System.out.println("Buffer Size is "+buffSize+" number of bytes");
	}catch(Exception e) {
	    e.printStackTrace();
	}
    }
    
    CharBuffer scan() {
	try {
	    int totalBytesRead = 0;
	    int bytesRead = 0;
	    //int iteration = 0;
	    raf.seek(0);
	    while((bytesRead = fc.read(bb)) != -1) {
		bb.flip();
		decoder.decode(bb,cb,true);
		cb.flip();
		//System.out.print(cb);
		cb.clear();
		bb.clear();
		totalBytesRead += bytesRead;
		//System.out.println("Iteration: ["+(++iteration)+"]");
	    }
	    
	    //System.out.println("Total number of bytes read: "+totalBytesRead);
	    
	}catch(Exception e) {
	    e.printStackTrace();
	}
	return cb;
    }
    
    void close() {
	System.out.println("InfoResources for "+filename+" closing...");
	try{
	    raf.close();
	    fc.close();
	    bb.clear();
	    cb.clear();
	}catch(Exception e) {
	    e.printStackTrace();
	}
    }
}
