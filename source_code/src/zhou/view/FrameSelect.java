package zhou.view;

import zhou.cfg.BaseSymbol;
import zhou.cfg.ProductionItem;


import javax.swing.*;

import java.awt.*;

import java.util.List;

public class FrameSelect extends JFrame {

    private Container container;
    String strSelect;
    public FrameSelect (List<ProductionItem> items){
        String preV = "";
        int productIndex = 1;
        StringBuilder builder = new StringBuilder();
        for (ProductionItem item:items){
//                StringBuilder builder = new StringBuilder();
            builder.append("SELECT( ");
            builder.append(item.left.v);
            if (!item.left.v.equals(preV)) {
                productIndex = 1;
            }else {
                productIndex++;
            }
            builder.append("_"+productIndex);
            builder.append(" )={ ");
            for(BaseSymbol symbol:item.select){
                builder.append(symbol.v+"  ");
            }
            builder.append(" }");
            builder.append("\n");
            preV = item.left.v;
        }
        this.strSelect = builder.toString();
    }
    public void start(){
        setSize(500,500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("预测分析表");
        setLocation(501,0);
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
