package zhou.cfg;

import java.util.HashSet;

public class ProductionItem {
   //产生式左部
    public BaseSymbol left;
    //产生式右部
    public BaseSymbol right;
    //此产生式左部的select集
    public HashSet<BaseSymbol> select;
    public int index;
    public ProductionItem(){
     select = new HashSet<>();
    }

}
