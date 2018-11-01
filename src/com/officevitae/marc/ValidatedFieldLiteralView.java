package com.officevitae.marc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ValidatedFieldLiteralView<V> extends JPanel implements ValidatedFieldLiteralChangeListener{

	// MDH@01NOV2018: we're going to put a 1 second delay in there for actually registering the text
	private Timer updateLiteralTimer=null;
	private void runUpdateLiteralTimer(){
		try{
			if(updateLiteralTimer==null){
				updateLiteralTimer=new Timer(750,new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e){
						try{updateLiteral();}finally{updateLiteralTimer=null;}
					}
				});
				updateLiteralTimer.setRepeats(false);
				updateLiteralTimer.start();
			}else
				updateLiteralTimer.restart();
		}finally{
			// if we succeed in getting the timer to run, we should make the text turn blue and disable the check box while we're at it...
			// NOTE updateLiteral() will call updateView() which will then take care of enabling the check box when appropriate, so I do not do that in updateLiteral!!!
			if(updateLiteralTimer!=null&&updateLiteralTimer.isRunning()){checkBox.setEnabled(false);textField.setForeground(Color.BLUE);}
		}
	}

	private ValidatedFieldLiteral<V> validatedLiteral=null;
	private JCheckBox checkBox=null;

	private JTextField textField=null;

	// MDH@25OCT2018: for allowing selection of one of the texts!!!
	private JComboBox comboBox=null;
	private String[] options=null; // the texts to choose from

	private void enableTextField(){if(super.isEnabled())textField.setEnabled(true);}
	private void enableCheckBox(){if(super.isEnabled())checkBox.setEnabled(true);}
	private void enableComboBox(){if(super.isEnabled())comboBox.setEnabled(true);}
	private void updateView(){
		if(validatedLiteral!=null){
			if(textField!=null)enableTextField();
			if(comboBox!=null)enableComboBox();
			if(checkBox!=null){
				boolean valid=validatedLiteral.isValid();
				if(textField!=null)textField.setForeground(valid?Color.BLACK:Color.RED);
				boolean disabled=validatedLiteral.isDisabled();
				System.out.println("Updating '"+checkBox.getText()+"' from "+(valid?"valid":"invalid")+" and "+(disabled?"disabled":"enabled")+" literal.");
				// leave it off if disabled, otherwise valid determines whether on or off
				boolean selected=(disabled?false:valid);
				checkBox.setSelected(selected);
				// turning off should always be possible but when not selected turning off is only possible if we have a valid text to return to!!
				checkBox.setEnabled(isEnabled()&&(selected||validatedLiteral.getValidText()!=null));
			}
		}else{
			if(textField!=null)textField.setEnabled(false);
			if(comboBox!=null)comboBox.setEnabled(false);
			if(checkBox!=null)checkBox.setEnabled(false);
		}
	}

	private void updateLiteral(){
		if(validatedLiteral==null)return;
		if(textField!=null){validatedLiteral.setText(textField.getText());textField.setForeground(Color.BLACK);} // MDH@01NOV2018: show the text in black (again), to indicate that the literal was updated with the new text
		if(comboBox!=null)validatedLiteral.setText(comboBox.getSelectedItem().toString());
		validatedLiteral.updateField(); // the representation of the associated field might have to be changed!!!
		// shouldn't we enable the literal if it is currently valid?????
		// this is a problem in so far that if the user disables the literal it will be enabled on a change to a valid value!!!
		// when to allow the user to actually change the enabled property through the check box
		// shouldn't be allowed to turn it on when invalid
		//////////if(!validatedLiteral.isDisabled())validatedLiteral.setSelected(validatedLiteral.isValid());
		updateView();
	}

	public void setText(String text){
		if(text==null)return; // ignore (that's when nothing is selected!!)
		if(textField!=null)textField.setText(text);
		if(comboBox!=null&&((DefaultComboBoxModel)comboBox.getModel()).getIndexOf(text)>=0)comboBox.setSelectedItem(text);
		updateLiteral();
	}

	public void setEnabled(boolean enabled){
		super.setEnabled(enabled);
		checkBox.setEnabled(enabled);
		if(comboBox!=null)comboBox.setEnabled(enabled);
		if(textField!=null)textField.setEnabled(enabled);
	}

	private void createView(String checkBoxTitle){
		if(checkBoxTitle!=null){
			JPanel checkBoxPanel=new JPanel(new BorderLayout());
			checkBoxPanel.add(checkBox=new JCheckBox(),BorderLayout.WEST);
			checkBoxPanel.add(new JLabel(checkBoxTitle)); // separate so it won't be disabled when the checkbox is!!!!
			super.add(checkBoxPanel,BorderLayout.WEST);
			checkBox.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e){
					validatedLiteral.setDisabled(!checkBox.isSelected()); // turning off means disabling, turning on means enabling
					// if enabled but an invalid literal, make the literal valid again (as should be possible)
					// as the text can only change due to interaction with this view, however there's a problem with the text showing in the
					// so perhaps we're going to need a textChanged() event as well??? We already have a checkChanged called in setText()
					if(!validatedLiteral.isDisabled()&&!validatedLiteral.isValid()){
						validatedLiteral.makeValid();
						if(textField!=null){
							textField.setText(validatedLiteral.getText());
							textField.setForeground(Color.BLACK); // calling updateView() is overkill, given that the text shown is valid text, the foreground should be black!!!
						}
						if(comboBox!=null){
							comboBox.setSelectedItem(validatedLiteral.getText());
						}
					}
					validatedLiteral.updateField();
				}
			});
			checkBox.setEnabled(false); // until a ValidatedFieldLiteral is plugged in
		}
		if(options==null){
			super.add(textField=new JTextField());
			textField.addKeyListener(new KeyAdapter(){
				@Override
				public void keyReleased(KeyEvent e){
					// MDH@01NOV2018: delay registering the text change for 1 second after stopping with typing
					runUpdateLiteralTimer(); // replacing: updateLiteral();
				}
			});
			textField.setEnabled(false); // waiting for a validated literal!!
		}else{
			super.add(comboBox=new JComboBox(new DefaultComboBoxModel(options)));
			/////////comboBox.insertItemAt("",0); // user should be allowed to not select an option (because options are the valid options!!)
			comboBox.addItemListener(new ItemListener(){
				@Override
				public void itemStateChanged(ItemEvent e){
					updateLiteral();
				}
			});
			comboBox.setEnabled(false); // waiting for a validated literal!!
		}
	}

	// ValidatedFieldLiteralChangeListener implementation
	public void invalidated(ValidatedFieldLiteral validatedLiteral){
		updateView();
	}

	public void enableChanged(ValidatedFieldLiteral validatedLiteral){
		updateView();
	}

	public ValidatedFieldLiteralView<V> setValidatedFieldLiteral(ValidatedFieldLiteral<V> validatedLiteral){
		if(this.validatedLiteral!=null) this.validatedLiteral.removeValidatedFieldLiteralChangeListener(this);
		this.validatedLiteral=validatedLiteral;
		if(this.validatedLiteral!=null){
			this.validatedLiteral.addValidatedFieldLiteralChangeListener(this);
			// I have to show the text obviously
			String validatedLiteralText=this.validatedLiteral.getText();
			if(textField!=null)textField.setText(validatedLiteralText);
			if(comboBox!=null&&((DefaultComboBoxModel)comboBox.getModel()).getIndexOf(validatedLiteralText)>=0)comboBox.setSelectedItem(validatedLiteralText);
		}else{ // no validated literal anymore
			if(textField!=null)textField.setText("");
			if(comboBox!=null)comboBox.setSelectedItem(""); // assuming it is there (as it should)
		}
		updateView();
		return this;
	}

	// suppose we want the user to select from a list of possible values????
	public ValidatedFieldLiteralView(String checkBoxTitle,String[] options){
		super(new BorderLayout());
		if(options!=null&&options.length>0)this.options=options;
		createView(checkBoxTitle);
	}

	public ValidatedFieldLiteralView(String checkBoxTitle){
		this(checkBoxTitle,null);
	}

}
