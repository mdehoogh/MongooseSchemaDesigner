package com.officevitae.marc;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class OptionView<T> extends JPanel{

	private JButton infoButton=null,resetButton=null;
	private JCheckBox checkBox=null;
	private JTextField textField=null;

	private T initialValue=null;
	private void updateResetButton(T value){if(resetButton!=null)resetButton.setEnabled(!initialValue.equals(value));}
	// call showValue() to update the view (e.g. when the value changed externally somehow!!)
	public void showValue(){
		T value=option.getValue();
		if(checkBox!=null)checkBox.setSelected((Boolean)value);
		if(textField!=null)textField.setText(value.toString());
		// only when the current value is not equal to the initial value
		updateResetButton(value);
	}
	private JComponent getContentsView(){
		JPanel contentsPanel=new JPanel(new BorderLayout());
		// for now we check the type
		if(initialValue instanceof Boolean){
			contentsPanel.add(checkBox=new JCheckBox(option.getName(),(Boolean)initialValue),BorderLayout.WEST);
			checkBox.addChangeListener(new ChangeListener(){
				@Override
				public void stateChanged(ChangeEvent e){
					try{
						option.parseValue(checkBox.isSelected()?Boolean.TRUE.toString():Boolean.FALSE.toString());
						updateResetButton(option.getValue());
					}catch(Exception ex){
						Utils.setInfo(this,"Invalid boolean option value.");
						showValue();
					}
				}
			});
		}else{
			contentsPanel.add(new JLabel("  "+option.getName()+": "),BorderLayout.WEST);
			contentsPanel.add(textField=new JTextField(initialValue.toString()));
			textField.addKeyListener(new KeyAdapter(){
				@Override
				public void keyReleased(KeyEvent e){
					try{
						option.parseValue(textField.getText());
						updateResetButton(option.getValue());
					}catch(Exception ex){
						Utils.setInfo(this,"Invalid option value text '"+textField.getText()+"'.");
						showValue();
					}
				}
			});
		}
		return contentsPanel;
	}
	private void createView(){
		if(option==null)return; // no option to show at all!!!
		initialValue=option.getValue();
		if(initialValue!=null){ // shouldn't happen though!!
			add(resetButton=new JButton("Reset"),BorderLayout.WEST);
			resetButton.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					try{
						option.setValue(initialValue);
					}finally{
						showValue();
					}
				}
			});
			resetButton.setEnabled(false); // initially disabled of course
		}
		add(getContentsView());
		if(option.getInfo()!=null){
			add(infoButton=new JButton("?"),BorderLayout.EAST);
			infoButton.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					JOptionPane.showMessageDialog(null,option.getInfo());
				}
			});
		}
	}
	public Option<T> option=null;
	public OptionView(Option<T> option){
		super(new BorderLayout());
		this.option=option;
		createView();
	}

}
