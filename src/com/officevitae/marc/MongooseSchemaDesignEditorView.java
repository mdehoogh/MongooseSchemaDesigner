package com.officevitae.marc;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.io.File;

public class MongooseSchemaDesignEditorView extends JPanel implements IFieldChangeListener, MongooseSchema.SyncListener,IMutableTextLinesProducer.ChangeListener,Utils.InfoMessageListener{

	private static final String TAG=MongooseSchemaDesignEditorView.class.getSimpleName();

	public class FieldListModel extends DefaultListModel<Field>{
		public void fieldChanged(int fieldIndex){
			Utils.setInfo(this,"Field #"+fieldIndex+" changed!");
			super.fireContentsChanged(this,fieldIndex,fieldIndex);
		}
	}

	// MDH@17OCT2018: this editor view maintains an text lines editor group to manage the editing of the text lines that go in and out the MongooseSchema
	private ITextLinesEditorGroup mongooseSchemaTextLinesEditorGroup=new ITextLinesEditorGroup.Base();

	private MongooseSchema mongooseSchema=null;

	// Utils.InfoMessageListener implementation
	public Object getSource(){return this.mongooseSchema;}
	public void infoMessagesChanged(){
		showInfoButton.setEnabled(Utils.hasInfoMessages(getSource()));
	}
	// end Utils.InfoMessageListener implementation

	private JList fieldList;

