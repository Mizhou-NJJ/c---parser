package zhou.lex;

import java.util.ArrayList;


public class Lexer {
    public final static String[] KW = {"include","struct","NULL","int","double","long","float","short","boolean","while","if","for","do","return","break","case","continue","define","goto","void","char"};
    private int rowNumber;
    private int pointer;
    private String text;
    private char [] ctext;
    private long tlen;
    private int indexOfToken;
    public final static char BLK = ' ';
    public final static char TAB = '\t';
    public final static char EOL='\0';
    public final static char NL = '\n';
    public final static char WNL = '\r';

    private ArrayList<Token> tks;
    private ArrayList<TokenError> errors;

    public Lexer(String text){
        this.text=text;
        this.tlen = text.length();
        this.ctext = text.toCharArray();
        this.tks = new ArrayList<>();
        this.errors = new ArrayList<>();
        rowNumber = 1;

    }
    public Lexer start(){
        char c;
        if (tlen<=0){
            System.out.println("无效的输入");
            System.exit(0);
        }
        while (pointer<tlen){
            c = ctext[pointer];
            if (c==BLK||c==TAB) pointer++;
            else if (c==NL){
                rowNumber++;
                pointer++;
            }else if (c==WNL&&ctext[pointer+1]==NL){
                rowNumber++;
                pointer+=2;
            }
            else{

                lexPart(c);
            }
        }
        //基本的语法分析是否满足基本构词语法
        basicGrammer();
        return this;

    }
    private void basicGrammer(){
       /*
       * 再C语言中，除了分隔符不应该不相隔出现,如 :
       * int int method(）； int func fuc()都是错误的
       * if(a====b)
       * if(a>b b&&c<0)
       * */
       Token preToken = new Token("Test",-1," Test233",rowNumber);
       for(int i=0;i<tks.size();i++){
           Token currToken = tks.get(i);
           //如果两个同类型单词相连出现说明是错了
           if (currToken.type == preToken.type&&currToken.type!=Token.SBL){
               switch (currToken.type){
                   case Token.REV_WORD:
                   case Token.OPER:
                       errors.add(new TokenError(currToken.rowNumber,"在 "+preToken.value +" "+currToken.value+"附近发生错误!"));

                       break;
                   case Token.IDS:
                       if(i-2>=0){
                           if (tks.get(i-2).type == Token.SBL){
                               errors.add(new TokenError(currToken.rowNumber,"在"+preToken.value +" "+currToken.value+"附近发生错误"));
                           }

                       }

                       break;

               }
               preToken.value = preToken.value+"(error near)";
               preToken.type = Token.ERROR_TOKEN;
           }
           preToken = currToken;
       }
    }

