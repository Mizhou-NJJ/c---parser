package zhou.view;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FrameCode extends JFrame {
    private Container container;
    String strSelect;
    public FrameCode (List<String> codes){
        StringBuilder b = new StringBuilder();
        for (String code:codes) b.append(code+"\n");
        this.strSelect = b.toString();
    }
    public void start(){
        setSize(500,500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("中间代码");
        setLocation(600,300);
        init();
        setVisible(true);
    }
    public static  JTextPane jTextPane;
    private void init(){
        container = getContentPane();
        JPanel panelMain = new JPanel();
        panelMain.setLayout(new BorderLayout());
        jTextPane = new JTextPane();
        jTextPane.setEditable(false);


        jTextPane.setText(strSelect);

        JScrollPane scrollPane = new JScrollPane(jTextPane);
        panelMain.add(scrollPane,BorderLayout.CENTER);

        container.add(panelMain);
    }
}
