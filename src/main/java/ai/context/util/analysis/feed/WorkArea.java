package ai.context.util.analysis.feed;

import ai.context.feed.Feed;
import ai.context.feed.FeedObject;
import ai.context.feed.synchronised.SynchronisableFeed;
import ai.context.util.DataSetUtils;
import ai.context.util.common.DraggableComponent;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

public class WorkArea extends JPanel {

    private boolean play = false;

    private Graphics2D g2d;
    private int w;
    private int h;

    private Workspace workspace;
    private Set<DraggableComponent> components = new HashSet<>();
    private DraggableComponent selected;

    private HashMap<Feed, Transformer> transformers = new HashMap<>();
    private List chain = new ArrayList();

    public WorkArea(Workspace workspace){
        this.workspace = workspace;
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);

        g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(1));


        Dimension size = getSize();
        Insets insets = getInsets();

        w = size.width - insets.left - insets.right;
        h = size.height - insets.bottom - insets.top;

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, w, h);
        g2d.setColor(Color.BLUE);

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
            else if(component instanceof Learner){
                ConstructorArgument[] args = ((Learner)component).getArguments();
                boolean expanded = ((Learner)component).isExpanded();
                int dH = component.getHeight() / (5 + 2 * args.length);
                for(int i = 0; i < args.length; i++){
                    if (args[i] != null && args[i].getType() == ConstructorArgument.TYPE.REFERENCE){
                        DraggableComponent hook = (DraggableComponent)args[i].getValue();

                        if(expanded){
                            g2d.drawLine(hook.getX() + hook.getWidth(), hook.getY() + hook.getHeight()/2, component.getX(), component.getY() + (2 * i + 6) * dH + dH/2);
                        }
                        else {
                            g2d.drawLine(hook.getX() + hook.getWidth(), hook.getY() + hook.getHeight()/2, component.getX(), component.getY() + component.getHeight()/2);
                        }
                    }
                }
            }
        }

        chain(chain);
    }

    public void chain(List chain){
        if(chain.isEmpty()){
            return;
        }

        g2d.setStroke(new BasicStroke(5));
        g2d.setColor(Color.RED);

        Feed origin = (Feed) chain.get(0);
        Transformer tO = transformers.get(origin);
        for(int i = 1; i < chain.size(); i++){
            List subChain = (List)(chain.get(i));
            Feed destination = (Feed) (subChain).get(0);
            Transformer tD = transformers.get(destination);
            g2d.drawLine(tO.getX(), tO.getY() + tO.getHeight()/2, tD.getX() + tO.getWidth(), tD.getY()  + tO.getHeight()/2);

            chain(subChain);
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

    public void playSelected(){
        if(selected != null && selected instanceof Transformer){
            play = true;

            try{
                final Feed feed = ((Transformer)selected).getFeed();
                boolean composite = false;
                if(feed instanceof SynchronisableFeed){
                    composite = true;
                }

                final boolean synch = composite;

                Runnable player = new Runnable() {
                    @Override
                    public void run() {
                        while (play){
                            try{
                                FeedObject data = null;

                                if(!synch){
                                    data = feed.readNext(this);
                                }
                                else {
                                    data = ((SynchronisableFeed)feed).getNextComposite(this);
                                }

                                if(data != null){
                                    ArrayList list = new ArrayList();
                                    DataSetUtils.add(data.getData(), list);
                                    String toAppend = new Date(data.getTimeStamp()) + ": " + list;
                                    workspace.append(toAppend);
                                }
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                };

                new Thread(player).start();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void buildSelected(){
        chain.clear();
        transformers.clear();
        for(DraggableComponent draggableComponent : components){
            if(draggableComponent instanceof Transformer){
                ((Transformer) draggableComponent).resetFeed();
            }
        }

        if(selected != null){
            try {
                ((Transformer) selected).getFeed();

                for(DraggableComponent draggableComponent : components){
                    if(draggableComponent instanceof Transformer){
                        Transformer transformer = ((Transformer) draggableComponent);
                        if(transformer.getFeed() != null){
                            transformers.put(transformer.getFeed(), transformer);
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    public void drawChain(int element){
        if(selected != null && selected instanceof Transformer){
            try {
                chain = ((Transformer)selected).getFeed().getElementChain(element);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }

        workspace.append("Chain: " + chain);
        repaint();
    }

    public boolean isPlaying(){
        return play;
    }

    public void stop(){
        play = false;
    }
}