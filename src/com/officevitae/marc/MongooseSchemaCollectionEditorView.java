package com.officevitae.marc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MongooseSchemaCollectionEditorView extends JPanel{

	// MDH@16NOV2018: in rare occasions the options associated with the collection are not editable
	//                this is accessible in the Mongoose schema collection, not the option collection
	private MongooseSchemaCollection mongooseSchemaCollection=null;
	private JButton saveMongooseSchemaCollectionOptionCollectionButton;
	private OptionCollectionView optionCollectionView=null;
	private JLabel optionCollectionLabel;
	// MDH@12NOV2018: refresh() takes care of telling the user when it can and should Save the option collection...
	public void refresh(){
		if(saveMongooseSchemaCollectionOptionCollectionButton!=null)
			saveMongooseSchemaCollectionOptionCollectionButton.setEnabled(mongooseSchemaCollection!=null&&mongooseSchemaCollection.isSyncable()&&!mongooseSchemaCollection.isSynced());
	}
	// OptionView.ChangeListener implementation

	private void showMongooseSchemaCollection(){
		if(optionCollectionView!=null){
			optionCollectionView.setOptionCollection(mongooseSchemaCollection!=null?mongooseSchemaCollection.getOptionCollection():null);
			// MDH@16NOV2018: the option collection view is editable when there is an associated Mongoose schema collection that is both syncable and synced!!
			//                this means that we have to force save the ass. option collection every time the user ends editing it...
			optionCollectionView.setEditable(mongooseSchemaCollection!=null&&mongooseSchemaCollection.isSynced()&&mongooseSchemaCollection.isSyncable());
			OptionCollection optionCollection=optionCollectionView.getOptionCollection();
			optionCollectionLabel.setText(optionCollection!=null?"Options of Mongoose schema collection "+mongooseSchemaCollection.toString():"");
		}
		refresh();
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
				if(mongooseSchemaCollection.saveOptionCollection()){
					Utils.setInfo(null,"Options of Mongoose schema collection "+mongooseSchemaCollection.toString()+" saved!");
					// we need to do a little more
					if(optionCollectionView!=null)optionCollectionView.updateOptionViews();
				}else
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
