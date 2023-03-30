package zhou.view;

import zhou.lex.Token;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

// 词法分析结果
public class FrameLexerResult extends JFrame {
    private Container container;
    private ArrayList<Token> tks;
    public FrameLexerResult (ArrayList<Token> tks){
        this.tks = tks;
    }
    public void start(){
        setSize(500,500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("词法分析结果");
        setLocation(0,0);
        init();
        setVisible(true);
    }
    public static JTable table;
    private void init(){
        container = getContentPane();
        JPanel panelMain = new JPanel();
        panelMain.setLayout(new BorderLayout());
        table = new JTable(0,3);

        table.setModel(new DefaultTableModel(new Object[][]{{null,null,null}},new java.lang.String[]{"name","type","common"}));
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();

        for(Token t:tks){
            tableModel.addRow(new Object[]{t.value,t.type,t.common});
        }
        JScrollPane scrollPane = new JScrollPane(table);
        panelMain.add(scrollPane,BorderLayout.CENTER);

        container.add(panelMain);
    }
}
