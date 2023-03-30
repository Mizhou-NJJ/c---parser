package zhou.parser;

import java.util.ArrayList;
import java.util.List;

public class SymbolTable {
    public SymbolTable parent;
    public SymbolTable next;
    public boolean isInit = false;
    public String tableName;
    public int offset;
    //返回类型
    public String retType;
    //入口标号
    public String label;
    //符号列表
    public List<Symbol> items;
    public int paramCount;

    public SymbolTable(){
        this.offset = 0;
        items = new ArrayList<>();
    }
    public void add(Symbol symbol){
        items.add(symbol);
    }

}
