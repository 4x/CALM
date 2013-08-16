package ai.context.util.analysis.feed;

import ai.context.util.common.DraggableComponent;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class WorkArea extends JPanel {

    private Graphics2D g2d;
    private int w;
    private int h;

    private Workspace workspace;
    private Set<DraggableComponent> components = new HashSet<>();
    private DraggableComponent selected;

    public WorkArea(Workspace workspace){
        this.workspace = workspace;
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);

        g2d = (Graphics2D) g;
        g2d.setColor(Color.BLUE);

        Dimension size = getSize();
        Insets insets = getInsets();

        w = size.width - insets.left - insets.right;
        h = size.height - insets.bottom - insets.top;

        g2d.clearRect(0, 0, w, h);

        for(DraggableComponent component : components){
            if(component instanceof Transformer){
                ConstructorArgument[] args = ((Transformer)component).getArguments();
                boolean expanded = ((Transformer)component).isExpanded();
                int dH = component.getHeight() / (3 + 2 * args.length);
                for(int i = 0; i < args.length; i++){
                    if (args[i] != null && args[i].getType() == ConstructorArgument.TYPE.REFERENCE){
                        DraggableComponent hook = (DraggableComponent)args[i].getValue();

                        if(expanded){
                            g2d.drawLine(hook.getX() + hook.getWidth(), hook.getY() + hook.getHeight()/2, component.getX(), component.getY() + (2 * i + 4) * dH + dH/2);
                        }
                        else {
                            g2d.drawLine(hook.getX() + hook.getWidth(), hook.getY() + hook.getHeight()/2, component.getX(), component.getY() + component.getHeight()/2);
                        }
                    }
                }
            }
        }
    }

    public void update(){

        this.repaint();
    }

    public void add(DraggableComponent component){
        super.add(component);
        components.add(component);
        component.setWorkArea(this);
    }

    public void remove(DraggableComponent component){
        super.remove(component);
        components.remove(component);
    }

    public void setSelected(DraggableComponent component, boolean isSelected){
        selected = null;
        for(DraggableComponent draggableComponent : components){
            if(draggableComponent != component){
                draggableComponent.deselect();
            }
        }
        if(isSelected){
            selected = component;
        }
    }

    public DraggableComponent getSelected() {
        return selected;
    }

    public void removeSelected(){
        if(selected != null){
            remove(selected);
        }
        selected = null;
    }

    public void buildSelected(){
        for(DraggableComponent draggableComponent : components){
            if(draggableComponent instanceof Transformer){
                ((Transformer) draggableComponent).resetFeed();
            }
        }

        if(selected != null){
            try {
                ((Transformer) selected).getFeed();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }
}