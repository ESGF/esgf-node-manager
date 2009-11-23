/*****************************************************************

  Organization: Lawrence Livermore National Lab (LLNL)
   Directorate: Computation
    Department: Computing Applications and Research
      Division: S&T Global Security
        Matrix: Atmospheric, Earth and Energy Division
       Program: PCMDI
       Project: Earth Systems Grid
  First Author: Gavin M. Bell (gavin@llnl.gov)

   Description:

*****************************************************************/
package esg.common;

import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    private static AtomicLong msgCounter = new AtomicLong(0);

    public static long nextSeq() { return msgCounter.getAndIncrement(); }

}