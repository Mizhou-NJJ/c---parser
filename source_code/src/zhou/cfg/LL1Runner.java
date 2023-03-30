package zhou.cfg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * LL1Runner用来解析文法，它将每条产生式规则解析为 一个ProductItem对象，
 * 需要注意的是，如A->int|double的规则，会被解析为连个ProductItem对象，
 * A->int 和 A->double
 * 并利用ProductItems来求改文法的Frist集合、Follow集合 以及Select集合
 * Select集合将用与后续递归下降分析的预测分析表
 */
public class LL1Runner {

    private List<ProductionItem> items;
    private Hashtable<String, HashSet<String>> tableFirst;
    private Hashtable<String,HashSet<String>> tableFollow;
    private HashSet<String> setOfNullable;
    private String cfgFilePath;

    /**
     *
     * @param cfgFilePath 文法文件路径
     */
    public LL1Runner(String cfgFilePath){
        items = new ArrayList<>();
        this.cfgFilePath = cfgFilePath;
    }

    private HashSet<String> nts;
    //读取cfg文件
    public ArrayList<String> readCfg(String cfgFilePath){
        // 要读取的文件路径
        String filePath = cfgFilePath;
        ArrayList<String> cfgItems = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!(line.length()<=2||(line.charAt(0)=='/'&&line.charAt(1)=='/'))){
                    cfgItems.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cfgItems;
    }

    public ArrayList<ProductionItem> compilerCfg(ArrayList<String> cfgItems){
        //消除做递归
        cfgItems = eliminate(cfgItems);
        ArrayList<ProductionItem> productionItems = new ArrayList<>();
        //获取非终符结合，在产生式左部出现过的符号，就是非终结符。
        nts = getNts(cfgItems);
        for(String item:cfgItems){
            //获取产生式左部和右部
            String []byDeduce = item.split("->");
            //去除产生式左部的空格
            String leftValue = byDeduce[0].replaceAll(" ","");
            //将产生式右部某个部分 以| 分割的部分
            String [] byOr = byDeduce[1].split("\\|");
            //在以空格进行分割，获取每个产生式符号
            for(String or:byOr){
                ProductionItem pi = new ProductionItem();
                pi.left = new BaseSymbol(leftValue, BaseSymbol.NT,null);
                String [] byBlank = or.split(" ");
                BaseSymbol curr = null;
                for(String str:byBlank){
                    if (!str.equals("")){
                        int symType;
                        if (str.equals("ε")) symType = BaseSymbol.EPS;
                        //判断是否是非终结符，在文法左部出现过的符号就是非终结符
                        else if (isNt(str)) symType = BaseSymbol.NT;
                        else symType = BaseSymbol.T;
                        if (pi.right==null){
                            pi.right = new BaseSymbol(str,symType);
                            curr = pi.right;
                        }else{
                            curr.next = new BaseSymbol(str,symType);
                            curr = curr.next;
                        }
                    }
                }
                productionItems.add(pi);
            }
        }
        return productionItems;
    }

    /**
     * 是否是非终结符
     * @param v
     * @return
     */
    private boolean isNt(String v){
        return nts.contains(v);
    }
    private HashSet<String> getNts(ArrayList<String> cfgItems){
        HashSet<String> nts = new HashSet<>();
        for(String item:cfgItems){
            String []byDeduce = item.split("->");
            nts.add(byDeduce[0].trim());
        }
        return nts;
    }

