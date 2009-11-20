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

public interface DataNodeManager extends ESGListener {
    
    public boolean registerComponent(DataNodeComponent component);
    public void removeComponent(String componentName);
    public void removeComponent(DataNodeComponent component);
    public int numOfComponents();
    public String[] getComponentNames();
    public boolean registerGateway(Gateway gateway);
    public void removeGateway(String gatewayName);
    public void removeGateway(Gateway gateway);
    public int numOfGateways();
    public String[] getGatewayNames();
    
}
