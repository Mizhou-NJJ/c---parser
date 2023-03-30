package zhou.lex;

public class Token {
    public final static int IDS=2;
    public final static String C_IDS="标识符";

    public final static int REV_WORD =1;//保留字
    public final static String C_REV_WORD ="保留字";

    public final static int OPER = 4;
    public final static  String C_OPER = "运算符";
    public final static int SBL = 5;
    public final static String C_SBL ="分隔符";
    public final static int CNS_STR = 6;
    public final static String C_CNS_STR = "字符串常量";
    public final static int CNS_CHAR = 7;
    public final static String C_CNS_CHAR = "字符常量";
    public final static int ERROR_TOKEN = 0xffff; // 错误的Token
    public final static int EOF = 0xffffffff;
    public final static int INT10 = 9;
    public final static int INT8 = 10;
    public final static int INT16 = 11;
    public final static int FLOAT = 12;
    public final static int CHAR = 13;

    public String value;
    public int type;
    public int line;
    public String common;
    public int rowNumber;
    /**
     *
     * @param v 标识符
     * @param t 标识符类型
     * @param common 对此标识符的注释
     */
    public Token(String v,int t,String common,int rowNumber){
        this.value=v;
        this.type=t;
        this.common=common;
        this.rowNumber = rowNumber;
    }

}
