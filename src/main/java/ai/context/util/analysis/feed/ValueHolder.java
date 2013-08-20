package ai.context.util.analysis.feed;

import ai.context.util.common.DraggableComponent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.lang.reflect.Array;

public class ValueHolder extends DraggableComponent{

    enum TYPE{
        DOUBLE,
        STRING,
        INTEGER,
        LONG,
        CUSTOM
    }
    private TYPE type;
    private  String name;
    private JTextField value;
    private JCheckBox select;

    public ValueHolder(TYPE type, String name) {

        setLayout(new GridLayout(3, 1));
        setSize(100, 60);
        this.type = type;
        this.name = name;
        add(new JLabel(name));
        select = new JCheckBox();
        add(select);
        select.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                AbstractButton abstractButton =
                        (AbstractButton) e.getSource();
                ButtonModel buttonModel = abstractButton.getModel();
                boolean selected = buttonModel.isSelected();
                select(selected);
            }
        });
        value = new JTextField();
        value.setSize(100, 20);
        add(value);
    }

    public Object getValue(){
        try{
            if(type == TYPE.DOUBLE){
                return Double.parseDouble(value.getText());
            }
            else if(type == TYPE.INTEGER){
                return Integer.parseInt(value.getText());
            }
            else if(type == TYPE.LONG){
                return Long.parseLong(value.getText());
            }
            else if(type == TYPE.CUSTOM){
                String[] data = value.getText().split("::");

                String collectionType = data[0];
                String dataType = data[1];
                String[] values = data[2].split(",");

                if(collectionType.equals("VALUE")){
                    Class clazz = Class.forName(dataType + "." + values[0]);
                    return clazz.newInstance();
                }
                else if(collectionType.equals("ARRAY")){
                    Class clazz = Class.forName(dataType);
                    Object array = Array.newInstance(clazz, values.length);
                    for(int i = 0; i < values.length; i++){
                       if(clazz.isEnum()){
                           Array.set(array, i, Enum.valueOf(clazz, values[i]));
                       }
                    }
                    return array;
                }
                else if(collectionType.equals("LIST")){

                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return value.getText();
    }


    public void select(boolean selected){
        area.setSelected(this, selected);
    }
    public void deselect(){
        select.setSelected(false);
    }

    public String toString(){
        return "VALUEHOLDER¬>" + System.identityHashCode(this) + "¬>" + type + "¬>" + name + "¬>" + value.getText();
    }

    public void setValue(String value){
        this.value.setText(value);
    }
}
