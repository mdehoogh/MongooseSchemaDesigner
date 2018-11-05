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

	private JButton infoButton;
	private JCheckBox checkBox=null;
	private JTextField textField=null;

	private T initialValue=null;
	private void createView(){
		initialValue=option.getValue();
		if(initialValue==null)return; // shouldn't happen though
		// for now we check the type
		if(initialValue instanceof Boolean){
			add(checkBox=new JCheckBox(option.getName(),(Boolean)initialValue),BorderLayout.WEST);
			checkBox.addChangeListener(new ChangeListener(){
				@Override
				public void stateChanged(ChangeEvent e){
					try{
						option.parseValue(checkBox.isSelected()?Boolean.TRUE.toString():Boolean.FALSE.toString());
					}catch(Exception ex){
						Utils.setInfo(this,"Invalid boolean option value.");
					}
				}
			});
		}else{
			add(new JLabel("  "+option.getName()+": "),BorderLayout.WEST);
			add(textField=new JTextField(initialValue.toString()));
			textField.addKeyListener(new KeyAdapter(){
				@Override
				public void keyReleased(KeyEvent e){
					try{
						option.parseValue(textField.getText());
					}catch(Exception ex){
						Utils.setInfo(this,"Invalid option value text '"+textField.getText()+"'.");
					}
				}
			});
		}
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
