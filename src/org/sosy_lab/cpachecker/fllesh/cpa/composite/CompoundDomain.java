package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;

public class CompoundDomain implements AbstractDomain {

  private CompoundJoinOperator mJoinOperator;
  private CompoundPartialOrder mPartialOrder;
  
  public CompoundDomain(List<AbstractDomain> pSubdomains) {
    
    List<JoinOperator> lJoinOperators = new ArrayList<JoinOperator>(pSubdomains.size());
    List<PartialOrder> lPartialOrders = new ArrayList<PartialOrder>(pSubdomains.size());
    
    for (AbstractDomain lSubdomain : pSubdomains) {
      lJoinOperators.add(lSubdomain.getJoinOperator());
      lPartialOrders.add(lSubdomain.getPartialOrder());
    }
    
    mJoinOperator = new CompoundJoinOperator(lJoinOperators);
    mPartialOrder = new CompoundPartialOrder(lPartialOrders);
    
  }

  @Override
  public JoinOperator getJoinOperator() {
    return mJoinOperator;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return mPartialOrder;
  }
}
