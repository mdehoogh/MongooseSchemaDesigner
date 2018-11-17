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

	/* MDH@16NOV2018: Option will inform its OptionCollection and so up and up...
	// if one of the reset buttons is enabled, the option view apparently changed...
	public interface ChangeListener{
		void optionChanged(Option option);
		void optionUnchanged(Option option);
	}
	private ChangeListener changeListener=null;
	private void informChangeListener(){
		try{if(resetButton.isEnabled())changeListener.optionChanged(option);else changeListener.optionUnchanged(option);}catch(Exception ex){}
	}
	public void setChangeListener(ChangeListener changeListener){
		this.changeListener=changeListener;
		informChangeListener();
	}
	*/
	private JButton infoButton=null,resetButton=null,defaultButton=null,collectionButton=null;
	private JCheckBox checkBox=null;
	private JTextField textField=null;
	@Override
	public void setEnabled(boolean enabled){
		super.setEnabled(enabled); // TODO should we do this????
		// are we going to show all contained components enabled/disabled? yes, except the info button!!!!
		// careful here though, the reset button is only to be disabled (not enabled when it currently isn't)
		if(resetButton!=null)if(!enabled)resetButton.setEnabled(false);
		if(checkBox!=null)checkBox.setEnabled(enabled);
		if(textField!=null)textField.setEnabled(enabled);
	}
	private void updateResetButton(T value){
		boolean resetable=(option!=null&&option.isChanged()); // most convenient...
		if(option!=null)
			Utils.consoleprintln("Option "+option.getName()+" is "+(resetable?"":"NOT ")+"resettable.");
		if(resetButton!=null){
			resetButton.setEnabled(resetable);
			//////informChangeListener();
		}
	}
	private void updateButtons(){
		resetButton.setEnabled(option.differsFromInitialValue());
		defaultButton.setEnabled(option.differsFromOptionDefault());
		collectionButton.setVisible(option.hasParent());
		collectionButton.setEnabled(option.differsFromParentDefault());
	}
	private T optionValue;
	// call showValue() to update the view (e.g. when the value changed externally somehow!!)
	public void showValue(){
		T value=option.getValue();
		if(checkBox!=null)checkBox.setSelected((Boolean)value);
		if(textField!=null)textField.setText(value.toString());
		// only when the current value is not equal to the initial value
		updateButtons();
	}
	private JComponent getContentsView(){
		JPanel contentsPanel=new JPanel(new BorderLayout());
		// for now we check the type
		if(optionValue instanceof Boolean){
			contentsPanel.add(checkBox=new JCheckBox(option.getName(),(Boolean)optionValue),BorderLayout.WEST);
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
			contentsPanel.add(textField=new JTextField(optionValue.toString()));
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
	void checkReset(){
		updateResetButton(option.getValue());
	}
	private JComponent getButtonView(){
		Box buttonBox=Box.createHorizontalBox();
		buttonBox.add(defaultButton=new JButton("Default"),BorderLayout.WEST);
		defaultButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try{
					option.resetToOptionDefault();
				}finally{
					showValue();
				}
			}
		});
		defaultButton.setEnabled(false); // initially disabled of course
		buttonBox.add(collectionButton=new JButton("Collection"),BorderLayout.WEST);
		collectionButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try{
					option.resetToParentDefault();
				}finally{
					showValue();
				}
			}
		});
		collectionButton.setEnabled(false); // initially disabled of course
		buttonBox.add(resetButton=new JButton("Reset"),BorderLayout.WEST);
		resetButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				try{
					option.resetToInitialValue();
				}finally{
					showValue();
				}
			}
		});
		resetButton.setEnabled(false); // initially disabled of course
		return buttonBox;
	}
	private void createView(){
		if(option==null)return; // no option to show at all!!!
		optionValue=option.getValue();
		if(optionValue!=null){ // shouldn't happen though!!
			add(getButtonView(),BorderLayout.WEST);
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
		showValue();
	}
	public Option<T> option=null;
	public OptionView(Option<T> option){
		super(new BorderLayout());
		this.option=option;
		createView();
	}

}
