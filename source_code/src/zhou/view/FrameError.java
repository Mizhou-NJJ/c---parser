package zhou.view;

import javax.swing.*;
import java.awt.*;

public class FrameError extends JFrame {
    private Container container;
    String strSelect;
    public FrameError (String strSelect){
        this.strSelect = strSelect;
    }
    public void start(String title){
        setSize(500,500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle(title);
        setLocationRelativeTo(null);
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
