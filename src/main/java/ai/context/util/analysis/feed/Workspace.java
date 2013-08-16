package ai.context.util.analysis.feed;

import au.com.bytecode.opencsv.CSVReader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TreeMap;

public class Workspace {

    private static TreeMap<String, FeedTemplate> feedTemplates = new TreeMap<>();

    public static void main(String[] args){

        try {
            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream("C:\\Dev\\Source\\CALM\\src\\main\\resources\\Transformers.csv")), ',', '"', 1);
            String[] line;
            while((line = reader.readNext()) != null){

                String name = line[1];
                String className = line[0];
                String[] arguments = line[2].split(";");

                feedTemplates.put(name, new FeedTemplate(className, name, arguments));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Workspace");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800, 620);
        frame.setVisible(true);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        splitPane.setDividerSize(5);
        splitPane.setDividerLocation(200);
        frame.add(splitPane);

        final WorkArea workArea = new WorkArea(null);
        workArea.setLayout(null);

        JPanel control = new JPanel();
        control.setLayout(new GridLayout(0, 1));

        control.add(new JLabel("Control Panel"));
        control.add(new JLabel(""));
        control.add(new JLabel("Select a Feed or Transformer"));

        Object[] templates = feedTemplates.keySet().toArray();

        final JComboBox transfomers = new JComboBox(templates);

        transfomers.setSize(200, 20);
        control.add(transfomers);

        JButton addTransformer = new JButton("Add Transformer");
        control.add(addTransformer);

        addTransformer.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if(transfomers.getSelectedItem() != null){
                    workArea.add(Transformer.build(feedTemplates.get(transfomers.getSelectedItem())));
                }
                workArea.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });

        control.add(new JLabel(""));
        control.add(new JLabel("Add a Value Variable"));

        final JTextField varName = new JTextField("Variable Name");
        control.add(varName);
        Object[] types = ValueHolder.TYPE.values();
        final JComboBox valVar = new JComboBox(types);
        control.add(valVar);

        JButton addValVar = new JButton("Add Value Variable");
        control.add(addValVar);
        addValVar.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if(valVar.getSelectedItem() != null){
                    workArea.add(new ValueHolder(ValueHolder.TYPE.valueOf(valVar.getSelectedItem().toString()), varName.getText()));
                }
                workArea.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });

        control.add(new JLabel(""));
        JButton removeSelected = new JButton("Remove Selected");
        control.add(removeSelected);
        removeSelected.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

                workArea.removeSelected();
                workArea.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });

        control.add(new JLabel(""));
        JButton buildFeed = new JButton("Build Selected Feed");
        control.add(buildFeed);
        buildFeed.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

                workArea.buildSelected();
                workArea.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });

        splitPane.add(control, 0);
        splitPane.add(workArea, 1);
    }
}
