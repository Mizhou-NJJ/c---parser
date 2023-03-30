package zhou.view;

import javax.swing.*;
import java.awt.*;

public class FrameProcessOfDeduce extends JFrame {
    private Container container;
    String strSelect;
    public FrameProcessOfDeduce (String strSelect){
        this.strSelect = strSelect;
    }
    public void start(){
        setSize(500,500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("产生式推导过程");
        setLocation(1000,0);
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
