package zhou.parser;

/**
 * 控制流语句的描述对象
 * 一般记录语句标号
 */
public class ProcessInfo {
    // 语句未真时的出口
    public String TRUE;
    //语句为假时的出口
    public String FALSE;
    //下一个标号
    public String nextLabel;
    //过程的入口地址
    public String enterLabel;
    public ProcessInfo(){}
    public ProcessInfo(String t,String  f){
        TRUE = t;
        FALSE = f;
    }
}
