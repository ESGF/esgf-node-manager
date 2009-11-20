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

   A simple main program to give when making the jar so folks can tell
   the version of the code by running it. (why not)

*****************************************************************/
package esg.node.core;

public class DataNodeMain {
    public static final String VERSION = "0.0.1";
    public static final String RELEASE = "Flatbush";
    public static void main(String[] args) {
	System.out.println("ESG Data Node: "+RELEASE+" Release v"+VERSION);
    }
}