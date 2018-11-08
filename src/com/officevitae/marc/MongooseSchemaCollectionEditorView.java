package com.officevitae.marc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MongooseSchemaCollectionEditorView extends JPanel{

	private MongooseSchemaCollection mongooseSchemaCollection=null;
	private JButton saveMongooseSchemaCollectionOptionCollectionButton;
	private OptionCollectionView optionCollectionView=null;
	private JLabel optionCollectionLabel;
	private void showMongooseSchemaCollection(){
		if(optionCollectionView!=null)optionCollectionView.setOptionCollection(mongooseSchemaCollection!=null?mongooseSchemaCollection.getOptionCollection():null);
		OptionCollection optionCollection=optionCollectionView.getOptionCollection();
		optionCollectionLabel.setText(optionCollection!=null?"Options of Mongoose schema collection "+mongooseSchemaCollection.toString():"");
		if(saveMongooseSchemaCollectionOptionCollectionButton!=null)saveMongooseSchemaCollectionOptionCollectionButton.setEnabled(optionCollectionView.getOptionCollection()!=null);
	}
	private JComponent getOptionCollectionLabelView(){
		JPanel optionCollectionLabelPanel=new JPanel(new BorderLayout());
		optionCollectionLabelPanel.add(optionCollectionLabel=new JLabel(),BorderLayout.WEST);
		optionCollectionLabelPanel.setFont(optionCollectionLabel.getFont().deriveFont(Font.BOLD));
		return optionCollectionLabelPanel;
	}
	private JComponent getSaveOptionCollectionView(){
		JPanel saveOptionCollectionPanel=new JPanel(new BorderLayout());
		saveOptionCollectionPanel.add(saveMongooseSchemaCollectionOptionCollectionButton=new JButton("Save options"),BorderLayout.WEST);
		saveMongooseSchemaCollectionOptionCollectionButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				if(mongooseSchemaCollection.saveOptionCollection())
					Utils.setInfo(null,"Options of Mongoose schema collection "+mongooseSchemaCollection.toString()+" saved!");
				else
					Utils.setInfo(null,"Failed to save the options of Mongoose schema collection '"+mongooseSchemaCollection.toString()+"'.");
			}
		});
		return  saveOptionCollectionPanel;
	}
	private JComponent getOptionCollectionView(){
		JPanel optionCollectionPanel=new JPanel(new BorderLayout());
		// we want to show the options and allow saving them
		optionCollectionPanel.add(getOptionCollectionLabelView(),BorderLayout.NORTH);
		optionCollectionPanel.add(optionCollectionView=new OptionCollectionView());
		optionCollectionPanel.add(getSaveOptionCollectionView(),BorderLayout.SOUTH);
		return optionCollectionPanel;
	}
	private void createView(){
		add(getOptionCollectionView());
	}

	public MongooseSchemaCollectionEditorView(){super(new BorderLayout());createView();}

	public MongooseSchemaCollectionEditorView setMongooseSchemaCollection(MongooseSchemaCollection mongooseSchemaCollection){
		this.mongooseSchemaCollection=mongooseSchemaCollection;
		showMongooseSchemaCollection();
		return this;
	}

}
