package zhou;
import zhou.cfg.LL1Runner;
import zhou.cfg.ProductionItem;
import zhou.lex.Lexer;
import zhou.lex.Token;
import zhou.lex.TokenError;
import zhou.parser.Descent;
import zhou.view.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AppMain extends JFrame {
    private Container container;
    private String testCodePath;
    private String cfgPath;
    public AppMain(String testCodePath,String cfgPath){
        this.testCodePath = testCodePath;
        this.cfgPath = cfgPath;
    }
    public static void main(String[] args) {
        Descent.turnOnlog = false;
        new AppMain("test_code.txt"/*测试代码文件路径*/,"cfg.txt"/*文法路径*/).start();
    }
    public void start(){
        setSize(500,500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("测试代码编辑器");
        setLocationRelativeTo(null);
        init();
        setVisible(true);
    }
    public static JEditorPane editorPane;
    private void init(){
        container = getContentPane();
        JPanel panelMain = new JPanel();
        panelMain.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel();
        JButton start = new JButton("分析");
        start.addActionListener(new OnStart());
        northPanel.add(start);
        panelMain.add(northPanel,BorderLayout.NORTH);
        editorPane = new JEditorPane();
        editorPane.setText(readSourceCode(testCodePath));
        JScrollPane scrollPane = new JScrollPane(editorPane);
        panelMain.add(scrollPane,BorderLayout.CENTER);

        container.add(panelMain);
    }
    private FrameLexerResult frameLexerResult;
    private FrameSelect frameSelect;
    private FrameProcessOfDeduce frameProcessOfDeduce;
    private FrameCode frameCode;
    class OnStart implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            if (frameLexerResult!=null) frameLexerResult.dispose();
            if (frameSelect!=null) frameSelect.dispose();
            if (frameProcessOfDeduce!=null) frameProcessOfDeduce.dispose();
            if (frameCode!=null) frameCode.dispose();
            String sourceCode = editorPane.getText();
            if (sourceCode.length()==0){
                return;
            }
            Lexer lexer = new Lexer(sourceCode);
            // 获取Token列表
            ArrayList<Token> tks = lexer.start().getTks();
            //输出词法分析中的错误
            if(lexer.isHasError()){
                StringBuilder errLexers = new StringBuilder();
                for(TokenError te:lexer.getErrors()){
                    errLexers.append(te.errorMsg()+"\n");
                }
                new FrameError(errLexers.toString()).start("词法分析中的错误");
            }
            //如果词法分析阶段没错
            else{
                //在最后添加$符号，代表程序结束
                tks.add(new Token("$",Token.EOF,"End of file",tks.get(tks.size()-1).rowNumber));
                //显示词法分析结果
                frameLexerResult = new FrameLexerResult(tks);
                frameLexerResult.start();
                //解释文法
                LL1Runner LL1Runner = new LL1Runner(cfgPath);
                /**
                 * 获取产生式列表
                 * 其中 ProductionItem item;
                 *  item.left 是文法左部
                 *  item.right是文法右部 item.right是一个链表，它的next属性指向右部的下一个文法符号
                 */
                List<ProductionItem> items = LL1Runner.run();
                /**
                 *
                 *
                 * 显示Select集合
                 */

                //显示Select集合
                frameSelect = new FrameSelect(items);
                frameSelect.start();

                /*
                * 递归下降分析
                * */
                Descent descent = new Descent(lexer);
                descent.startDecent();
                /*
                 * 显示推导过程
                 *
                 * */
                frameProcessOfDeduce = new FrameProcessOfDeduce(descent.getProcessDeduce());
                frameProcessOfDeduce.start();
                // 获取语法分析时的错误
                String parseError = descent.getErrorMessage();
                if (parseError.length()>0){
                    FrameError frameError = new FrameError(parseError);
                    frameError.start("语法分析时遇到的错误");
                }
                //显示中间代码
                else{
                    List<String> codes = descent.getCode();
                    frameCode = new FrameCode(codes);
                    frameCode.start();
                }
            }

        }
    }




    public static String readSourceCode(String filePath){
        InputStream in = null;
        String sourceCode = null;
        StringBuilder b = new StringBuilder();
        try {
            in = new FileInputStream(new File(filePath));
            int v ;
            while ((v=in.read())!=-1){
                b.append((char)v);
            }
            sourceCode = b.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sourceCode;
    }
}
