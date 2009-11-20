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
package esg.node.core;

public interface DataNodeComponent extends ESGListener{
    
    public void init();
    public String getName();
    public DataNodeManager getDataNodeManager();
    public void unregister();
    public void addESGListener(ESGListener listner);
    public void removeESGListener(ESGListener listener);
    
}
