package com.officevitae.marc;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class OptionCollectionView extends JPanel{
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
