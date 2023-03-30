package zhou.parser;

import sun.misc.Queue;
import zhou.lex.Lexer;
import zhou.lex.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class Descent {
    public static boolean turnOnlog = false;
    //词法分析器对象，用于读取下一个Token, 通过r.scanner()读取；
    private Lexer r;
    //全局符符号表
    private HashMap<String, SymbolTable> tables;
    //用于存放表达式符号
    private Stack<Symbol> globalStack = new Stack<>();
    //用于存放globalStack的当前大小
    private Stack<Integer> globalStackPointer = new Stack<>();
    private StringBuilder processDeduce = new StringBuilder();
    public static final String MODEL_ERROR = "存在语法错误,此程序不是此文法的子句";

    public Descent(Lexer lexer) {
        this.r = lexer;
        tables = new HashMap<>();
    }

    public String getProcessDeduce() {
        return processDeduce.toString();
    }

    /**
     * 开始分析，每个方法对应着文法的推导过程，方法名对应文法符号名称
     */
    public void startDecent() {
        try {
            program(r.scanner());
        } catch (Exception e) {
            //随便填的
            error(new Token("unknow error", 0xfffffffe, "", Integer.MAX_VALUE), "Unknow error");
        }
    }

    private void program(Token t) {
        print("programe -> func funcs");
        //这里应该判断 t.value 是否属于 select(program),可以但没必要
        func(t);
        funcs(r.scanner());
        if (r.scanner().value.equals("$")) {
            log("语法分析结束");
            for (String s : codeExpr) {
                log("三地址码:" + s);
            }
        } else {
            log("存在语法错误");

        }

    }

    private void func(Token t) {
        print("func -> type IDN ( args ) func_body");
        //获取基本类型
        String retType = type(t);
        //下一个期望获取的是标识符 IDN
        Token expID = r.scanner();
        log("func:" + expID.value);
        //如果类型不是标识符应当报错
        if (expID.type != Token.IDS) {
            error(expID, MODEL_ERROR);
        }
        //期望获取 (
        Token expLB = r.scanner();
        //不是则报错
        if (!expLB.value.equals("(")) {
            error(expLB, MODEL_ERROR);
        }
        /*
         * 返回值是参数符号Symbol列表,当是空参方法时，返回null
         *
         * */
        List<Symbol> symArgs = args(r.scanner());
        Token expRB = r.scanner();
        if (!expRB.value.equals(")")) {
            error(expRB, MODEL_ERROR);
        }
        //*********语法动作对应SDT中的@MAKE_FUNC_TABLE*************
        //构造符号表
        SymbolTable funcTable = new SymbolTable();
        funcTable.offset = 0;
        funcTable.tableName = expID.value;
        funcTable.retType = retType;
        //记录参数个数
        if (symArgs==null) funcTable.paramCount=0;
        else funcTable.paramCount = symArgs.size();
        //判断是否是空参,如果不是，则添加到符号表
        if (symArgs != null) {
            funcTable.items.addAll(symArgs);
        }
        //添加到全局符号表，这个用于函数间相互调用时检查类型、或者是否定义
        tables.put(expID.value, funcTable);
        //************************************
        func_body(r.scanner(), funcTable,expID.value);
    }

    private void funcs(Token t) {
        //funcs -> func funcs
        //funcs -> ε
        //select（funcs_1) = { short long int float double unsigned void char }
        //select(funcs-20 = {$}
        /**
         * 继续递归调用，funcs -> func funcs或者空串，调用func的前提市 token属于 select(funcs_1)
         *
         */
        if (isIn(t.value, "short", "long", "int", "float", "double", "unsigned", "void", "char")) {
            print("funcs -> func funcs");
            func(t);
            funcs(r.scanner());
        }
        // funcs - >EPS
        /*
         * funcs可以使用空产生式的前提是 t.value 属于 select(funcs_2);
         * */
        else {
            print("funcs-> ε");
            --r.tkPoint;
            if (!isIn(t.value, "$")) error(t, MODEL_ERROR);
        }
    }

    private String type(Token t) {
        print("type-> int|short|...as " + t.value);
        return valueIn(t.value, "int", "short", "long", "char", "float", "double", "void");
    }

    private List<Symbol> args(Token t) {
        List<Symbol> symArgs = null;
        String _type = type(t);
        if (_type != null) {
            print("args -> type IDN arg");
            Token expID = r.scanner();
            if (expID.type != Token.IDS) {
                error(expID, MODEL_ERROR);
            }
            symArgs = new ArrayList<>();
            Symbol symArg1 = new Symbol();
            symArg1.varName = expID.value;
            symArg1.varType = _type;
            symArg1.isInit=true;
            symArg1.size = sizeOf(_type);
            symArgs.add(symArg1);
            arg(r.scanner(), symArgs);

        } else {
            print("args -> ε");
            --r.tkPoint;
            if (!t.value.equals(")")) {
                error(t, MODEL_ERROR);
            }
        }
        return symArgs;
    }

    private void arg(Token t, List<Symbol> argsList) {
        //定义符号表条目
        Symbol _s = null;
        if (t.value.equals(",")) {
            print("arg -> , type IDN arg");
            Token expType = r.scanner();
            String argType = type(expType);
            if (argType == null) {
                error(expType, MODEL_ERROR);
            }

            Token expID = r.scanner();
            if (expID.type != Token.IDS) {
                error(expID, MODEL_ERROR);
            }
            //**********ARG_INFO***********
            _s = new Symbol();
            _s.size = sizeOf(argType);
            _s.varType = argType;
            _s.varName = expID.value;
            _s.isInit = true;
            argsList.add(_s);
            //******************
            arg(r.scanner(), argsList);
        } else {
            print("arg -> ε");
            --r.tkPoint;
            if (!t.value.equals(")")) {
                error(t, MODEL_ERROR);
            }
        }

    }

    private void func_body(Token t, SymbolTable table,String funcName) {
        if (t.value.equals("{")) {
            print("func_body -> block");
            block(t, table,funcName);
        }
        //看SELECT(func_block) ={"}"}
        else {
            --r.tkPoint;
            print("func_body -> EPS ");
            if (!t.value.equals(";")) {
                error(t, MODEL_ERROR);
            }
        }
    }

    /**
     * 解析方法体
     *
     * @param t
     * @param table
     */
    private void block(Token t, SymbolTable table,String funcName) {
        print("block -> { define_stmts stmts }");
        if (!t.value.equals("{")) error(t, "at block:" + MODEL_ERROR);
        /*
        *  @FUNC_INIT
        * 方法已被初始化,记录到符号表
        * */
        table.isInit = true;
//        Symbol symFunc = getSymbolInBy(funcName,table);
//        symFunc.isInit = true;
//        symFunc.varName = funcName;
//        symFunc.varType = tables.get(funcName).retType;
        /*
         * @GEN_FUNC_PROCESS_LABEL
         * 此方法对应一个过程，生成此过程的标号 其实就是函数名称，
         * 将此标号初始化到符号表
         * */
        table.label = funcName;
        codeExpr.add(funcName+":");

        /*
         * 声明语句 后接其它语句，此处文法有缺陷，已经在define_stmts方法和stmts方法中说明与处理
         * */
        define_stmts(r.scanner(), table);
        stmts(r.scanner(), table);
        //期望得到的下一个符号是 }
        Token expFRB = r.scanner();
        if (!expFRB.value.equals("}")) error(expFRB, "at block" + MODEL_ERROR);
    }

    /**
     * 解析声明语句
     *
     * @param t
     * @param table
     */
    private void define_stmts(Token t, SymbolTable table) {
        /*
         *   define_stmts -> define_stmt define_stmts
         *   define_stmts -> ε
         *   select(define_stmts)
         * */
        // 第一条define_stmts的select集合
        if (isIn(t.value, "long", "double", "short", "void", "unsigned", "float", "int", "char")) {
            print("define_stmts -> define_stmt define_stmts");
            log("define_stmts:" + t.value);
            define_stmt(t, table);
            /**
             * 下面的if else 控制也是解决文法局限性问题，例如：
             *  声明语句后无法使用赋值语句的问题
             */
            Token next = r.scanner();
            //如果是标识符，则是赋值语句，否则是声明语句
            if (isTypeIn(next.type, Token.IDS)) {
                stmts(next, table);
            } else {
                define_stmts(next, table);
            }

        }
        //define_stmts - > EPS
        else {
            print("define_stmts -> EPS");
            --r.tkPoint;
            if (!(isIn(t.value, "{", "break", "if", "}", "case", "default", "while", "(", "switch", "for", "do", "continue", "return") ||
                    isTypeIn(t.type, Token.CNS_STR, Token.INT8, Token.INT10, Token.CHAR, Token.IDS, Token.INT16, Token.FLOAT))) {
                // select(define_stmts)中的类型判断
                error(t, "at define_stmts:" + MODEL_ERROR);
            }
        }
    }

    private void define_stmt(Token t, SymbolTable table) {
        print("define_stmt -> type IDN init vars");
        String varType = type(t);
        if (varType == null) error(t, MODEL_ERROR);

        Token expID = r.scanner();
        if (expID.type != Token.IDS) error(expID, MODEL_ERROR);
        //******SDT CHECK_VAR_DEFINE
        //如果已经定义，则需要报错
        if (isInTable(table, expID.value)) {
            error(expID, expID.value + " 重复定义");
        }
        //否则加入符号表
        else {
            Symbol symId = new Symbol();
            symId.varName = expID.value;
            symId.varType = varType;
            symId.size = sizeOf(varType);
            table.offset += symId.size;
            table.items.add(symId);
        }
        /*
         * VAR_INIT 见擦汗表达式项是否已经被初始化了
         * */
        Token next = r.scanner();
        if (next.value.equals("=")){
            Symbol symVar = getSymbolInBy(expID.value,table);
            symVar.isInit = true;
        }
        init(next, table, expID);

        vars(r.scanner(), table);
        Token expSeal = r.scanner();
        if (!expSeal.value.equals(";")) error(expSeal, MODEL_ERROR);
    }

    private void stmts(Token t, SymbolTable table) {
        //stmts -> stmt stmts
        //stmts -> ε
        //select(stmts-1) = {INT8 ( while IDN for continue INT10 INT16 return do CHAR switch STR FLOAT break if }
        //select(stmts-2) = {default } case }
        /*
         * 这里没有按照产生式stmts规则来写，因为那个LL1文法无法推导出一些符合语义规范的字串，如：
         * 会出现赋值语句后无法在进行语句声明的问题，
         * 这里加了if else 控制来修复这个问题
         * */
        log("stmts enter---------:" + t.value);
        if (isIn(t.value, "int", "double", "float", "char", "short", "unsigned", "long", "short")) {
            log("stmts_defin_stmts:" + t.value);
            define_stmts(t, table);
            t = r.scanner();
        }
        log("stmts:" + t.value);
        if (isIn(t.value, "(", "while", "for", "continue", "return", "do", "switch", "break", "if") ||
                isTypeIn(t.type, Token.INT8, Token.IDS, Token.INT10, Token.INT16, Token.CHAR, Token.CNS_STR, Token.FLOAT)) {
            print("stmts -> stmt stmts");
            stmt(t, table);
            stmts(r.scanner(), table);

        }
        // stms-> EPS
        else {
            print("stmts -> EPS");
            --r.tkPoint;
            if (!isIn(t.value, "default", "}", "case")) {
                error(t, "at stmts:" + MODEL_ERROR);
            }
        }
//        }

    }

    private void init(Token t, SymbolTable table, Token id) {
        log("init:" + t.value);
        if (t.value.equals("=")) {
            print("init -> = expression");
            //记录当前栈的大小，方便在解析完expression方法后，计算出栈数目
            globalStackPointer.push(globalStack.size());
            //
            Token expId = r.scanner();
            expression(expId, table);
            /*
             * SDT @CHECK_EXPR_TYPE
             *检查表达式类型，一般情况下，double类型可以被int short等类型赋值，
             * 这里为了简单起见，只要类型不同，都报错，如int a = b(short 类型);
             * 检查表达式项是否初始化
             * 只有声明语句才可以推到 init,而init -> = expression,这意味着 =号左边的符号一定刚被加到符号表中，
             * 而符号表最后一个就是它
             * */
            //获取表达式左边的符号
            Symbol parent = table.items.get(table.items.size() - 1);
            int popCount = globalStack.size() - globalStackPointer.pop();
            //依次检擦类型

            while (popCount-- > 0) {
                Symbol tmp= globalStack.pop();
                //当是调用方法是 如 int a = test();
                //就需要去全局表中查找
                SymbolTable st = tables.get(tmp.varName);
                if (st!=null&&st.isInit){
                    tmp.varType = st.retType;
                    tmp.isInit = st.isInit;
                    tmp.varName = st.tableName;
                }
                if(!tmp.isInit&&!tmp.varName.equals(parent.varName)) error(t,tmp.varName+"未初始化");
                if (!parent.varType.equals(tmp.varType)) {
                    error(expId, expId.value + " 表达式类型不合法");
                }
            }
            // SDT GEN_DEFINE_EXPR_CODE
            codeExpr.add(String.format("%s=%s", id.value, expressionStack.pop().varName));
        }
        // init -> EPS
        else {
            print("init -> EPS");
            --r.tkPoint;
            if (!(t.value.equals(",") || t.value.equals(";"))) {
                error(t, MODEL_ERROR);
            }
        }
    }

    private void vars(Token t, SymbolTable table) {
        //vars -> , IDN init vars
        //vars -> ε
        //select(vars-1) = {,}
        //select(vars-2) = {;}
        if (t.value.equals(",")) {
            print("vars -> , IDN init vars");
            Token expID = r.scanner();
            if (expID.type != Token.IDS) error(expID, MODEL_ERROR);
            /*
             SDT CHECK_VAR_DEFINE
            检查变量是否定义过，它的属性继承自父节点的属性
            * */
            // 获取父节点
            Symbol symParent = table.items.get(table.items.size() - 1);
            //检查是否已经定义
            if (isInTable(table, expID.value)) {
                error(expID, expID.value + " 重复定义");
            }
            //否则加入符号表
            else {
                Symbol symId = new Symbol();
                symId.varName = expID.value;
                symId.varType = symParent.varType;
                symId.size = sizeOf(symParent.varType);
                table.offset += symId.size;
                table.items.add(symId);
            }
            init(r.scanner(), table, expID);
            vars(r.scanner(), table);
        }
        //vars - >EPS
        else {
            print("vars -> EPS");
            log("var:" + t.value);
            --r.tkPoint;
            if (!t.value.equals(";")) error(t, MODEL_ERROR);
        }
    }

    private void expression(Token t, SymbolTable table) {
        //expression-> value operation
        //select(expression) = {INT10 CHAR INT8 IDN INT16 STR ( FLOAT }
        log("expression:" + t.value);
        print("expression -> value operation");
        value(t, table);
        operation(r.scanner(), table);
    }

    private void stmt(Token t, SymbolTable table) {
        log("stmt:" + t.value);
        if (isTypeIn(t.type, Token.CHAR, Token.CNS_STR, Token.INT10, Token.IDS, Token.INT8, Token.INT16) ||
                isIn(t.value, ")")) {
            print("stmt -> assign_stmt");

            assign_stmt(t, table);
            // GEN_EXPR_ASSIGN_CODE
            // 生成最后的首地址赋值码

            if (expressionStack.size()>0){
                if (!tables.containsKey(t.value))
                codeExpr.add(String.format("%s=%s", t.value, expressionStack.pop().varName));
            }
//            expressionStack.push(symCode);

        } else if (isIn(t.value, "break", "return", "continue")) {
            print("stmt -> jump_stmt");
            jump_stmt(t, table);
        } else if (isIn(t.value, "while", "do", "for")) {
            print("stmt -> iteration_stmt");
            iteration_stmt(t, table);
        } else if (isIn(t.value, "if", "switch")) {
            print("stmt -> branch_stmt");
            branch_stmt(t, table);
        } else {
            error(t, "at stmt :" + MODEL_ERROR);
        }
    }

    private void assign_stmt(Token t, SymbolTable table) {
        print("assign_stmt -> expression ;");
        log("assign_stmt:" + t.value);
        Symbol s = new Symbol();
//
//        globalStack.push(n);
        //表达式右部付哈
        Symbol expLeft = getSymbolInBy(t.value, table);
        //记录当前指针位置,方便计算下次出栈数
        globalStackPointer.push(globalStack.size());
        globalStack.push(expLeft);
        expression(t, table);
        /*
        * @CHECK_EXPR_TYPE 检查表达式类型是否合法,检查表达式项是否已被初始化
        * */
        Symbol tmp = globalStack.pop();
        int popCount = globalStack.size() - globalStackPointer.pop();
        while (popCount-- > 0) {
            //当是调用方法是 如 int a = test();
            //就需要去全局表中查找
            SymbolTable st = tables.get(tmp.varName);
            if (st!=null&&st.isInit){
                tmp.varType = st.retType;
                tmp.isInit = st.isInit;
                tmp.varName = st.tableName;
            }
            if(!tmp.isInit&&!tmp.varName.equals(expLeft.varName)) error(t,tmp.varName+"未初始化");
            if (!tmp.varType.equals(expLeft.varType)) {
                error(t, "表达式类型不合法");
            }
            tmp = globalStack.pop();
        }

        log("assign_stmt:gsize" + globalStack.size());
        Token expSeal = r.scanner();
        if (!expSeal.value.equals(";")) {
            error(t, MODEL_ERROR);
        }
    }

    private void jump_stmt(Token t, SymbolTable table) {
        log("jump_stmt: eat[return]" + t.value);
        if (isIn(t.value, "continue", "break")) {
            print("jump_stmt -> continue ;| break ;");
            Token expSeal = r.scanner();
            if (!expSeal.value.equals(";")) {
                error(t, MODEL_ERROR);
            }
        } else if (t.value.equals("return")) {
            print("jump_stmt -> return  inull_expr");
            isnull_expr(r.scanner(), table);
            Token expSeal = r.scanner();
            if (!expSeal.value.equals(";")) {
                error(t, MODEL_ERROR);
            }
        } else {
            error(t, MODEL_ERROR);
        }

    }

    private void iteration_stmt(Token t, SymbolTable table) {
        if (t.value.equals("while")) {
            Token expLBR = r.scanner();
            if (!expLBR.value.equals("(")) error(expLBR, MODEL_ERROR);
            /*
             * @LABEL_OUT_IN
             * 生成 while表达式为true的标号和 为false的标号
             * */
            String enterLabel = "L" + (++indexOfL);
            codeExpr.add(enterLabel + ":");
            ProcessInfo processInfo = new ProcessInfo("L" + (++indexOfL), "L" + (++indexOfL));
            processInfo.enterLabel = enterLabel;
            ifProcessInfo.push(processInfo);
            //
            logical_expression(r.scanner(), table);
            Token expRBR = r.scanner();
            if (!expRBR.value.equals(")")) error(expRBR, MODEL_ERROR);
            /*
             * @GEN_IF_CODE
             *
             * */
            String nextLabel = ifProcessInfo.peek().nextLabel;
            if (nextLabel != null) {
                codeExpr.add(String.format("%s:", ifProcessInfo.peek().nextLabel));
            }
            codeExpr.add(String.format("if %s goto %s", ifExpr.pop(), ifProcessInfo.peek().TRUE));
            codeExpr.add(String.format("goto %s", ifProcessInfo.peek().FALSE));
            /*
             * @GEN_TRUE_LABEL
             * 生成表达式为真时的出口标号
             * */
            codeExpr.add(String.format("%s:", ifProcessInfo.peek().TRUE));

            block_stmt(r.scanner(), table);
            /*
             * @GEN_LABEL_CODE
             * 生成while循环入口的三地址码
             * */
            codeExpr.add(String.format("goto %s", ifProcessInfo.peek().enterLabel));
            //此过程结束，过程记录信息已经没用了
            ifProcessInfo.pop();

        } else if (t.value.equals("for")) {
            Token expLBR = r.scanner();
            if (!expLBR.value.equals("(")) error(expLBR, MODEL_ERROR);
            isnull_expr(r.scanner(), table);
            Token expSeal1 = r.scanner();
            if (!expSeal1.value.equals(";")) error(expSeal1, MODEL_ERROR);
            isnull_expr(r.scanner(), table);
            Token expSeal2 = r.scanner();
            if (!expSeal2.value.equals(";")) error(expSeal2, MODEL_ERROR);
            isnull_expr(r.scanner(), table);
            Token expRBR = r.scanner();
            if (!expRBR.value.equals(")")) error(expRBR, MODEL_ERROR);
            block_stmt(r.scanner(), table);
        } else if (t.value.equals("do")) {
            block_stmt(r.scanner(), table);
            Token expWhile = r.scanner();
            if (!expWhile.value.equals("while")) error(expWhile, MODEL_ERROR);
            Token expLBR = r.scanner();
            if (!expLBR.value.equals("(")) error(expLBR, "匹配不到(");
            logical_expression(r.scanner(), table);
            Token expRBR = r.scanner();
            if (!expRBR.value.equals(")")) error(expRBR, "匹配不到)");
            Token expSeal = r.scanner();
            if (!expSeal.value.equals(";")) error(expSeal, "匹配不到 ;");
        }
    }

    private int indexOfL = 0;

    private void branch_stmt(Token t, SymbolTable table) {
        print("branch_stmt -> if ( logical_expression ) block_stmt result");
        if (t.value.equals("if")) {
            Token expLBR = r.scanner();
            if (!expLBR.value.equals("(")) error(expLBR, "匹配不到 (");
            // SDT LABEL_OUT_IN
            /*
             * 生成if语句为True的标号和False的标号
             * */
//            codeExpr.add("L" + indexOfL + ":");
            ProcessInfo processInfo = new ProcessInfo("L" + (++indexOfL), "L" + (++indexOfL));
            ifProcessInfo.push(processInfo);
            //
            logical_expression(r.scanner(), table);
            Token expRBR = r.scanner();
            log("expRBP:" + expRBR.value);
            if (!expRBR.value.equals(")")) error(expRBR, "匹配不到 ）");
            // SDT GEN_IF_CODE
            /**
             * if语句内的表达式执行完后，中间表达式栈中还存在着最后一个比较表达式，
             * 生成它的三地址码,如果没有逻辑符号如 && ||,则 nextLabel 为空,就不需要生成下一个逻辑表达式的
             * 标号
             */
            String nextLabel = ifProcessInfo.peek().nextLabel;
            if (nextLabel != null) {
                codeExpr.add(String.format("%s:", ifProcessInfo.peek().nextLabel));
            }
            codeExpr.add(String.format("if %s goto %s", ifExpr.pop(), ifProcessInfo.peek().TRUE));
            codeExpr.add(String.format("goto %s", ifProcessInfo.peek().FALSE));

            // GEN_TRUE_LABEL
            //这里是表达式为true时的出口
            codeExpr.add(String.format("%s:", ifProcessInfo.peek().TRUE));
            block_stmt(r.scanner(), table);
            //SDT GEN_FALSE_LABEL
            codeExpr.add(String.format("%s:", ifProcessInfo.peek().FALSE));
            // 此条if语句解析完毕，过程信息就没用了
            ifProcessInfo.pop();
            //
            result(r.scanner(), table);
        } else if (t.value.equals("switch")) {
            Token expLBR = r.scanner();
            if (!expLBR.value.equals("(")) error(expLBR, "匹配不到 (");
            Token expId = r.scanner();
            if (expId.type != Token.IDS) error(expId, MODEL_ERROR);
            Token expRBR = r.scanner();
            if (!expRBR.value.equals(")")) error(expRBR, "匹配不到）");
            Token expHkh = r.scanner();
            if (!expHkh.value.equals("{")) error(expHkh, "匹配不到 {");
            case_stmt(r.scanner(), table);
            case_stmts(r.scanner(), table);
            default_stmt(r.scanner(), table);
            Token expRhkh = r.scanner();
            if (!expRhkh.value.equals("}")) error(expRhkh, "匹配不到 }");
        } else {
            error(t, MODEL_ERROR);
        }

    }

    private void case_stmt(Token t, SymbolTable table) {

        if (!t.value.equals("case")) error(t, MODEL_ERROR);
        _const(r.scanner());
        Token expMh = r.scanner();
        if (!expMh.value.equals(":")) error(expMh, MODEL_ERROR);
        stmts(r.scanner(), table);
    }

    private void case_stmts(Token t, SymbolTable table) {
        if (t.value.equals("case")) {
            case_stmt(t, table);
            case_stmts(r.scanner(), table);
        } else {
            --r.tkPoint;
            if (!isIn(t.value, "}", "default")) {
                error(t, MODEL_ERROR);
            }
        }
    }

    private void default_stmt(Token t, SymbolTable table) {
        if (!t.value.equals("default")) error(t, MODEL_ERROR);
        Token expMh = r.scanner();
        if (!expMh.value.equals(":")) error(expMh, MODEL_ERROR);
        stmts(r.scanner(), table);
    }

    private void result(Token t, SymbolTable table) {
        if (t.value.equals("else")) {
            block_stmt(r.scanner(), table);
        } else {
            --r.tkPoint;
            log("res:" + t.value);
            if (!((isIn(t.value, "while", "return", "default", "if", "}", "do", "for", "(", "continue", "case", "break", "switch") ||
                    isTypeIn(t.type, Token.INT8, Token.INT10, Token.FLOAT, Token.INT16, Token.CNS_STR, Token.CHAR, Token.IDS)))) {
                error(t, MODEL_ERROR);
            }
        }
    }

    private Stack<String> ifExpr = new Stack<>();
    private Stack<ProcessInfo> ifProcessInfo = new Stack<>();

    private void logical_expression(Token t, SymbolTable tble) {

        if (t.value.equals("!")) {
            print("logical_expression -> ! expression bool_expression");
            expression(r.scanner(), tble);
            /*
             * @GEN_MID_EXPR
             * */
            //GEN_MID_EXPR
            Symbol expRight = expressionStack.pop();
            Symbol expOper = expressionStack.pop();
            Symbol expLeft = expressionStack.pop();
            /*
             * @CHECK_BOOL_EXPR
             * 检查是否是布尔表达式的，这里不检测 if(v)的形式，
             * 只判断 a 比较符号 b的情况
             * */
            if (!isIn(expOper.varName, ">", "<", "!=", ">=", "<=", "==")) {
                error(t, "bool表达式错误!");
            }
            // @REVERSE_EQ_OPER 对比较符号进行反转
            //为了简单起见，！时，只把比较符号进行反转
            String reversOpers = null;
            switch (expOper.varName) {
                case ">":
                    reversOpers = "<=";
                    break;
                case ">=":
                    reversOpers = "<";
                    break;
                case "!=":
                    reversOpers = "==";
                    break;
                case "==":
                    reversOpers = "!=";
                    break;
                case "<":
                    reversOpers = ">=";
                    break;
                case "<=":
                    reversOpers = ">";
                    break;
            }

            ifExpr.push(expLeft.varName + reversOpers + expRight.varName);
            /*
             * @CHECK_BOOL_EXPR 检查是否是布尔表达式
             * */
            bool_expression(r.scanner(), tble);
        } else if (isIn(t.value, "(") || isTypeIn(t.type, Token.INT8, Token.CNS_STR, Token.INT16, Token.CHAR, Token.IDS, Token.FLOAT, Token.INT10)) {
            print("logical_expression -> expression bool_expression");
            expression(t, tble);
            //GEN_MID_EXPR
            Symbol expRight = expressionStack.pop();
            Symbol expOper = expressionStack.pop();
            Symbol expLeft = expressionStack.pop();
            /*
             * @CHECK_BOOL_EXPR
             * 检查是否是布尔表达式的，这里不检测 if(v)的形式，
             * 只判断 a 比较符号 b的情况
             * */
            if (!isIn(expOper.varName, ">", "<", "!=", ">=", "<=", "==")) {
                error(t, "bool表达式错误!");
            }
            ifExpr.push(expLeft.varName + expOper.varName + expRight.varName);

            bool_expression(r.scanner(), tble);
        }
    }

    private void bool_expression(Token t, SymbolTable table) {
        if (isIn(t.value, "||", "&&")) {
            print("bool_expression -> lop expression bool_expression");
            String oper = lop(t);
            if (oper.equals("||")) {
                codeExpr.add(String.format("if %s goto %s", ifExpr.pop(), ifProcessInfo.peek().TRUE));
                String newLabel = "L" + (++indexOfL);
                ifProcessInfo.peek().nextLabel = newLabel;
                codeExpr.add(String.format("goto %s", newLabel));
            } else if (oper.equals("&&")) {
                String newLabel = "L" + (++indexOfL);
                codeExpr.add(String.format("if %s goto %s", ifExpr.pop(), newLabel));
                ifProcessInfo.peek().nextLabel = newLabel;
                codeExpr.add(String.format("goto %s", ifProcessInfo.peek().FALSE));
            }
            expression(r.scanner(), table);
            // GEN_MID_EXPR
            Symbol expRight = expressionStack.pop();
            Symbol expOper = expressionStack.pop();
            Symbol expLeft = expressionStack.pop();
            ifExpr.push(expLeft.varName + expOper.varName + expRight.varName);
            //
            bool_expression(r.scanner(), table);
        } else {
            print("bool_expression -> EPS");
            --r.tkPoint;
            if (!isIn(t.value, ")", ";")) error(t, MODEL_ERROR);
        }
    }

    private String lop(Token t) {
        print("lop -> || | &&");
        if (!isIn(t.value, "&&", "||")) error(t, MODEL_ERROR);
        else return t.value;
        return null;
    }

    private void value(Token t, SymbolTable table) {
        log("value:" + t.value);
        print("value -> item value'");
        item(t, table);
        valueN(r.scanner(), table);
    }

    private void operation(Token t, SymbolTable table) {

        log("operation:" + t.value);
        if (isIn(t.value, "!=", ">", "<=", "==", "<", ">=")) {
            print("operation -> compare_op value");
            compare_op(t);
            value(r.scanner(), table);
        } else if (isIn(t.value, "-=", "%=", "*=", "+=", "=", "/=")) {
            print("operation -> equal_op value");
            equal_op(t);
            value(r.scanner(), table);
        } else {
            print("operation -> EPS");
            --r.tkPoint;
            if (!isIn(t.value, ";", "||", "&&", ",", ")")) {
                error(t, MODEL_ERROR);
            }
        }
    }

    private void compare_op(Token t) {
        print("compare_op -> > | >= | < | <= | == !=");
        if (!isIn(t.value, ">", ">=", "<", "<=", "==", "!=")) {
            error(t, MODEL_ERROR);
        } else {
            // GEN_EXPR_CODE
            Symbol s = new Symbol();
            s.varName = t.value;
            s.varType = t.value;
            expressionStack.push(s);
        }
    }

    private void equal_op(Token t) {
        print("equal_op -> = | += | -= | *= | /= | %=");
        if (!isIn(t.value, "=", "+=", "-=", "*=", "/=", "%=")) error(t, MODEL_ERROR);
    }

    private void block_stmt(Token t, SymbolTable table) {
        print("block_stmt -> stmts");
        log("block_stmt:" + t.value);
        if (!t.value.equals("{")) error(t, "block_stmt: does not matched '{'");
        /*
         * @MAKE_CHILD_TABLE
         * 建立符号表，初始化符号表的parent属性
         * */
        SymbolTable tableChild = new SymbolTable();
        tableChild.parent = table;
        tableChild.offset = 0;
        //
        stmts(r.scanner(), tableChild);
        Token expRBR = r.scanner();

        if (!expRBR.value.equals("}")) error(expRBR, "block_stmt:does not matched '}'");
    }

    private void item(Token t, SymbolTable table) {
        print("item -> factor item'");
        log("item:" + t.value);
        factor(t, table);
        itemN(r.scanner(), table);
    }

    private Stack<Symbol> expressionStack = new Stack<>();
    private int addrExpIndex = 1;
    private List<String> codeExpr = new ArrayList<>();

    private void valueN(Token t, SymbolTable table) {
        //value' - > + item value'
        //value' - > - item value'
        //value' - > EPS
        //select(value'_3) = {) ; or <= >= == && /= -= += *= != < > %= = , }
        if (isIn(t.value, "+", "-")) {
            print("value' -> + item value' | - item value'");
            String currOper = t.value;
            item(r.scanner(), table);
            // SDT GEN_EXPR_CODe
            Symbol pre = expressionStack.peek();
            String code = expressionStack.pop().varName + currOper + expressionStack.pop().varName;
            codeExpr.add("t" + addrExpIndex + "=" + code);
            Symbol symCode = new Symbol();
            symCode.varName = "t" + addrExpIndex;
            symCode.varType = pre.varType;
            expressionStack.push(symCode);
            addrExpIndex++;
            log("VALUEN:-------- " + pre.varName);
            valueN(r.scanner(), table);
        }
        // value' -> EPS 的select集合
        else {
            print("value' -> EPS");
            log("valueN:" + t.value);
            --r.tkPoint;
            if (!isIn(t.value, ")", ";", "||", "<=", ">=", "==", "&&", "/=", "-=", "+=", "*=", "!=", "<", ">", "%=", "=", ",")) {
                error(t, "Does not match select(value'_3)");
            }
        }
    }

    private void factor(Token t, SymbolTable table) {
        log("factor: " + t.value);
        if (t.value.equals("(")) {
            print("factor -> * factor item'");
            value(r.scanner(), table);
            Token expRBR = r.scanner();
            if (!expRBR.value.equals(")")) {
                error(expRBR, "）得不到匹配");
            }
        } else if (t.type == Token.IDS) {
            print("factor -> IDN call_func");
            //CHECK_UNDEFINE
            // 可能是变量或方法
            if (!isInTable(table, t.value) && !isInGlobalTable(t.value)) {
                error(t, "未定义，请先定义在使用");
            }
            //如果已经定义压栈
            Symbol ins = getSymbolInBy(t.value, table);
            expressionStack.push(ins);
            globalStack.push(ins);
            //*************************
            call_func(r.scanner(), table,t);
        } else if (isTypeIn(t.type, Token.INT16, Token.FLOAT, Token.CHAR, Token.INT10, Token.INT8, Token.CNS_STR)) {
            print("factor -> const");
            Symbol vconst = _const(t);
            //SDT
            globalStack.push(vconst);
            expressionStack.push(vconst);

        } else {
            error(t, MODEL_ERROR);
        }

    }

    private void itemN(Token t, SymbolTable table) {
        //select(item'_4) = {*= ; %= && = += < -= > - >= + == or <= , /= ) != }
        log("itemN: " + t.value);
        if (isIn(t.value, "*", "/", "%")) {
            print("item' -> * factor item' | / factor item' | % factor item' ");
            factor(r.scanner(), table);
            // SDT GEN_EXPR_CODE
            Symbol pre = expressionStack.peek();
            String code = expressionStack.pop().varName + t.value + expressionStack.pop().varName;
            codeExpr.add("t" + addrExpIndex + "=" + code);
            Symbol symCode = new Symbol();
            symCode.varName = "t" + addrExpIndex;
            symCode.varType = pre.varType;
            expressionStack.push(symCode);
            addrExpIndex++;
            itemN(r.scanner(), table);
        }
        // item' -> EPS
        else {
            print("item' -> EPS");
            --r.tkPoint;
            if (!isIn(t.value, "*=", ";", "%=", "&&", "=", "+=", "<", "-=", ">", "-", ">=", "+", "==", "||", "<=", "/=", ")", "!=", ",")) {
                error(t, MODEL_ERROR);
            }
        }
    }

    private void call_func(Token t, SymbolTable table,Token expID) {
        if (t.value.equals("(")) {
            print("call_func -> ( es )");
            int preStackSize = expressionStack.size();
            es(r.scanner(), table);
            Token expRBR = r.scanner();
            if (!expRBR.value.equals(")")) error(t, "）得不到匹配");
            /**
             * @GEN_CALL_FUNC
             * 生成函数调用代码，包括生成参数,检查参数个数等，这里不检查参数类型是否合法了
             */
            //参数个数
            int popCount = expressionStack.size()-preStackSize;
            int paramCount = popCount;
            //检查参数个数时候匹配
            int targetFuncParamCount  = tables.get(expID.value).paramCount;
            if (targetFuncParamCount!=paramCount){
                error(expID,String.format("参数个数不匹配，目标参数个数%d个，当前 %d个",targetFuncParamCount,paramCount));
            }
            //由于第一个参数在栈低，所以反过来
            Stack<Symbol> paramStack =new Stack<>();
            while (popCount-->0){
                paramStack.push(expressionStack.pop());
            }
            //生成参数代码
            while (!paramStack.isEmpty()){
                codeExpr.add("param "+paramStack.pop().varName);
            }
            //生成调用语句
            codeExpr.add(String.format("call %s , %d",expID.value,paramCount));

        }
        //call_func -> EPS
        //SELECT(call_func_2)={<= %= * += >= /= -= *= == , > % != ; + ) - && / < = or }
        else {
            log("call_func:" + t.value);
            print("call_func -> EPS");
            --r.tkPoint;
            if (!isIn(t.value, "<=", "%=", "*", "+=", ">=", ">=", "/=", "-=", "*=", "==", ",", ">", "%", "!=", ";", "+", ")", "-", "&&", "/", "<", "=", "||")) {
                error(t, "call_func: 无任何可选产生式");
            }
        }

    }

    public Symbol _const(Token t) {
        //SELECT(const_1) = {INT10 INT8 INT16 }
        Symbol s = null;
        if (isTypeIn(t.type, Token.INT10, Token.INT8, Token.INT16)) {
            print("const -> num_const");
            return num_const(t);
        } else {
            print("const -> FLOAT | CHAR | STR");
            if (!isTypeIn(t.type, Token.FLOAT, Token.CHAR, Token.CNS_STR)) {
                error(t, MODEL_ERROR);
            } else {
                s = new Symbol();
                switch (t.type) {
                    case Token.FLOAT:
                        s.value = t.value;
                        s.varName = t.value;
                        s.isInit = true;
                        s.varType = "float";
                        break;
                    case Token.CHAR:
                        s.value = t.value;
                        s.varType = "char";
                        s.varName = t.value;
                        s.isInit=true;
                        break;
//                    case Token.CNS_STR:
                }
                return s;
            }
        }
        return s;
    }

    public void es(Token t, SymbolTable table) {
        print("es -> isnull_expr isnull_es");
        isnull_expr(t, table);
        isnull_es(r.scanner(), table);
    }

    private void isnull_expr(Token t, SymbolTable table) {
        //isnull_expr -> expression
        //isnull_expr -> EPS
        //SELECT(isnull_expr_1) = {FLOAT INT16 INT10 STR CHAR IDN INT8 ( }
        //SELECT(isnull_expr_2) = {) , ; }

        if (isTypeIn(t.type, Token.FLOAT, Token.INT16, Token.INT10, Token.CNS_STR, Token.CHAR, Token.IDS, Token.INT8) ||
                isIn(t.value, "(")) {
            print("isnull_expr -> expression");
            expression(t, table);
        }
        //isnull_expr -> EPS
        else {
            print("isnull_expr -> EPS");
            --r.tkPoint;
            if (!isIn(t.value, ")", ",", ";")) {
                error(t, "isnull_expr: 没有可选产生式");
            }
        }
    }

    private void isnull_es(Token t, SymbolTable table) {
        if (t.value.equals(",")) {
            print("isnull_es -> , isnull_expr isnull_es");
            isnull_expr(r.scanner(), table);
            isnull_es(r.scanner(), table);
        }
        //isnull_es -> EPS
        else {
            print("isnull_es -> EPS");
            --r.tkPoint;
            if (!t.value.equals(")")) {
                error(t, MODEL_ERROR);
            }
        }
    }

    private Symbol num_const(Token t) {
        print("num_const -> INT10| INT8 | INT16");
        Symbol s = null;
        if (!isTypeIn(t.type, Token.INT8, Token.INT10, Token.INT16)) {
            error(t, MODEL_ERROR);
        } else {
            s = new Symbol();
            s.varType = "int";
            s.value = t.value;
            s.isInit = true;
            s.varName = t.value;
            return s;
        }
        return s;
    }

    /**
     * 判断 v 是否 = set中的值，其作用是判断一个Token.value是否属于Select(x)集合
     *
     * @param v   Token.value
     * @param set Select(x)的集合
     * @return
     */
    private boolean isIn(String v, String... set) {
        for (String s : set) {
            if (v.equals(s)) return true;
        }
        return false;
    }

    /**
     * 和上面的方法作用一样，不同的是，这个方法是用来判断类型是否相同
     *
     * @param t
     * @param set
     * @return
     */
    private boolean isTypeIn(int t, int... set) {
        for (int e : set) {
            if (t == e) return true;
        }
        return false;
    }

    //存放错误用的
    private StringBuilder errorBuilder = new StringBuilder();

    private void error(Token t, String msg) {
        log("Error(" + t.value + ") 在第:" + t.rowNumber + "行 信息:" + msg);
        errorBuilder.append("Error(" + t.value + ") 在第:" + t.rowNumber + "行 信息:" + msg + "\n");
//        System.exit(0xfff);
    }

    public String getErrorMessage() {
        return errorBuilder.toString();
    }

    private String valueIn(String v, String... set) {
        for (String s : set) {
            if (v.equals(s)) return s;
        }
        return null;
    }

    private void print(String rule) {
        processDeduce.append(rule);
        processDeduce.append("\n");
    }

    /**
     * 计算类型的大小
     *
     * @param type
     * @return
     */
    private int sizeOf(String type) {
        switch (type) {
            case "int":
            case "float":
                return 4;
            case "short":
                return 2;
            case "long":
            case "double":
                return 8;
            case "char":
                return 1;
            case "void":
                return 0;
        }
        return 0;
    }

    /**
     * 检查一个符号是否存在于符号表，如果已经存在，说明已经定义
     *
     * @param table
     * @param name
     * @return
     */
    private boolean isInTable(SymbolTable table, String name) {
        SymbolTable tem = table;
        while (tem != null) {
            for (Symbol sb : tem.items) {
                if (sb.varName.equals(name)) return true;
            }
            tem = tem.parent;
        }
        return false;
    }

    /**
     * 检查符号是否在全局符号表中定义
     *
     * @param funName 一般是方法名称，或Symbol.varName
     * @return
     */
    private boolean isInGlobalTable(String funName) {
        return tables.containsKey(funName);
    }

    /**
     * 按名称反回符号表中指定的符号，即从全局符号表中查找，也从当前过程符号表中查找
     *
     * @param name
     * @param table
     * @return
     */
    private Symbol getSymbolInBy(String name, SymbolTable table) {
        Symbol s = null;
        SymbolTable tem = table;
        /**
         * 一个方法体可能存在多个符号表，例如 if语句外层和内层分别对应一个符号表，if语句外层的符号表
         * 应该指向if内层的符号表
         *
         * 查表时，应该从当前符号表朝外层查，因为定义在if外层的变量，在if中也可以使用
         */
        while (tem != null) {
            for (Symbol sb : tem.items) {
                if (sb.varName.equals(name)) return sb;
            }
            //外层表
            tem = tem.parent;
        }
        //是方法定义
        if (tables.containsKey(name)) {
            s = new Symbol();
            SymbolTable sb = tables.get(name);
            s.varName = name;
            s.varType = sb.retType;
            s.size = sizeOf(s.varType);
        }
        return s;
    }

    /**
     * @return 返回三地址码
     */
    public List<String> getCode() {
        return codeExpr;
    }

    private void log(String s) {
        if (turnOnlog) {
            System.out.println("log: " + s);
        }
    }

}
