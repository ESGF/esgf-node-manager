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

//Just being explicit ;-)
import java.lang.Exception;

public class ESGException extends Exception {
    public ESGException() { }
    public ESGException(String message) { super(message); }
    public ESGException(String message, Throwable cause) { super(message,cause); }
    public ESGException(Throwable cause) { super(cause); }
}