    public static ArrayList<String> eliminate(ArrayList<String> rules) {
        // 创建一个新的规则列表
        ArrayList<String> newRules = new ArrayList<>();
        // 遍历每一条规则
        for (String rule : rules) {
            // 将规则拆分为左部和右部
            String[] parts = rule.split("->");
            String left = parts[0].trim();
            String right = parts[1].trim();
            // 如果规则的右部以左部开头，说明这是一条直接左递归规则
            if (right.startsWith(left)) {
                // 将左部后面添加一个'_prime'，作为新的非终结符号
                String leftPrime = left + "' ";
                // 将规则拆分为以左部开头和不以左部开头的两部分
                String[] rightParts = right.split(left, 2);
                // 添加两条新的规则，一条是左递归转化为右递归的规则，一条是终结规则
                newRules.add(left + "->" + rightParts[1]+" " + leftPrime);
                newRules.add(leftPrime + "->" + rightParts[0]+" " + leftPrime + "| ε");
            }
            // 如果规则不是直接左递归规则，则直接加入新的规则列表
            else {
                newRules.add(rule);
            }
        }
        // 返回新的规则列表
        return newRules;
    }

    public List<ProductionItem>  run(){
        items= compilerCfg(readCfg(cfgFilePath));
        setOfNullable = nullable(items);
        tableFirst = first(items,setOfNullable);
        tableFollow = follow(items);
        //select
        for(ProductionItem item:items){
            //如果右部第一个是终结符,直接添加至自己的select集合
            if (item.right.type== BaseSymbol.T){
                item.select.add(item.right);
            }
            //如果右部第一个不是终结符,将first集合添加至select集合
            else if (item.right.type== BaseSymbol.NT){
                item.select.addAll(toSymbol(tableFirst.get(item.right.v)));
            }
            //如果右部第一个是空串,将follow集合添加至select集合
            else if (item.right.type== BaseSymbol.EPS){
                item.select.addAll(toSymbol(tableFollow.get(item.left.v)));
            }
        }
        return items;
    }

    private HashSet<BaseSymbol> toSymbol(HashSet<String> set){
        HashSet<BaseSymbol> setSym = new HashSet<>();
        for(String s:set){
            if (!s.equals("ε")){
                BaseSymbol symbol = new BaseSymbol();
                symbol.type = BaseSymbol.T;
                symbol.v = s;
                setSym.add(symbol);
            }
        }
        return setSym;
    }
    //求First集
    public Hashtable<String,HashSet<String>> first(List<ProductionItem> ps,HashSet<String>nullAble){
        Hashtable<String, HashSet<String>> table = new Hashtable<>();
       // 初始化表格
        for(ProductionItem item:ps){
            if (!table.containsKey(item.left.v)){
                table.put(item.left.v,new HashSet<>());
            }
        }
        boolean isChanged = true;
        while (isChanged){
            isChanged = false;
            for(ProductionItem item:ps){
                // 当前行集合
                HashSet<String> row = table.get(item.left.v);
                BaseSymbol p = item.right;
                if(p.type == BaseSymbol.EPS){
                    if (row.add(p.v)) isChanged = true;
                }
                // 如果是终结符
                else if (p.type == BaseSymbol.T){
                    if (row.add(p.v)) isChanged = true;
                }// 非终结符
                else{
                    while (p!=null&&p.type== BaseSymbol.NT){
                        if (row.addAll(table.get(p.v))) isChanged = true;
                        if(nullAble.contains(p.v)){
                            p = p.next;
                        }else  break;
                    }
                }
            }
        }
        return  table;
    }

