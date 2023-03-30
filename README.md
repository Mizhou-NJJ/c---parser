# c---parser
这个一个C--语言的语法分析器，包括词法分析、语法分析、语义处理等。

### 文法及其对应的语义动作(@开头的标识符为语义动作)

``` java
program -> func funcs
funcs -> func funcs | ε
func ->  type IDN ( args ) @MAKE_FUNC_TABLE func_body
type -> int | short | long |char |float |double| void
args -> type IDN @ARG_INFO @ARGS arg | ε
arg -> , type IDN @ARG_INFO arg | ε
func_body -> ; | block
block -> { @FUNC_INIT @GEN_FUNC_PROCESS_LABEL define_stmts stmts }
define_stmts -> define_stmt define_stmts | ε
define_stmt -> type IDN @CHECK_VAR_DEFINE @VAR_INIT init vars ;
init -> = expression @CHECK_EXPR_TYPE @GEN_DEFINE_EXPR_CODE | ε
vars -> , IDN @CHECK_VAR_DEFINE init vars | ε
stmts -> stmt stmts | ε
stmt -> assign_stmt @GEN_EXPR_ASSIGN_CODE | jump_stmt | iteration_stmt | branch_stmt
assign_stmt -> expression @VAR_INIT @CHECK_EXPR_TYPE ;
jump_stmt -> continue ; | break ; | return innull_expr ;
iteration_stmt -> while ( @LABEl_OUT_IN logical_expression ) @GEN_IF_CODE @GEN_TRUE_LABEL block_stmt @GEN_ENTER_CODE | for ( isnull_expr ; isnull_expr ; isnull_expr ) block_stmt | do block_stmt while ( logical_expression ) ;
branch_stmt -> if (@LABEL_OUT_IN logical_expression ) @GEN_IF_CODE @GEN_TRUE_LABEL  block_stmt @GEN_FALSE_LABEL result | switch ( IDN ) { case_stmt case_stmts default_stmt }
result -> else block_stmt | ε

logical_expression -> ! expression @REVERS_EQ_OPER @GEN_MID_EXPR @CHECK_BOOL_EXPR bool_expression | expression @GEN_MID_EXPR @CHECK_BOOL_EXPR bool_expression
bool_expression -> lop @GEN_LABEL_OR_CODE expression @GEN_MID_EXPR bool_expression | ε
//下面的or 是 ||
lop -> && | or
case_stmts -> case_stmt case_stmts | ε
case_stmt -> case const : stmts
default_stmt -> default : stmts
block_stmt -> { @MAKE_CHILD_TABLE stmts }
expression -> value operation
operation -> compare_op value | equal_op value | ε
compare_op -> > | >= | < | <= | == | != @EN_EXPRESSION_STACK
equal_op -> = @CAL_LEFTID_INFO | += | -= | *= | /= | %=
value -> item value'
value' -> + item @GEN_EXPR_CODE value' | - item @GEN_EXPR_CODE value' | ε
item -> factor item'
item' -> * factor @GEN_EXPR_CODE item' | / factor @GEN_EXPR_CODE item' | % factor @GEN_EXPR_CODE item' | ε
factor -> ( value ) | IDN @CHECK_UNDEFINE call_func | const
call_func -> ( es ) @GEN_CALL_FUNC | ε
es -> isnull_expr isnull_es
isnull_expr -> expression | ε
isnull_es -> , isnull_expr isnull_eas | ε
const -> num_const | FLOAT | CHAR | STR
num_const -> INT10 @NUM_CONST
```

### 项目结构
zhou.cfg
> 文法相关如解释文法、计算first、follow集合等

zhou.lex
> 词法分析相关

zhou.parser
> 语法分析，同时进行语法制导翻译，按照老师要求，采用递归下降分析方法，Descent.java为分析器。

zhou.view
> UI组件

### 实现功能
类型检测、重复定义或未定义、方法调用等

### 输入输出
输入：c语言测试代码 、文法
输出：词法分析结果、文法推导过程、中间代码或上述遇到的错误。





