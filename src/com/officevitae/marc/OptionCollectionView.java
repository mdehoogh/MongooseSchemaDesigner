package com.officevitae.marc;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class OptionCollectionView extends JPanel{
	private boolean editable=true;
	public void setEditable(boolean editable){
		this.editable=editable;
		// pass along to the option views
		int optionIndex=optionsBox.getComponentCount();
		while(--optionIndex>=0)optionsBox.getComponent(optionIndex).setEnabled(this.editable);
	}
	/*
	private void setOptionChangeListener(OptionView.ChangeListener optionViewChangeListener){
		// plug in the option view change listener into all option views
		int optionIndex=optionsBox.getComponentCount();
		while(--optionIndex>=0)((OptionView)optionsBox.getComponent(optionIndex)).setChangeListener(optionViewChangeListener);
	}
	*/
	private Box optionsBox;
	private JTextLinesEditor optionsTextLinesEditor;
	private JComponent getOptionsTextView(){
		JPanel optionsTextPanel=new JPanel(new BorderLayout());
		optionsTextPanel.add(optionsTextLinesEditor=new JTextLinesEditor());
		return optionsTextPanel;
	}
	private OptionCollection optionCollection=null;
	private int selectedOptionsTabIndex=0;
	// showOptionCollection() should be called whenever optionCollection changes...
	private void showOptionCollection(){
		optionsTextLinesEditor.setTextLinesContainer(optionCollection);
		optionsBox.removeAll();
		if(optionCollection!=null)for(Option option:optionCollection)optionsBox.add(new OptionView(option));
		// if currently showing the text, force a reread...
		if(selectedOptionsTabIndex==1)optionsTextLinesEditor.read();
	}
	public void updateOptionViews(){
		// this basically means to reshow the value (which will also update
		int optionIndex=optionsBox.getComponentCount();
		while(--optionIndex>=0)((OptionView)optionsBox.getComponent(optionIndex)).checkReset();
	}
	private void createView(){
		JTabbedPane optionsTabbedPane=new JTabbedPane();
		optionsTabbedPane.addTab("Design",new JScrollPane(optionsBox=Box.createVerticalBox()));
		optionsTabbedPane.addTab("Text",getOptionsTextView());
		optionsTabbedPane.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e){
				int optionsTabIndex=optionsTabbedPane.getSelectedIndex();
				try{
					switch(optionsTabIndex){
						case 0:
							if(selectedOptionsTabIndex==1)
								if(optionsTextLinesEditor.write())
									showOptionCollection();
							break;
						case 1:
							optionsTextLinesEditor.read();
							break;
					}
				}finally{
					selectedOptionsTabIndex=optionsTabIndex;
				}
			}
		});
		super.add(optionsTabbedPane);
	}
	public OptionCollectionView(){super(new BorderLayout());createView();}
	public OptionCollectionView setOptionCollection(OptionCollection optionCollection){
		this.optionCollection=optionCollection;
		showOptionCollection();
		return this;
	}
	public OptionCollection getOptionCollection(){return optionCollection;}

	// refresh() forces an update of optionCollection when it's currently showing the options text!!
	public void refresh(){
		if(selectedOptionsTabIndex==1)optionsTextLinesEditor.write();
	}
}