    private void add(Token tk){
        tks.add(tk);
        indexOfToken++;
    }
    //分析
    private void lexPart(char c){
        if (isDigit(c)){
            // 如果第一个字母是数字，
            String tk = splOne();

            if (isNumber(tk)){
                add(new Token(tk,Token.INT10,"INT10",rowNumber));
            }
        }else if (isAlphaOr_(c)){
            /*
             *  第一个是字母 则可能是 标识符，保留字
             * */
            String tk = splOne();


            if (isIdentify(tk)){
                if (isKeyword(tk)){
                    add(new Token(tk,Token.REV_WORD,Token.C_REV_WORD,rowNumber));
                }else{ // 标识符
                    add(new Token(tk,Token.IDS,Token.C_IDS,rowNumber));
                }
            }

        }else if(c=='"'){
            String tk = splOne();
            if (isString(tk)){
                add(new Token(tk,Token.CNS_STR,Token.C_CNS_STR,rowNumber));
            }else{
                errors.add(new TokenError(rowNumber,"缺少\""));
            }
        }else if(c=='\''){
            String tk = splOne();
            System.out.println(tk+"-----------");
            if(tk.charAt(0)=='\'' &&tk.charAt(tk.length()-1)=='\''){
                add(new Token(tk,Token.CHAR,"CHAR",rowNumber));
            }else{
                errors.add(new TokenError(rowNumber,"缺少\'"));
            }
        }
        else{

           switch (c){
               case '[':
               case ']':
               case ',':
               case ';':
                   add(new Token(c+"",Token.SBL,Token.C_SBL,rowNumber));
                   break;
               case '(':
               case ')':
               case '{':
               case '}':
                   add(new Token(c+"",Token.SBL,Token.C_SBL,rowNumber));
                   break;


               case '+':
                   /*
                   *  可能是 += ++ +
                   * */
                   if (pointer+1<tlen){
                       if (ctext[pointer+1]=='+'){ // ++
                           add(new Token("++",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else if (ctext[pointer+1]=='='){//+=
                           add(new Token("+=",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else {
                           add(new Token("+",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }
                   break;
               case '-':
                   /*
                   *  可能是 -= ,--,-
                   * */
                   if (pointer+1<tlen){
                       if (ctext[pointer+1]=='-'){ // ++
                           add(new Token("--",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else if (ctext[pointer+1]=='='){//+=
                           add(new Token("-=",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else {
                           add(new Token("-",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }else{
                       errors.add(new TokenError(rowNumber,"在“-”附近发生错误!"));
                   }
                   break;
               case '!':
                   /*
                   *  可能是 ! ,!=
                   * */
                   if (pointer<tlen){
                       if (ctext[pointer+1]=='='){
                           add(new Token("!=",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else {
                           add(new Token("!",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }else {
                       errors.add(new TokenError(rowNumber,"在“!”附近发生错误!"));
                   }
                   break;
               case '=':
                   /*
                   * 可能是 =,==
                   * */
                   if (pointer<tlen){
                       if (ctext[pointer+1]=='='){
                           add(new Token("==",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else{
                           add(new Token("=",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }else {
                       errors.add(new TokenError(rowNumber,"在“=”附近发生错误!"));
                   }
                   break;
               case '<':
                   if (pointer+1<tlen){
                       if (ctext[pointer+1]=='='){
                           add(new Token("<=",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else {
                           add(new Token("<",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }else errors.add(new TokenError(rowNumber,"在“<”附近发生错误!"));
                   break;
               case '>':
                   if (pointer+1<tlen){
                       if (ctext[pointer+1]=='='){
                           add(new Token(">=",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else {
                           add(new Token(">",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }else errors.add(new TokenError(rowNumber,"在“>”附近发生错误!"));
                   break;
               case '*':
                   /*
                   *  可能 *,*=,*IDS,**IDS,***IDS
                   *
                   * */
                   if (pointer+1<tlen){
                       if (ctext[pointer+1]=='='){
                           add(new Token("*=",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else {
                           add(new Token("*",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }else errors.add(new TokenError(rowNumber,"在“*”附近发生错误!"));
                   break;
               case '/':
                   /*
                   *  可能是 /,//,/=，
                   *  多行注释 以/*开头以 star star/结尾
                   * */
                   if (pointer+1<tlen){
                       if (ctext[pointer+1]=='='){
                           add(new Token("/=",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else if (ctext[pointer+1]=='/'){
                           while (pointer<tlen&&(ctext[pointer]!=NL&&ctext[pointer]!=WNL)) pointer++;
                           rowNumber++;
                       } else if (ctext[pointer+1]=='*'){ // 多行注释
                           while (pointer<tlen){
                               if (ctext[pointer]==NL||ctext[pointer]==WNL) rowNumber++;
                              if (ctext[pointer]=='/'&&ctext[pointer-1]=='*'&&ctext[pointer-2]=='*'){
                                  break;
                              }
                              pointer++;
                           }
                       } else{
                           add(new Token("/",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }else errors.add(new TokenError(rowNumber,"在“/”附近发生错误!"));
                   break;
               case '%':
                   if (pointer+1<tlen){
                       if (ctext[pointer+1]=='='){
                           add(new Token("%=",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else {
                           add(new Token("%",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }else errors.add(new TokenError(rowNumber,"在“%”附近发生错误!"));
                   break;

               case '#':
                   //预处理忽略
                   break;
               case '.':
                   //成员运算 忽略
                   break;
               case '&':
                   // 逻辑与 算数与
                   if (pointer+1<tlen){
                       if (ctext[pointer+1]=='&'){
                           add(new Token("&&",Token.OPER,Token.C_OPER,rowNumber));
                           pointer++;
                       }else {
                           add(new Token("&",Token.OPER,Token.C_OPER,rowNumber));
                       }
                   }else errors.add(new TokenError(rowNumber,"在“%”附近发生错误!"));

                   break;
               case '|':
                   //逻辑或 算术或
                   if (pointer+1<tlen){
                       if (ctext[pointer+1]=='|'){
                           add(new Token("||",Token.OPER,Token.C_OPER,Token.OPER));
                           pointer++;
                       }else {
                           add(new Token("|",Token.OPER,Token.C_OPER,Token.OPER));
                       }
                   }else errors.add(new TokenError(rowNumber,"在“%”附近发生错误!"));

                   break;
               default:errors.add(new TokenError(rowNumber,c+"是非法的语句成分"));break;
           }
           pointer++;
        }
    }
    public String splOne(){
        StringBuilder b= new StringBuilder();
        while (pointer<tlen){
            if (isAlphaOr_(ctext[pointer])||isDigit(ctext[pointer])){
                b.append(ctext[pointer]);
                pointer++;
            }else if (ctext[pointer]=='"'){
                b.append(ctext[pointer]);
                pointer++;
                while (pointer<tlen&&ctext[pointer]!='"'){
                    b.append(ctext[pointer]);
                    pointer++;
                }
                b.append(ctext[pointer++]);
                return b.toString();
            }else if(ctext[pointer]=='\''){
                b.append(ctext[pointer]);
                pointer++;
                while (pointer<tlen&&ctext[pointer]!='\''){
                    b.append(ctext[pointer]);
                    pointer++;
                }
                b.append(ctext[pointer++]);
                return b.toString();
            }
            else {
                return b.toString();
            }
        }
        return b.toString();
    }
    public boolean isString(String t){
        return t.charAt(0) == '"'&&t.charAt(t.length()-1)=='"';
    }
    public boolean isNumber(String t) {
        char ct[] = t.toCharArray();
        char c = ct[0];
        if (isDigit(c)){
            if (c=='0'){
                if (t.length()==1) return true;
                /*
                 *  可能是 0b 0x
                 * */
                if (ct[1]=='x'){ // 16进制前缀
                    if (isHexNumber(ct,2)){
                        return true;
                    }else {
                        if (indexOfToken-1>=0){
                            if (tks.get(indexOfToken-1).type == Token.OPER){

                                errors.add(new TokenError(rowNumber,"“"+t+"”是非法的16进制数"));
                            }else{
                                add(new Token(t,Token.IDS,"",rowNumber));
                                errors.add(new TokenError(rowNumber,"“"+t+"”是非法的标识符"));
                            }
                        }
                    }
                }else if (ct[1]=='b'){//二进制前缀

                    if (isBinaryNumber(ct,2)){
                        return true;
                    }else {
                        if (indexOfToken-1>=0){
                            if (tks.get(indexOfToken-1).type == Token.OPER){

                                errors.add(new TokenError(rowNumber,"“"+t+"”是非法的2进制数"));
                            }else{
                                add(new Token(t,Token.IDS,"",rowNumber));
                                errors.add(new TokenError(rowNumber,"“"+t+"”是非法的标识符"));
                            }
                        }
                    }
                }else{ //八进制
                    if (isOctNumber(ct,1)){
                        return true;
                    }else {
                        if (indexOfToken-1>=0){
                            if (tks.get(indexOfToken-1).type == Token.OPER){

                                errors.add(new TokenError(rowNumber,"“"+t+"”是非法的8进制数"));
                            }else{
                                add(new Token(t,Token.IDS,"",rowNumber));
                                errors.add(new TokenError(rowNumber,"“"+t+"”是非法的标识符"));
                            }
                        }

                    }

                }
            }else{ // 1-9
                if (isDecimal(ct,0)){
                    return true;
                }else {
                    if (indexOfToken-1>=0){
                        if (tks.get(indexOfToken-1).type == Token.OPER){

                            errors.add(new TokenError(rowNumber,"“"+t+"”是非法的10进制数"));
                        }else{
                            add(new Token(t,Token.IDS,"",rowNumber));
                            errors.add(new TokenError(rowNumber,"“"+t+"”是非法的标识符"));
                        }
                    }

                }
            }
        }
        return false;
    }

    /**
     *
     * @param ct 待分析字符数组 ex: {''}
     * @param p
     * @return
     */
    private boolean isHexNumber(char [] ct,int p){

        boolean f=false;
        for(int i=p;i<ct.length;i++){
            if (isDigit(ct[i])||(ct[i]>='a'&&ct[i]<='f')||(ct[i]>='A'&&ct[i]<='F')) f=true;
            else return false;
        }
        return f;
    }

    private boolean isBinaryNumber(char [] ct,int p){
        boolean f=false;
        for(int i=p;i<ct.length;i++){
            if (ct[i]=='1'||ct[i]=='0') f=true;
            else return false;
        }
        return f;
    }
    private boolean isOctNumber(char []ct,int p){
        boolean f=false;
        for(int i=p;i<ct.length;i++){
            if (ct[i]>='0'&&ct[i]<='7') f=true;
            else return false;
        }
        return f;
    }

    private boolean isDecimal(char [] ct,int p){
        boolean f=false;
        for(int i=p;i<ct.length;i++){
            if (isDigit(ct[i])) f=true;
            else return false;
        }
        return f;
    }
    private boolean isAlpha(char c){
        if ((c>='a'&&c<='z')||(c>='A'&&c<='Z')) return true;
        return false;
    }
    private boolean isAlphaOr_(char c){
        return isAlpha(c)|| c =='_';
    }
    private boolean isDigit(char c){
        if (c>='0'&&c<='9') return  true;
        return false;
    }
    public boolean isIdentify(String tk){
        char [] ctk = tk.toCharArray();
        if (!isAlpha(ctk[0])&&ctk[0]!='_') return false;
        else if (tk.length()==1) return true;
        boolean f=false;
        for(int i=1;i<ctk.length;i++){
           if (isAlpha(ctk[i])||ctk[i]=='_'||isDigit(ctk[i])) f = true;
           else return false;
        }
        return f;
    }
    public boolean isKeyword(String tk){

        for(String k:KW){
            if(k.equals(tk)) return true;
        }
        return false;
    }

    public ArrayList<Token> getTks() {
        return tks;
    }
    public ArrayList<TokenError> getErrors(){
        return errors;
    }
    public boolean isHasError(){
        return errors.size()>0;
    }
    public int tkPoint;
    public Token scanner(){
        Token ret = null;
        if (tkPoint<tks.size()){
            ret = tks.get(tkPoint);
            tkPoint++;
        }
//        if (ret==null) System.exit(0xff-5);
        return ret;
    }



}

