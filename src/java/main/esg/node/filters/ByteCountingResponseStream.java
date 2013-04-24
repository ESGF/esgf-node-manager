/***************************************************************************
*                                                                          *
*  Organization: Earth System Grid Federation                              *
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
*   For details, see http://esgf.org/                                      *
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

/**
   Description:
   Simple Stream implementation with callback listener support to report total number of bytes passed.
**/
package esg.node.filters;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;


/**
 * Implementation of <b>ServletOutputStream</b>
 * @author Gavin M. Bell
 */

public class ByteCountingResponseStream extends ServletOutputStream {
    
    private ByteCountListener byteCountListener = null;

    private int debug = 0;
    protected int buffSize = 0;
    protected byte[] buffer = null;
    protected int bufferCount = 0;
    protected long totalBytes = 0;

    /**
     * Construct a servlet output stream associated with the specified Response.
     *
     * @param response The associated response
     */
    public ByteCountingResponseStream(HttpServletResponse response, ByteCountListener byteCountListener) throws IOException{
        super();
        closed = false;
        this.response = response;
        this.output = response.getOutputStream();
        this.byteCountListener = byteCountListener;
    }

    protected boolean closed = false;

    /**
     * The content length past which we will not write, or -1 if there is
     * no defined content length.
     */
    protected int length = -1;
    protected HttpServletResponse response = null;
    protected ServletOutputStream output = null;

    public void setDebugLevel(int debug) { this.debug = debug; }


    /**
     * Set the buffSize number and create buffer for this size
     */
    protected void setBuffer(int _buffSize) {
        this.buffSize = _buffSize;
        buffer = new byte[buffSize];
        if (debug > 1) { System.out.println("buffer is set to "+buffSize); }
    }

    /**
     * Close this output stream, causing any buffered data to be flushed and
     * any further output data to throw an IOException.
     */
    public void close() throws IOException {

        if (debug > 1) { System.out.println("close() @ ByteCountingResponseStream"); }
        if (closed)
            throw new IOException("This output stream has already been closed");

        if (bufferCount > 0) {
            if (debug > 2) {
                System.out.print("output.write(");
                System.out.write(buffer, 0, bufferCount);
                System.out.println(")");
            }
            output.write(buffer, 0, bufferCount);
            bufferCount = 0;
        }
        
        output.close();
        closed = true;
        if(byteCountListener != null) byteCountListener.setByteCount(totalBytes);
    }


    /**
     * Flush any buffered data for this output stream, which also causes the
     * response to be committed.
     */
    public void flush() throws IOException {

        if (debug > 1) {
            System.out.println("flush() @ ByteCountingResponseStream");
        }
        if (closed) {
            throw new IOException("Cannot flush a closed output stream");
        }

        if (bufferCount > 0) {
            if (debug > 1) {
                System.out.println("flushing out to stream, bufferCount = " + bufferCount);
            }
            write(buffer, 0, bufferCount);
            bufferCount = 0;
        }
    }

    /**
     * Write the specified byte to our output stream.
     *
     * @param b The byte to be written
     *
     * @exception IOException if an input/output error occurs
     */
    public void write(int b) throws IOException {

        if (debug > 1) {
            System.out.println("write "+b+" in ByteCountingResponseStream ");
        }
        if (closed)
            throw new IOException("Cannot write to a closed output stream");

        if (bufferCount >= buffer.length) {
            flush();
        }

        buffer[bufferCount++] = (byte) b;
        totalBytes++;
    }


    /**
     * Write <code>b.length</code> bytes from the specified byte array
     * to our output stream.
     *
     * @param b The byte array to be written
     *
     * @exception IOException if an input/output error occurs
     */
    public void write(byte b[]) throws IOException {

        write(b, 0, b.length);

    }


    /**
     * Write <code>len</code> bytes from the specified byte array, starting
     * at the specified offset, to our output stream.
     *
     * @param b The byte array containing the bytes to be written
     * @param off Zero-relative starting offset of the bytes to be written
     * @param len The number of bytes to be written
     *
     * @exception IOException if an input/output error occurs
     */
    public void write(byte b[], int off, int len) throws IOException {

        if (debug > 1) {
            System.out.println("write, bufferCount = " + bufferCount + " len = " + len + " off = " + off);
        }
        if (debug > 2) {
            System.out.print("write(");
            System.out.write(b, off, len);
            System.out.println(")");
        }

        if (closed)
            throw new IOException("Cannot write to a closed output stream");

        if (len == 0)
            return;

        // Can we write into buffer ?
        if (len <= (buffer.length - bufferCount)) {
            System.arraycopy(b, off, buffer, bufferCount, len);
            bufferCount += len;
            totalBytes += bufferCount;
            return;
        }

        // There is not enough space in buffer. Flush it ...
        flush();

        // ... and try again. Note, that bufferCount = 0 here !
        if (len <= (buffer.length - bufferCount)) {
            System.arraycopy(b, off, buffer, bufferCount, len);
            bufferCount += len;
            totalBytes += bufferCount;
            return;
        }

        // write direct to output stream
        output.write(b, off, len);
    }

    /**
     * Has this response stream been closed?
     */
    public boolean closed() {
        return (this.closed);
    }
}
