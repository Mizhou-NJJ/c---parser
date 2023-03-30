package zhou.lex;

public class TokenError {
    private int rowNumber;
    private String msg;
    public TokenError(int rowNumber,String msg){
        this.rowNumber=rowNumber;
        this.msg = msg;
    }

    public String errorMsg() {
        return "第"+rowNumber+"行发生错误,"+msg;
    }
}
