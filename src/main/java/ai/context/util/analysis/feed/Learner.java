package ai.context.util.analysis.feed;

import ai.context.core.StateActionPair;
import ai.context.feed.Feed;
import ai.context.feed.predictor.PredictionExtractionFeed;
import ai.context.learning.LearnerWrapper;
import ai.context.util.common.DraggableComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Learner extends DraggableComponent {

    private boolean expanded = false;

    private JCheckBox select;
    private LearnerWrapper wrapper;

    private Feed signalFeed;
    private Feed dataFeed;

    private JButton pause = new JButton("Pause/UnPause");
    private Learner thisLearner;

    private Component[] hideables;
    private ConstructorArgument[] arguments = new ConstructorArgument[2];
    private Set<PredictionExtractionFeed> extractors = new HashSet<>();
    private Workspace workspace;

    private JTextField closeField = new JTextField();
    private JTextField valuesField = new JTextField();
    private JTextField horizonField = new JTextField();
    private JTextField resolutionField = new JTextField();
    private JTextField toleranceField = new JTextField();
    private JTextField maxPopField = new JTextField();

    public Learner(final Workspace workspace) {

        thisLearner = this;
        this.workspace = workspace;
        setLayout(new GridLayout(0, 1));
        setSize(100, 100);
        add(new JLabel("Learner"));
        select = new JCheckBox();
        add(select);
        select.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                AbstractButton abstractButton =
                        (AbstractButton)e.getSource();
                ButtonModel buttonModel = abstractButton.getModel();
                boolean selected = buttonModel.isSelected();
                select(selected);
            }
        });
        final JButton expand = new JButton("Expand");
        add(expand);
        expand.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                expanded = !expanded;
                if(expanded){
                    expand.setText("Minimise");
                    setSize(200, 400);
                    toggle(true);
                }
                else {
                    expand.setText("Expand");
                    setSize(100, 100);
                    toggle(false);
                }
                repaint();
                area.repaint();
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

        final JButton start = new JButton("Start Learning");
        add(start);
        start.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

                try {
                    start();
                    workspace.setLearner(thisLearner);
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                } catch (InvocationTargetException e1) {
                    e1.printStackTrace();
                } catch (InstantiationException e1) {
                    e1.printStackTrace();
                }
                repaint();
                area.repaint();
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

        add(pause);
        pause.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(wrapper != null){
                    if(wrapper.isPaused()){
                        pause.setText("Pause");
                        wrapper.setPaused(false);
                        setBackground(Color.MAGENTA);
                    }
                    else {
                        pause.setText("UnPause");
                        wrapper.setPaused(true);
                        setBackground(Color.GRAY);
                    }
                    workspace.setLearner(thisLearner);
                    repaint();
                    area.repaint();
                }
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


        String[] argsDescription = new String[]{
                "Signal Feed",
                "Values Feed",
                "Present Close Field",
                "Value Fields (Comma Separated)",
                "Horizon",
                "Resolution",
                "Tolerance (0 - 1)",
                "Max Pop of Strategies"
        };
        int i = 0;
        for(String arg : argsDescription){
            add(new JLabel(arg));
            final int index = i;
            if(index < 2){
                final JButton hook = new JButton("Hook");
                add(hook);
                hook.addMouseListener(new MouseListener() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if(hook.getText().equals("Hook")){
                            if(area.getSelected() != null){
                                arguments[index] = new ConstructorArgument(ConstructorArgument.TYPE.REFERENCE, area.getSelected());
                                hook.setText("Unhook");
                            }
                        }
                        else{
                            hook.setText("Hook");
                            arguments[index] = null;
                        }
                        area.repaint();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {}

                    @Override
                    public void mouseReleased(MouseEvent e) { }

                    @Override
                    public void mouseEntered(MouseEvent e) { }

                    @Override
                    public void mouseExited(MouseEvent e) { }

                });
            }
            else if(index == 2){
                add(closeField);
            }
            else if(index == 3){
                add(valuesField);
            }
            else if(index == 4){
                add(horizonField);
            }
            else if(index == 5){
                add(resolutionField);
            }
            else if(index == 6){
                add(toleranceField);
            }
            else if(index == 7){
                add(maxPopField);
            }
            i++;
        }
        toggle(false);
        setBackground(Color.GRAY);
    }

    public void start() throws IllegalAccessException, InvocationTargetException, InstantiationException {

        if(wrapper != null){
            wrapper.kill();
        }

        Set<Feed> parents = new HashSet<>();
        Object[] arguments = new Object[this.arguments.length];
        int i = 0;
        for(ConstructorArgument argument : this.arguments){

            if(argument == null){
                arguments[i] = null;
            }
            else if(argument.getType() == ConstructorArgument.TYPE.VALUE){
                arguments[i] = argument.getValue();
            }
            else{
                if(argument.getValue() instanceof Transformer){
                    arguments[i] = ((Transformer)argument.getValue()).getFeed();
                    parents.add((Feed) arguments[i]);
                }
                else if(argument.getValue() instanceof ValueHolder){
                    arguments[i] = ((ValueHolder)argument.getValue()).getValue();
                }
                else {
                    arguments[i] = argument.getValue();
                }
            }
            i++;
        }
        int close = Integer.parseInt(closeField.getText());
        int horizon = Integer.parseInt(horizonField.getText());
        double res = Double.parseDouble(resolutionField.getText());
        double tol = Double.parseDouble(toleranceField.getText());
        int maxPop = Integer.parseInt(maxPopField.getText());
        String[] valStr = valuesField.getText().split(",");
        int[] values = new int[valStr.length];
        for(int iV = 0; iV < values.length; iV++){
            values[iV] = Integer.parseInt(valStr[iV]);
        }
        wrapper = new LearnerWrapper((Feed)arguments[0], (Feed)arguments[1], close, values, horizon, res, tol, maxPop);
        signalFeed = (Feed) arguments[0];
        dataFeed = (Feed) arguments[1];
        for(PredictionExtractionFeed extractor : extractors){
            wrapper.addExtractor(extractor);
        }
        wrapper.start();
        pause.setText("Pause");

        setBackground(Color.MAGENTA);
        repaint();
    }

    public void select(boolean selected){
        area.setSelected(this, selected);
    }
    public void deselect(){
        select.setSelected(false);
    }

    private void toggle(boolean show){
        if(!show){
            hideables = new Component[getComponents().length - 5];
            for(int cN = 0; cN < getComponents().length; cN++){
                if(cN > 4){
                    hideables[cN - 5] = getComponent(cN);
                }
            }
            for(Component component : hideables){
                remove(component);
            }
        }
        else{
            for(Component component : hideables){
                add(component);
            }
        }
        revalidate();
    }
    public boolean isExpanded() {
        return expanded;
    }

    public ConstructorArgument[] getArguments() {
        return arguments;
    }

    public Map<Double, StateActionPair> getAlphas(){
        return wrapper.getAlphas();
    }

    public Map<Double, Integer> getFactorInfluences(StateActionPair state){
        return wrapper.getFactorInfluences(state);
    }

    public String toString(){

        String data = "";
        for(ConstructorArgument argument : arguments){
            if(argument != null){
                data += System.identityHashCode(argument.getValue());
            }
            data += ";";
        }

        return "LEARNER¬>" + System.identityHashCode(this) + "¬>" + data + closeField.getText() + ";" + valuesField.getText() + ";" + horizonField.getText() + ";" + resolutionField.getText() + ";" + toleranceField.getText() + ";" + maxPopField.getText();
    }

    public void configure(String config){
        String id = config.split("¬>")[1];
        String[] parts = config.split("¬>")[2].split(";");

        arguments[0] = new ConstructorArgument(ConstructorArgument.TYPE.REFERENCE, ObjectHolder.get(parts[0]));
        arguments[1] = new ConstructorArgument(ConstructorArgument.TYPE.REFERENCE, ObjectHolder.get(parts[1]));

        closeField.setText(parts[2]);
        valuesField.setText(parts[3]);
        horizonField.setText(parts[4]);
        resolutionField.setText(parts[5]);
        toleranceField.setText(parts[6]);
        maxPopField.setText(parts[7]);

        ObjectHolder.save(id, thisLearner);
    }

    public void addExtractor(PredictionExtractionFeed extractor){
        extractors.add(extractor);
    }

    public Feed getSignalFeed(){
        return signalFeed;
    }

    public Feed getDataFeed(){
        return dataFeed;
    }
}

