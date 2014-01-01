package ai.context.util.analysis.feed;

import ai.context.core.ai.StateActionPair;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;

public class DashBoard extends JSplitPane{

    private HashMap<String, StateActionPair> population = new HashMap<>();
    private Workspace workspace;
    private JTable table = new JTable();
    private DefaultTableModel tableModel;
    public DashBoard(final Workspace workspace){
        super(JSplitPane.HORIZONTAL_SPLIT);

        this.workspace = workspace;
        setDividerSize(5);
        setDividerLocation(300);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(scroll);
        tableModel = new DefaultTableModel(new Object[]{"Score","Strategy ID"},0);
        table.setModel(tableModel);

        final Analytics analytics = new Analytics(workspace);
        add(analytics);

        JButton getAlpha = new JButton("Get Best Strategies");
        analytics.add(getAlpha);
        getAlpha.setBounds(10, 10, 150, 30);
        getAlpha.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                population.clear();
                tableModel.setRowCount(0);
                if(workspace.getLearner() != null){
                    Map<Double, StateActionPair> states = workspace.getLearner().getAlphas();
                    for(Map.Entry<Double, StateActionPair> entry : states.entrySet()){
                        population.put(entry.getValue().getId(), entry.getValue());
                        tableModel.addRow(new Object[]{entry.getKey(), entry.getValue().getId()});
                    }
                    table.repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void mouseExited(MouseEvent e) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(tableModel.getRowCount() > 0){
                    try{
                        analytics.update(population.get(table.getValueAt(table.getSelectedRow(), 1)));
                    }
                    catch (Exception ex){}
                }
            }
        });
    }
}
