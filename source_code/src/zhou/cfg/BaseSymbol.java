package zhou.cfg;


public class BaseSymbol {
    // 终结符
    public final static int T = 1;
    //非终结符
    public final static int NT = 0;
    public final static int EPS = 999;
    public final static int HEAD = -1;

    public int type;
    public String v;
    // 一般存在于文法右部
    public BaseSymbol next;
    public int Mtype;
    public final static char EPSILON = 'ε';
    public BaseSymbol(){
    }
    public BaseSymbol(String v, int type, BaseSymbol next){
        this.v = v;
        this.type = type;
        this.next = next;
    }
    public BaseSymbol(String v, int type){
        this.v = v;
        this.type = type;
    }

}