	// SyncListener implementation
	public void syncChanged(MongooseSchema mongooseSchema){
		Utils.consoleprintln("Mongoose schema design editor responding to the sync status of the schema with representation '"+mongooseSchema.getRepresentation(true)+"'.");
		// if this is the currently selected mongoose schema update the label!!!
		if(mongooseSchema.equals(this.mongooseSchema)){
			saveButton.setEnabled(!mongooseSchema.isSynced());
			mongooseSchemaEntryLabel.setText("Schema "+mongooseSchema.getRepresentation(true));
		}
		else Utils.consoleprintln("NOTE: The Mongoose schema design that changed ("+mongooseSchema.getName()+") is not the currently selected Mongoose schema design"+(this.mongooseSchema!=null?"("+this.mongooseSchema.getName()+")":"")+".");
	}
	// end syncListener implementation
	// listen to any changes to a field of mine
	public void fieldChanged(Field field){
		if(field==null)return;
		int fieldIndex = mongooseSchema.getIndexOfField(field);
		if(fieldIndex<0){Utils.setInfo(this,"Unknown field "+field.getName()+"' changed!");return;}
		// by 'replacing' the model element at this given index, we force
		((FieldListModel)fieldList.getModel()).fieldChanged(fieldIndex);
	}
	private JButton newFieldButton;
	private JTextField newFieldTextField;
	private Field newField(String fieldName){
		// preferably ask for the name of the field in a dialog or perhaps let the user type it in
		Field field=null;
		try{
			field=new Field(fieldName);
			if(mongooseSchema!=null&&mongooseSchema.addField(field)){
				((DefaultListModel<Field>)fieldList.getModel()).addElement(field);
				field.addFieldChangeListener(this); // want to know when the field changes (so I can update the list accordingly) and/or show the Save button of course
			}else
				field=null;
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' adding field "+fieldName+".");
		}
		return field;
	}
	private boolean isExistingFieldname(String name){
		ListModel model=fieldList.getModel();
		for(int fieldindex=0;fieldindex<model.getSize();fieldindex++) {
			if (model.getElementAt(fieldindex).toString().equalsIgnoreCase(name)) return true;
		}
		return false;
	}
	private JComponent getNewFieldView(){
		JPanel newFieldPanel=new JPanel(new BorderLayout());
		newFieldPanel.add(newFieldButton=new JButton("Add"),BorderLayout.WEST);
		newFieldButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Field field=newField(newFieldTextField.getText()); // attempt to add the new field, if we succeed disable the new field button and select the newly added field!!!
				if(field!=null){
					// if we want to preselect the field type if we think the field name indicates that it is a reference to another mongooseSchema
					if(field.getName().length()>3&&field.getName().substring(field.getName().length()-3).equalsIgnoreCase("_id")){
						String referencedSchemaName=field.getName().substring(0,field.getName().length()-3);
						MongooseSchema referencedSchema=MongooseSchemaFactory.getMongooseSchemaWithName(referencedSchemaName);
						if(referencedSchema!=null){
							field.setType(referencedSchema.getTypeOfIdField());
							field.refLiteral.setText(referencedSchemaName);
							field.refLiteral.setDisabled(false);
						}
					}
					newFieldButton.setEnabled(false);
					SwingUtils.removeDefaultButton(newFieldButton);
					fieldList.setSelectedValue(field,true);
				}
			}
		});
		newFieldButton.setEnabled(false); // no valid field name entered yet!!!
		newFieldPanel.add(newFieldTextField=new JTextField());
		//////////newFieldTextField.setBorder(new LineBorder(Color.BLACK));
		newFieldTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				// let's check whether this is a valid text name
				String newFieldname=newFieldTextField.getText();
				boolean isValidFieldname=!newFieldname.isEmpty()&&!isExistingFieldname(newFieldname);
				newFieldButton.setEnabled(isValidFieldname);
				if(isValidFieldname)SwingUtils.setDefaultButton(newFieldButton);
			}
		});
		return newFieldPanel;
	}
	private boolean newFieldSelected=false; // the flag that will keep track of the selection of a field
	private JComponent getFieldListView(){
		JPanel fieldListPanel=SwingUtils.getTitledPanel(null);
		fieldListPanel.add(SwingUtils.getLabelView("Fields"),BorderLayout.NORTH);
		fieldList=new JList<Field>(new FieldListModel()); // MUST be a FieldListModel!!
		fieldList.setCellRenderer(new FieldListCellRenderer()); // MDH@15OCT2018: we want to be able to 'remove' a field temporarily i.e. disable it, so it won't be published
		// MDH@15OCT2018: in order to be able to detect clicking a field item that is currently selected
		fieldList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(!newFieldSelected){
					FieldListModel fieldListModel=(FieldListModel)fieldList.getModel();
					int fieldListIndex=fieldList.locationToIndex(e.getPoint());
					Field clickedField=(Field)fieldListModel.getElementAt(fieldListIndex);
					try{
						clickedField.setEnabled(!clickedField.isEnabled()); // toggling the enabled flag might not be allowed...
						fieldListModel.fieldChanged(fieldListIndex);
					}catch(IllegalArgumentException iex){
						Utils.setInfo(this,"ERROR: "+iex.getLocalizedMessage());
					}
				}else // the next time will be a real click!!!
					newFieldSelected=false;
			}
		});
		// the problem with the cell renderer is to know when it was clicked!!!!
		fieldList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting())return;
				newFieldSelected=true;
				fieldView.setField(fieldList.getSelectedValue());
				System.out.println("Field selected!");
			}
		});
		fieldListPanel.add(new JScrollPane(fieldList));
		fieldListPanel.add(getNewFieldView(),BorderLayout.SOUTH);
		return fieldListPanel;
	}
	private FieldView fieldView; // the view where to show the field information
	private JComponent getSchemaFieldsView(){
		JPanel fieldsEntryPanel=new JPanel(new BorderLayout());
		JSplitPane splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); // MDH@19OCT2018: switch to a horizontal split so we do not need to use the entire width of this subwindow for just showing the name
		splitPane.setOneTouchExpandable(true); // allow collapsing the field list view with a single click
		splitPane.setLeftComponent(getFieldListView());
		splitPane.setRightComponent(fieldView=new FieldView());
		/////////fieldView.setVisible(false); // initially no actually field selected or shown...
		fieldsEntryPanel.add(splitPane);
		splitPane.setDividerLocation(150); // not too wide!!
		// we can have a list of field names left ready to be edited?????
		return fieldsEntryPanel;
	}
	private void saveSchema(){
		if(mongooseSchema!=null){
			// there might be uncommitted text in the text lines editor
			if(tabbedPane.getSelectedIndex()==0&&!fieldsTextLinesEditor.write())
				Utils.setInfo(this,"Can't save, due to failing to update the schema from the text.");
			else
				mongooseSchema.save();
		}else
			Utils.setInfo(this,"Bug: No schema to save!");
	}
	/*
	private void publishSchema(){
		if(mongooseSchema!=null){
			if(tabbedPane.getSelectedIndex()==0&&!fieldsTextLinesEditor.write())
				Utils.setInfo(this,"Can't publish, due to failing to update the schema from the text.");
			else
				mongooseSchema.publish();
		}else
			Utils.setInfo(this,"Bug: No schema to publish!");
	}
	*/
	private boolean isExistingSubSchemaname(String subSchemaname){
		DefaultComboBoxModel subSchemaComboBoxModel=(DefaultComboBoxModel)subSchemaComboBox.getModel();
		int subSchemaIndex=subSchemaComboBoxModel.getSize();
		while(--subSchemaIndex>0)if(((MongooseSchema)subSchemaComboBoxModel.getElementAt(subSchemaIndex)).getName().equals(subSchemaname))return true;
		return false;
	}
	/*
	private JButton addSubSchemaButton=new JButton("Add subschema:");
	private JTextField subSchemaTextField=new JTextField();
	private JComponent getAddSubSchemaView(){
		JPanel addSubSchemaPanel=new JPanel(new BorderLayout());
		addSubSchemaPanel.add(addSubSchemaButton,BorderLayout.WEST);
		addSubSchemaButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				// we need to create a subschema of the given name to the current schema
				// I guess we're going to prepend the subschema name with the name of its parent!!
				String subSchemaName=subSchemaTextField.getText().trim();
				MongooseSchema mongooseSubSchema=new MongooseSchema(subSchemaName,mongooseSchema);
				if(mongooseSchema.equals(mongooseSubSchema.getParent())){ // if the parent was set correctly (which we may assume!!)
					addSubSchemaButton.setEnabled(false); // don't allow adding twice!!
					SwingUtils.removeDefaultButton(addSubSchemaButton);
					((DefaultComboBoxModel)subSchemaComboBox.getModel()).addElement(mongooseSubSchema);
					subSchemaComboBox.setSelectedItem(mongooseSubSchema); // immediately select the newly created subschema (which will take care of showing the associated fields!!!)
					// TODO it's a bit of a nuisance to show a subschema in the same fields because this means we need to plug in the subschema
					//      this means we need a means to navigate back to the parent schema
					// I suppose Save and Publish are only valid at the top level, perhaps Save is valid as well for subschema's but Publish is NOT
				}else
					Utils.setInfo(this,"Failed to add sub schema "+subSchemaName+".");
			}
		});
		addSubSchemaButton.setEnabled(false);
		addSubSchemaPanel.add(subSchemaTextField);
		subSchemaTextField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(KeyEvent e){
				// check if this is a new subschema name, if so enable the button and make it default
				if(!isExistingSubSchemaname(subSchemaTextField.getText().trim())){
					addSubSchemaButton.setEnabled(true);
					SwingUtils.setDefaultButton(addSubSchemaButton); // make it the default button so we can quickly add a subschema
				}else
					addSubSchemaButton.setEnabled(false);
			}
		});
		subSchemaTextField.setEnabled(false);
		return addSubSchemaPanel;
	}
	*/
	private JButton saveButton;
	private ITextLinesConsumer getExistingTextLinesConsumerFile(File file){
		File parentFile=file.getParentFile();
		if(parentFile!=null&&!parentFile.exists()&&!parentFile.mkdirs())return null;
		return new ITextLinesConsumer.TextFile(file);
	}
	private void write(ITextLinesConsumer textLinesConsumer,ITextLinesProducer textLinesProducer){
		try{
			textLinesProducer.produceTextLines();
			textLinesConsumer.setTextLines(textLinesProducer.getProducedTextLines());
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' saving to "+textLinesConsumer.toString()+".");
		}
	}
	private JButton showInfoButton;
	private InfoMessageViewer infoMessageViewer=null;
	JFrame showInfoFrame=null;
	private void showInfo(){
		if(showInfoFrame==null){
			showInfoFrame=new JFrame();
			showInfoFrame.getContentPane().setLayout(new BorderLayout());
			showInfoFrame.getContentPane().add(infoMessageViewer=new InfoMessageViewer());
			showInfoFrame.setSize(1024,768);
			showInfoFrame.setLocationRelativeTo(null); // center
			showInfoFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		}
		// ascertain to show the right messages (that of the current Mongoose schema!)
		infoMessageViewer.setSource(mongooseSchema);
		showInfoFrame.setTitle("Schema "+mongooseSchema.getName()+" information messages");
		showInfoFrame.setVisible(true);
	}
	private JComponent getSaveView(){
		JPanel savePanel=new JPanel(new BorderLayout());
		savePanel.add(showInfoButton=new JButton("Show info"),BorderLayout.WEST);
		showInfoButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				showInfo();
			}
		});
		savePanel.add(saveButton=new JButton("Save"),BorderLayout.EAST);
		saveButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				switch(tabbedPane.getSelectedIndex()){
					case 0:
						// update the fields collection first, before performing the normal write!!!
						fieldsTextLinesEditor.write();
					case 1:
						if(mongooseSchema!=null&&!mongooseSchema.save())Utils.setInfo(this,"Failed to save schema '"+mongooseSchema.getName()+"'.");
						break;
					case 2:
						// writing model, routes and controller
						write(getExistingTextLinesConsumerFile(new File("./app/controllers",mongooseSchema.getName()+".controller.js")),controllerTextLinesEditor);
						write(getExistingTextLinesConsumerFile(new File("./app/routes",mongooseSchema.getName()+".routes.js")),routesTextLinesEditor);
						write(getExistingTextLinesConsumerFile(new File("./app/models",mongooseSchema.getName()+".model.js")),modelTextLinesEditor);
						break;
				}
			}
		});
		return savePanel;
	}
	/*
	private JComponent schemaButtonView;
	private JComponent getSchemaButtonView(){
		JPanel mongooseSchemaButtonPanel=new JPanel(new BorderLayout());
		mongooseSchemaButtonPanel.add(saveButton=new JButton("Save"),BorderLayout.WEST);
		////////mongooseSchemaButtonPanel.add(getAddSubSchemaView());
		Box buttonBox=Box.createHorizontalBox();
		////////buttonBox.add(backButton=new JButton("Back"));
		buttonBox.add(publishSchemaButton=SwingUtils.getButton("Publish",false,true,null));
		mongooseSchemaButtonPanel.add(buttonBox,BorderLayout.EAST);
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveSchema();
			}
		});
		publishSchemaButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				publishSchema();
			}
		});
		return mongooseSchemaButtonPanel;
	}
	*/
	private JComboBox subSchemaComboBox;
	/*
	private JComponent getSubSchemaSelectorView(){
		JPanel subSchemaSelectorPanel=new JPanel(new BorderLayout());
		subSchemaSelectorPanel.add(new JLabel("Subschema: "),BorderLayout.WEST);
		subSchemaSelectorPanel.add(subSchemaComboBox=new JComboBox(new DefaultComboBoxModel()));
		((DefaultComboBoxModel)subSchemaComboBox.getModel()).addElement("-"); // always include the (main) entry associated with the main schema
		subSchemaComboBox.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e){
				if(subSchemaComboBox.getSelectedIndex()<1)return;
				// switch to showing the subschema!!! BUT remember the first element is NOT a MongooseSchema!!!!
				try{
					setMongooseSchema((MongooseSchema)subSchemaComboBox.getSelectedItem());
				}catch(Exception ex){}
			}
		});
		return subSchemaSelectorPanel;
	}
	*/
	///////private JButton backButton;
	private JComponent getSchemaHeaderView(){
		JPanel schemaHeaderPanel=new JPanel(new BorderLayout());
		/* replacing:
		mongooseSchemaEntryLabel=SwingUtils.getButton("",true,true,SwingUtils.NARROW_BORDER);
		// at the top we put the name of the schema BUT we now also need
		JPanel schemaNamePanel=new JPanel(new BorderLayout());
		schemaNamePanel.setMinimumSize(new Dimension(200,48));
		schemaNamePanel.add(mongooseSchemaEntryLabel,BorderLayout.WEST);
		schemaNamePanel.add(backButton=SwingUtils.getButton("Back",false,false,null),BorderLayout.EAST);
		// return to showing the parent of the current schema!!
		backButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setMongooseSchema(mongooseSchema.getParent());
			}
		});
		backButton.setVisible(false); // main schema's don't allow back buttons!!
		schemaHeaderPanel.add(schemaNamePanel,BorderLayout.NORTH);
		*/
		mongooseSchemaEntryLabel.setFont(mongooseSchemaEntryLabel.getFont().deriveFont(Font.BOLD));
		schemaHeaderPanel.add(SwingUtils.getLeftAlignedView(mongooseSchemaEntryLabel),BorderLayout.NORTH);
		////////schemaHeaderPanel.add(getSubSchemaSelectorView());
		return schemaHeaderPanel;
	}
	private JComponent getSchemaDesignView(){
		JPanel schemaDesignPanel=new JPanel(new BorderLayout());
		schemaDesignPanel.add(getSchemaFieldsView());
		/////////schemaDesignPanel.add(schemaButtonView=getSchemaButtonView(),BorderLayout.SOUTH);
		return schemaDesignPanel;
	}
	private JTextLinesEditor modelTextLinesEditor;
	private JComponent saveView=null; // we need these views in order to be able to hide them???
	/*
	private JComponent getSaveModelView(){
		JPanel saveModelPanel=new JPanel(new BorderLayout());
		saveModelPanel.add(publishSchemaButton=new JButton("Save"),BorderLayout.EAST);
		publishSchemaButton.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){publishSchema();}});
		return saveModelPanel;
	}
	*/
	private JComponent schemaModelView=null; // the one to show and/or hide!!!
	private JComponent getSchemaModelView(){
		JPanel schemaTextPanel=new JPanel(new BorderLayout());
		schemaTextPanel.add(modelTextLinesEditor=new JTextLinesEditor());
		///////schemaTextPanel.add(saveModelView=getSaveModelView(),BorderLayout.SOUTH);
		return schemaTextPanel;
	}
	private JTextLinesEditor fieldsTextLinesEditor; // where we show the text of the Mongoose Schema
	public void textLinesChanged(IMutableTextLinesProducer textLinesProducer){
		if(textLinesProducer.equals(fieldsTextLinesEditor)){
			if(this.mongooseSchema!=null){
				try{
					if(!this.mongooseSchema.unsavedWithFieldsTextLines(textLinesProducer.getProducedTextLines())){
						saveButton.setEnabled(false);
						return;
					}
				}catch(Exception ex){}
				// benefit of the double
				saveButton.setEnabled(true);
			}
		}
	}
	private JComponent getSaveSchemaView(){
		JPanel saveSchemaPanel=new JPanel(new BorderLayout());
		saveSchemaPanel.add(saveButton=new JButton("Save"),BorderLayout.EAST);
		saveButton.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){saveSchema();}});
		return saveSchemaPanel;
	}
	private JComponent getSchemaTextView(){
		JPanel schemaTextPanel=new JPanel(new BorderLayout());
		schemaTextPanel.add(fieldsTextLinesEditor=new JTextLinesEditor());
		////////schemaTextPanel.add(saveSchemaView=getSaveSchemaView(),BorderLayout.SOUTH);
		return schemaTextPanel;
	}
	private JComponent outputView;
	private JTextLinesEditor routesTextLinesEditor,controllerTextLinesEditor;
	private JComponent getSchemaRoutesView(){
		JPanel schemaRoutesPanel=new JPanel(new BorderLayout());
		schemaRoutesPanel.add(routesTextLinesEditor=new JTextLinesEditor());
		return schemaRoutesPanel;
	}
	private JComponent getSchemaControllerView(){
		JPanel schemaControllerPanel=new JPanel(new BorderLayout());
		schemaControllerPanel.add(controllerTextLinesEditor=new JTextLinesEditor());
		return schemaControllerPanel;
	}
	private JComponent getOutputView(){
		JPanel outputPanel=new JPanel(new BorderLayout());
		JTabbedPane outputTabbedPane=new JTabbedPane();
		outputTabbedPane.addTab("Model",getSchemaModelView());
		outputTabbedPane.addTab("Routes",getSchemaRoutesView());
		outputTabbedPane.addTab("Controller",getSchemaControllerView());
		outputPanel.add(outputTabbedPane);
		return outputPanel;
	}
	private JTabbedPane tabbedPane;
	// updateSelectedTabView() to be called whenever the user select another tab, or when the selected mongoose schema changes!!
	private int lastSelectedTabIndex=-1;
	private void updateSelectedTabView(){
		// MDH@25OCT2018: ascertain to have the right tabs
		if(mongooseSchema!=null&&mongooseSchema.getParent()==null){
			if(tabbedPane.getTabCount()<3)tabbedPane.addTab("Output",outputView);
			saveView.setVisible(true);
		}else{
			saveView.setVisible(false);
			if(tabbedPane.getTabCount()>=3)tabbedPane.remove(2);
		}
		switch(tabbedPane.getSelectedIndex()){
			case 0:
				// TODO is there a better way to do this??? because we only need to plug in another field collection if the schema changes!!!!
				// NOTE suppose the contents of the container changes (i.e. is volatile) it may have changed elsewhere without us knowing and we need to validate!!!
				//      so validate() means determining if the contents changed, and if it did push it on the history stack (and show it)
				//      well showing would suffice
				try{
					fieldsTextLinesEditor.read(); // replacing:	fieldsTextLinesEditor.setTextLinesContainer(mongooseSchema.getFieldCollection());
					lastSelectedTabIndex=0;
				}catch(Exception ex){
					Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' obtaining the schema definition.");
				}
				break;
			case 1:
				// TODO we should only do the following if the schemaTextArea's text actually changed!!!
				// MDH@17OCT2018: we force a load so that what we see actually represents the text loaded (if any)
				if(mongooseSchema!=null){
					// disconnecting the fields text lines editor from the field collection will update the field collection accordingly (if something changed there)
					// so we will get to see the proper fields and all!!!
					fieldsTextLinesEditor.write();// replacing: fieldsTextLinesEditor.setTextLinesContainer(null);
					/* MDH@18OCT2018: we shouldn't be loading!!!!
					try{
						mongooseSchema.load();
					}catch(Exception ex){
						Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' initializing the schema design.");
					}
					*/
					/* TODO what to do when the schema failed to load successfully????
					boolean aSubSchema=(this.mongooseSchema.getParent()!=null); // it's a subschema if the schema has a parent!!
					//////////schemaButtonView.setVisible(!aSubSchema); // replacing publishSchemaButton.setVisible(!aSubSchema);
					*/
					showSchemaFields();
					lastSelectedTabIndex=1;
				}
				break;
			case 2:
				// force a read so we will see the most recent model text lines
				if(mongooseSchema!=null){
					// I have to update the schema (fields) with the contents of the editor, if the editor view is now showing
					if(lastSelectedTabIndex==0)fieldsTextLinesEditor.write();
					modelTextLinesEditor.read();
					routesTextLinesEditor.read();
					controllerTextLinesEditor.read();
				}
				lastSelectedTabIndex=2;
				break;
		}
	}
	////////private JSplitPane ioSplitPane,designSplitPane;
	private JComponent getSchemaView(){
		JPanel mongooseSchemaEntryPanel=new JPanel(new BorderLayout());
		mongooseSchemaEntryLabel=new JLabel(); // SwingUtils.getButton("",true,true,SwingUtils.NARROW_BORDER);
		// at the top we put the name of the schema BUT we now also need
		mongooseSchemaEntryPanel.add(getSchemaHeaderView(),BorderLayout.NORTH);
		/*
		JSplitPane ioSplitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		JSplitPane designSplitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		ioSplitPane.setOneTouchExpandable(true);
		designSplitPane.setOneTouchExpandable(true);
		designSplitPane.setLeftComponent(getSchemaTextView());
		designSplitPane.setRightComponent(getSchemaDesignView());
		ioSplitPane.setTopComponent(designSplitPane);
		ioSplitPane.setBottomComponent(getSchemaModelView());
		mongooseSchemaEntryPanel.add(ioSplitPane);
		ioSplitPane.setDividerLocation(0.9d);
		*/
		// replacing:
		tabbedPane=new JTabbedPane();
		// initially when the schema is selected in the tree view we show the text only (before it's parsed when the Design view is shown!!!)
		tabbedPane.addTab("Text",getSchemaTextView());
		tabbedPane.addTab("Design",getSchemaDesignView());
		outputView=getOutputView();
		// now wait for a top-level Mongoose schema to be selected!!!		tabbedPane.addTab("Model",schemaModelView=getSchemaModelView());
		tabbedPane.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e){
				updateSelectedTabView();
			}
		});
		mongooseSchemaEntryPanel.add(tabbedPane);
		mongooseSchemaEntryPanel.add(saveView=getSaveView(),BorderLayout.SOUTH); // MDH@19OCT2018: allow saving (and publishing) at any moment
		return mongooseSchemaEntryPanel;
	}
	// end GUI stuff
	//////private JComponent mongooseSchemaEntryView=null;
	private JLabel mongooseSchemaEntryLabel;
	/*
	private void showSubSchemas(){
		subSchemaTextField.setText("");
		subSchemaTextField.setEnabled(this.mongooseSchema!=null); // we can have subschemas if we have a mongoose schema!!
		addSubSchemaButton.setEnabled(false);
		// this means reading the subschema's  which I guess can be defined in their own files like <main schema>.<subschema>.msc
		DefaultComboBoxModel subSchemaComboBoxModel=(DefaultComboBoxModel)subSchemaComboBox.getModel();
		subSchemaComboBoxModel.removeAllElements();
		// TODO show all children
		subSchemaComboBoxModel.addElement("");
		if(mongooseSchema!=null)for(MongooseSchema subSchema:mongooseSchema.getSubSchemas())subSchemaComboBoxModel.addElement(subSchema);
	}
	*/
	private void showSchemaFields(){
		DefaultListModel<Field> model=(DefaultListModel<Field>)fieldList.getModel();
		model.clear();
		MongooseSchema.FieldCollection fieldCollection=(mongooseSchema!=null?mongooseSchema.fieldCollection:null);
		if(fieldCollection!=null)for(Field field:fieldCollection)model.addElement(field); // get all the fields into the fieldList's model
		if(fieldCollection==null||fieldCollection.isEmpty())fieldList.clearSelection();else fieldList.setSelectedIndex(0); // if any fields present, show the first one!!
		newFieldTextField.setText("");
		newFieldButton.setEnabled(false);
	}
	private void showMongooseSchema(){
		if(this.mongooseSchema!=null){
			Utils.setInfo(this,"Showing schema '"+this.mongooseSchema.getRepresentation(false)+"'.");
			/////////backButton.setVisible(aSubSchema);
			// we're actually editing the fields and NOT the entire schema (with subschemas included!!!)
			// MDH@19OCT2018: switch to showing the (full) representation (and not just toString() which shows the name and the sync status only)
			mongooseSchemaEntryLabel.setText("Schema "+mongooseSchema.getRepresentation(true)); // replacing: mongooseSchemaEntryView.setBorder(new TitledBorder("Table "+mongooseSchema.name+" ")); // show the name of the mongooseSchema in the border
			//////////showSubSchemas();
			((CardLayout)getLayout()).show(this,"Schema");
		}else
			((CardLayout)getLayout()).show(this,"NoSchema");
	}
	public void setMongooseSchema(MongooseSchema mongooseSchema){
		// NOTE that this could possibly be a subschema
		try{
			if(this.mongooseSchema!=null){
				if(tabbedPane.getSelectedIndex()==0)fieldsTextLinesEditor.write(); // in case we were editing the field collection, update it before actually removing the text lines container
				fieldsTextLinesEditor.removeChangeListener(this);
				fieldsTextLinesEditor.setTextLinesContainer(null); // get rid of editing the current field collection...
				modelTextLinesEditor.setTextLinesContainer(null);
				/////this.mongooseSchemaTextLinesEditorGroup.removeTextLinesEditor(this.mongooseSchema); // MDH@17OCT2018: unregister the current MongooseSchema as one of the text lines editors
				this.mongooseSchema.deleteSchemaSyncListener(this);
				this.mongooseSchema.setFieldChangeListener(null);
				showInfoButton.setEnabled(false);
			}
			this.mongooseSchema=mongooseSchema;
			////////schemaButtonView.setVisible(this.mongooseSchema!=null&&this.mongooseSchema.getParent()==null); // TODO should we put this somewhere else??? unless we allow saving any schema even it's a subschema!!!
			updateSelectedTabView(); // MDH@17OCT2018: we have to ascertain that the Mongoose Schema is up to date for showing in the currently showing tab page
			if(this.mongooseSchema!=null){
				showInfoButton.setEnabled(Utils.hasInfoMessages(this.mongooseSchema));
				fieldsTextLinesEditor.setTextLinesContainer(this.mongooseSchema.getFieldCollection());
				// a basic MongooseSchema exposes both a model text lines consumer and producer (so we have to set them separately)
				modelTextLinesEditor.setTextLinesProducer(this.mongooseSchema.getModelTextLinesProducer());
				routesTextLinesEditor.setTextLinesProducer(mongooseSchema.getRoutesTextLinesProducer());
				controllerTextLinesEditor.setTextLinesProducer(mongooseSchema.getControllerTextLinesProducer());
				// TODO do we need this?????
				modelTextLinesEditor.setTextLinesConsumer(this.mongooseSchema.getModelTextLinesConsumer());

				switch(tabbedPane.getSelectedIndex()){
					case 0:fieldsTextLinesEditor.read();break;
					case 1:break;
					case 2:break;
				}
				saveButton.setEnabled(!this.mongooseSchema.isSynced());
				fieldsTextLinesEditor.addChangeListener(this); // any change to the text should result in checking whether or not this text differs from the last 'saved' text!!!
				this.mongooseSchema.addSchemaSyncListener(this); // listen in to any changes to the sync property!!!
				this.mongooseSchema.setFieldChangeListener(this);
			}
		}catch(Exception ex){
			Utils.consoleprintln("ERROR: '"+ex.getLocalizedMessage()+"' updating the Mongoose schema editor view to show a new Mongoose schema.");
		}finally{
			showMongooseSchema();
		}
	}
	public MongooseSchema getMongooseSchema(){return mongooseSchema;} // exposes the associated mongoose schema!!!
	public String toString(){return "Office Vitae Mongoose Schema Editor";}
	public MongooseSchemaDesignEditorView(){
		super(new CardLayout());
		Utils.addInfoMessageListener(this);
		add(new JLabel("The selected schema will show here!"),"NoSchema");
		add(getSchemaView(),"Schema");
	}

}
