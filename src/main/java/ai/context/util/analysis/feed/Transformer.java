package ai.context.util.analysis.feed;

import ai.context.feed.Feed;
import ai.context.util.common.DraggableComponent;
import ai.context.util.learning.AmalgamateUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class Transformer extends DraggableComponent{

    private Constructor constructor;
    private boolean expanded = false;

    private ConstructorArgument[] arguments;

    private Feed feed;

    private JCheckBox select;

    private Component[] hideables;

    private String className;
    private String name;
    private String[] argDescription;

    public static Transformer build(FeedTemplate template){
        try {
            return new Transformer(template.getClassName(), template.getName(), template.getArgments());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Transformer(String className, String name, String[] argsDescription) throws ClassNotFoundException {
        this.className = className;
        this.name = name;
        this.argDescription = argsDescription;

        Class<?> c = Class.forName(className);
        Constructor[] allConstructors = c.getDeclaredConstructors();
        constructor = allConstructors[0];

        setLayout(new GridLayout(0, 1));
        setSize(100, 60);
        add(new JLabel(name));
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
                    setSize(200, 300);
                    toggle(true);
                }
                else {
                    expand.setText("Expand");
                    setSize(100, 60);
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

        this.arguments = new ConstructorArgument[argsDescription.length];

        int i = 0;
        for(String arg : argsDescription){
            add(new JLabel(arg));
            final int index = i;
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
            i++;
        }
        toggle(false);
        setBackground(Color.RED);
    }

    public Feed getFeed() throws IllegalAccessException, InvocationTargetException, InstantiationException {

        if(feed != null){
            return feed;
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
        feed = (Feed) constructor.newInstance(arguments);

        for(Feed parent : parents){
            parent.addChild(feed);
        }
        setBackground(Color.GREEN);
        repaint();
        return feed;
    }

    public void select(boolean selected){
        area.setSelected(this, selected);
    }
    public void deselect(){
        select.setSelected(false);
    }

    private void toggle(boolean show){
        if(!show){
            hideables = new Component[getComponents().length - 3];
            for(int cN = 0; cN < getComponents().length; cN++){
                if(cN > 2){
                    hideables[cN - 3] = getComponent(cN);
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

    public ConstructorArgument[] getArguments() {
        return arguments;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void resetFeed(){
        feed = null;
        setBackground(Color.ORANGE);
        repaint();
    }

    public String toString(){

        String data = "";
        for(ConstructorArgument argument : arguments){
            if(argument != null){
                data += System.identityHashCode(argument.getValue());
            }
            data += ";";
        }

        return "TRANSFORMER¬>" + System.identityHashCode(this) + "¬>" + className + "¬>" + name + "¬>" + AmalgamateUtils.getSCSVString(argDescription) + "¬>" + data;
    }

    public void configure(String config){
        String id = config.split("¬>")[1];
        String[] parts = config.split("¬>")[5].split(";");

        for(int i = 0; i < parts.length; i++){
            arguments[i] = new ConstructorArgument(ConstructorArgument.TYPE.REFERENCE, ObjectHolder.get(parts[i]));
        }

        ObjectHolder.save(id, this);
    }
}