    /**
     *
     * @param ps 产生式列表
     * @return 可推出空串的集合
     */
    public HashSet<String> nullable(List<ProductionItem> ps){
        HashSet<String> setOfNullable = new HashSet<>();
        boolean isChanged = true;
        while (isChanged){
            isChanged  = false;
            // 遍历所有产生式
            for(ProductionItem item:ps){
                BaseSymbol p = item.right;
                //如果是ε,添加到集合
                if(p.type== BaseSymbol.EPS){
                    if (!setOfNullable.contains(item.left.v)){
                        isChanged = setOfNullable.add(item.left.v);
                    }
                }
                //如果是非终结符 并且已经在nullable 集合中才需要进行下一步
                else if (p.type == BaseSymbol.NT&&setOfNullable.contains(p.v)){
                    // 是否可以推出空串的标记，可推出空串为true
                    boolean flagNull = true;
//                    p = p.next;
                    /*
                     * X->B1B2...Bn  X属于 nullable集合 当且仅当 B1B2...Bn可以推出ε
                     * 依次查看文法右部是否可以推出空串
                     **/
                    while (p!=null){
                        /*
                        * 如果p不是终结符 或者 p不属于nullable（p不属于nullable可能只是暂时的)则推不出空串
                        * */
                        if(!(p.type== BaseSymbol.NT)||!(setOfNullable.contains(p.v))){
                             flagNull = false;
                             break;
                        }
                        p = p.next;
                    }
                    if(flagNull){
                        isChanged = setOfNullable.add(item.left.v);
                    }
                }
            }

        }
        return setOfNullable;
    }

    /**
     * follow集合，产生式列表
     * @param ps
     * @return
     */
    public  Hashtable<String,HashSet<String>> follow(List<ProductionItem> ps){
        //初始化表
        Hashtable<String,HashSet<String>> table = new Hashtable<>();
        for(ProductionItem item:ps){
            table.put(item.left.v,new HashSet<>());
        }
        table.get(ps.get(0).left.v).add("$");
        boolean isChanged = true;

        while (isChanged){
            isChanged = false;
            for(ProductionItem item:ps){
                Stack<BaseSymbol> ss = new Stack<>();
                BaseSymbol tp = item.right;
                while (tp!=null){
                    ss.push(tp);
                    tp = tp.next;
                }
                HashSet<String> temp = new HashSet<>();
                BaseSymbol prePart = null;
                while (!ss.isEmpty()){
                    BaseSymbol currentPart = ss.pop();

                    if (currentPart.type == BaseSymbol.T){
                        temp.add(currentPart.v);
                    }
                    else if(currentPart.type== BaseSymbol.NT){
                        //如果是最后一个非终结符 例如 E->TM  中的M
                        if(currentPart.next ==null){
                            followU(followAs(table,currentPart),followAs(table,item.left));
                        }else{
                            HashSet<String> followOfCurrentPart = followAs(table,currentPart);
                            if (followU(followOfCurrentPart,firstAs(prePart))){
                                isChanged = true;
                            }
                            if (followU(followOfCurrentPart,temp)){
                                isChanged = true;
                                temp.clear();
                            }
                            if (isNullable(prePart)){
                                if (followU(followOfCurrentPart,followAs(table,prePart))) isChanged = true;
                            }
                        }
                    }
                    prePart = currentPart;
                }
            }
        }
        return  table;
    }
    private  HashSet<String> firstAs(BaseSymbol p){
        if (p==null) return new HashSet<>();
        else if (tableFirst.containsKey(p.v)) return tableFirst.get(p.v);
        else return new HashSet<>();
    }
    private HashSet<String> followAs(Hashtable<String,HashSet<String> >tableOfFollow, BaseSymbol p){
        if (p==null) return new HashSet<>();
        else if (tableOfFollow.containsKey(p.v)) return tableOfFollow.get(p.v);
        else return new HashSet<>();
    }
    private boolean isNullable(BaseSymbol part){
        if(part==null) return true;
        if (setOfNullable.contains(part.v)) return true;
        return false;
    }
    private boolean isNonTerminal(){

        return false;
    }

    private boolean isUpper(char c){
        return c>='A'&&c<='Z';
    }
    private boolean isAlpha(char c){
        return c>='a'&&c<='z' || c>='A'&&c<='Z';
    }
    private boolean isLower(char c){
        return isAlpha(c)&&!isUpper(c);
    }

    private  boolean  followU(HashSet<String> a,HashSet<String> b){
        int prel = a.size();
        for(String v:b){
            if (!v.equals("ε")){
                a.add(v);
            }
        }
        return !(prel==a.size());
    }

}
