package com.officevitae.marc;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.List;

public class FieldView extends JPanel implements FieldTypeSelectorView.ChangeListener{

	private static final void setInfo(String info){
		System.out.println("Field view info"+info);
	}

	private Field field;
	private JPanel fieldPanel; // the one that shows a valid field!!

	// a lot of ValidatedFieldLiteralView's!!!
	private ValidatedFieldLiteralView<Double> minNumberLiteralView,maxNumberLiteralView;
	private ValidatedFieldLiteralView<Long> minLengthLiteralView,maxLengthLiteralView,startAtLiteralView;
	private ValidatedFieldLiteralView<String> minDateLiteralView,maxDateLiteralView;
	private ValidatedFieldLiteralView<String[]> valuesLiteralView;
	private ValidatedFieldLiteralView<String> defaultLiteralView,matchLiteralView,aliasLiteralView,getLiteralView,setLiteralView,validateLiteralView,refLiteralView,indexLiteralView;

	// MDH@24OCT2018: replacing typeComboBox with a FieldTypeSelectorView which has full support for defining a type
	private FieldTypeSelectorView fieldTypeSelectorView; // replacing: private JComboBox typeComboBox;
	// whenever the selected field type changes, we determine which views to show
	public void fieldTypeChanged(FieldTypeSelectorView fieldTypeSelectorView,IFieldType selectedFieldType){
		Utils.setInfo(this,"Type of field '"+field.getName()+"' of schema '"+field.getCollection().getSchema().getName()+"' changed to "+selectedFieldType.getDescription().toString()+".");
		field.setType(selectedFieldType); // update the type of the current field
		showField();
		/*
		// TODO distrust whether setType succeeds????
		////////autoIncrementView.setVisible(selectedFieldType.equals(MongooseFieldType.NUMBER)); // if it's a Number it could be an auto-increment field...
		// we can have options if not an auto-increment field!!!
		optionsView.setVisible(!field.isAutoIncremented()); // at this moment on the required check box is on the options view so all these options are not applicable at that point!!
		indexLiteralView.setVisible(!field.isAutoIncremented()); // indexView replaced by indexLiteralView
		// we have to hide certain views if not applicable to the selected field type
		stringOptionsView.setVisible(selectedFieldType.equals(MongooseFieldType.STRING));
		numberOptionsView.setVisible(selectedFieldType.equals(MongooseFieldType.NUMBER)&&!field.isAutoIncremented()); // any common Number allows setting minimum and maximum number
		dateOptionsView.setVisible(selectedFieldType.equals(MongooseFieldType.DATE));
		refView.setVisible(selectedFieldType.equals(MongooseFieldType.OBJECTID)); // an ObjectId can refer to another object (table)
		/////////autoIncrementView.setVisible(field.type.equals(FieldType.AUTO_INCREMENT)); // an auto incremented integer can have a startAt value
		// what else? a lot of views should not be visible when auto increment integer is selected!!
		*/
	}

	private JCheckBox requiredCheckBox,selectCheckBox;
	// the check boxes associated with the flags of the optional options
	//////////private JCheckBox defaultCheckBox,minNumberCheckBox,maxNumberCheckBox,minDateCheckBox,maxDateCheckBox,startAtCheckBox,valuesCheckBox;
	private JComponent /*autoIncrementView,*/optionsView,functionsView/*,indexView*/,stringOptionsView,numberOptionsView,dateOptionsView,refView/*,arrayElementTypeView*/;
	private void showType(){
		// MDH@24OCT2018: now using a FieldTypeSelectorView for taking care of selecting a type...
		 // replacing: typeComboBox.setSelectedItem(field.getType()); // most convenient way to speed up stuff
		fieldTypeSelectorView.selectFieldType(field.getType());
	}
	public String toString(){return(field!=null?"View of field '"+field.getName()+"'":"Field view");}
	/*
	private void updateStartAtView(boolean updateText){
		if(updateText)startAtLiteralView.setText(field.startAtLiteral.getText());
		boolean startAtTextValid=field.startAtLiteral.isValid();
		startAtLiteralView.setForeground(startAtTextValid?Color.BLACK:Color.RED);
		startAtCheckBox.setSelected(field.startAtFlag&&startAtTextValid);
		startAtCheckBox.setEnabled(field.startAtFlag||startAtTextValid||(field.startAtLiteral.getValidText()!=null));
	}
	*/
	private ValidatedFieldLiteralView<Long> getStartAtView(){
		return new ValidatedFieldLiteralView<Long>("Auto-incremented starting at: ");
	}
	private void showStringOptions(){
		lowercaseRadioButton.setSelected(field.isLowercase());
		uppercaseRadioButton.setSelected(field.isUppercase());
		caseSensitiveRadioButton.setSelected(!field.isLowercase()&&!field.isUppercase());
		trimCheckBox.setSelected(field.isTrim());
		// allow user to always turn the flag off, but not always on unless there's a valid text to return to
		minLengthLiteralView.setValidatedFieldLiteral(field.minLengthLiteral);
		maxLengthLiteralView.setValidatedFieldLiteral(field.maxLengthLiteral);
		// before showing the values we'd have to plug in any valid min and/or max length
		valuesLiteralView.setValidatedFieldLiteral(field.valuesLiteral); /////field.updateValuesLengthExtremes();updateValuesView(true);
		matchLiteralView.setValidatedFieldLiteral(field.matchLiteral);
		stringOptionsView.setVisible(true);
	}
	private void showDateOptions(){
		minDateLiteralView.setValidatedFieldLiteral(field.minDateLiteral);
		maxDateLiteralView.setValidatedFieldLiteral(field.maxDateLiteral);
		dateOptionsView.setVisible(true);
	}
	private void showNumberOptions(){
		minNumberLiteralView.setValidatedFieldLiteral(field.minNumberLiteral);
		maxNumberLiteralView.setValidatedFieldLiteral(field.maxNumberLiteral);
		// we've got some linking to do as well
		numberOptionsView.setVisible(true);
	}
	private List<String> referencableTableNames=null;
	private void showRefView(){
		refLiteralView.setValidatedFieldLiteral(field.refLiteral);
		// technically we could already determine whether refBrowseButton should be enabled, but this actually depends on the choosen type
		referencableTableNames=MongooseSchemaFactory.getNamesOfSchemasWithIdOfType(field.getType(),field.getCollection().getSchema().getName());
		refBrowseButton.setEnabled(referencableTableNames!=null&&!referencableTableNames.isEmpty());
		// if somebody changes the start at literal and invalidates it somehow or whether selected or not we need to show the view again
		field.refLiteral.addValidatedFieldLiteralChangeListener(new ValidatedFieldLiteralChangeListener(){
			public synchronized void invalidated(ValidatedFieldLiteral validatedLiteral){
				showField();
			}
			public synchronized void enableChanged(ValidatedFieldLiteral validatedLiteral){
				showField();
			}
		});
		refView.setVisible(true);
	}
	private void showOptions(){
		requiredCheckBox.setSelected(field.isRequired());
		requiredCheckBox.setEnabled(!field.isRequiredBlocked()); // MDH@01NOV2018: required may be blocked (e.g. automatically with _id)
		selectCheckBox.setSelected(field.isSelect());
		defaultLiteralView.setValidatedFieldLiteral(field.defaultLiteral);
		optionsView.setVisible(true);
	}
	private void showIndexOptions(){
		// MDH@01NOV2018: we have to prevent changing the current value if it is blocked
		indexLiteralView.setEnabled(!field.isIndexBlocked());
		indexLiteralView.setValidatedFieldLiteral(field.indexTypeLiteral);
		indexLiteralView.setVisible(true);
		/* replacing:
		if(field.isIndex())indexRadioButton.setSelected(true);else
		if(field.isUnique())uniqueRadioButton.setSelected(true);else
		if(field.isSparse())sparseRadioButton.setSelected(true);else noIndexRadioButton.setSelected(true);
		indexView.setVisible(true);
		*/
	}
	private void showFunctionOptions(){
		getLiteralView.setValidatedFieldLiteral(field.getLiteral);
		setLiteralView.setValidatedFieldLiteral(field.setLiteral);
		validateLiteralView.setValidatedFieldLiteral(field.validateLiteral);
		functionsView.setVisible(true);
	}
	private void showStartAtView(){
		startAtLiteralView.setValidatedFieldLiteral(field.startAtLiteral);
		// if somebody changes the start at literal and invalidates it somehow or whether selected or not we need to show the view again
		((ValidatedFieldLiteral)field.startAtLiteral).addValidatedFieldLiteralChangeListener(new ValidatedFieldLiteralChangeListener(){
			public void invalidated(ValidatedFieldLiteral validatedLiteral){
				showField();
			}
			public void enableChanged(ValidatedFieldLiteral validatedLiteral){
				showField();
			}
		});
		startAtLiteralView.setVisible(true);
	}
	private void showAliasView(){
		aliasLiteralView.setValidatedFieldLiteral(field.aliasLiteral);
		aliasLiteralView.setVisible(true);
	}

	private void showTag(){fieldTagTextField.setText(field.getTag());} // MDH@30OCT2018

	private void hideField(){
		((CardLayout)getLayout()).show(this,"NoField");
	}
	private void showField(){
		try{
			showTag();
			virtualCheckBox.setEnabled(!field.isVirtualBlocked()); // virtual will be blocked on _id
			if(field.isVirtual()){
				aliasLiteralView.setVisible(false);
				refView.setVisible(false);
				indexLiteralView.setVisible(false);
				optionsView.setVisible(false);
				stringOptionsView.setVisible(false);
				dateOptionsView.setVisible(false);
				numberOptionsView.setVisible(false);
				showFunctionOptions();
			}else{ // a 'real' field
				showAliasView(); // TODO can an auto-increment column have an alias????
				boolean references=field.isReferencing();
				boolean autoincremented=field.isAutoIncremented();
				IFieldType type=field.getType();
				// MDH@24OCT2018: we no longer have a subview to select the array element field type, that's all taken care of by FieldTypeSelectorView
				/* replacing
				if(type.equals(MongooseFieldType.ARRAY)){ // either a generic Array or type specific array!!
					removeSubSchemaFieldTypes(arrayElementTypeComboBoxModel);
					addSubSchemaFieldTypes(arrayElementTypeComboBoxModel);
					IFieldType arrayElementType=field.getArrayElementType();
					if(arrayElementType!=null)arrayElementTypeComboBoxModel.setSelectedItem(arrayElementType.getDescription()); // TODO may we assume to have the array element type defined???
					arrayElementTypeView.setVisible(true);
				}else
					arrayElementTypeView.setVisible(false);
				*/
				// I suppose anything that is numeric can be auto-incremented!!
				// can't tell exactly but Number fields seem to be the only auto-incremental fields (with the used library)
				if(type.equals(MongooseFieldType.NUMBER)||type.equals(MongooseFieldType.INT32)||type.equals(MongooseFieldType.LONG))showStartAtView();else startAtLiteralView.setVisible(false); //////autoIncrementView.setVisible(false);

				if(!autoincremented&&!field.getName().equalsIgnoreCase("_id")&&(type.equals(MongooseFieldType.OBJECTID)||type.equals(MongooseFieldType.NUMBER)||type.equals(MongooseFieldType.STRING)||type.equals(MongooseFieldType.BUFFER)))
					showRefView();
				else if(refView!=null)refView.setVisible(false);

				if(!autoincremented)showIndexOptions();else indexLiteralView.setVisible(false); // TODO can a reference be used as index????

				// options only when NOT an auto-incremented thingie
				if(!autoincremented)showOptions();else optionsView.setVisible(false);
				if(!autoincremented)showFunctionOptions();else functionsView.setVisible(false);
				// index stuff
				// type specific stuff
				if(!autoincremented&&type.equals(MongooseFieldType.STRING))showStringOptions();else stringOptionsView.setVisible(false);
				if(type.equals(MongooseFieldType.DATE))showDateOptions();else dateOptionsView.setVisible(false);
				// all number types should allow setting minimum and maximum although I'm not certain whether it will work...
				if(!autoincremented&&(type.equals(MongooseFieldType.NUMBER)||type.equals(MongooseFieldType.INT32)||type.equals(MongooseFieldType.LONG)))
					showNumberOptions();
				else numberOptionsView.setVisible(false); // only show Number options on a regular (non-auto-incremented) Number field!!
				// get a border showing the name of the field now!!
			}
		}finally{
			fieldNameLabel.setText("Field "+field.getName()); // replacing: fieldPanel.setBorder(new TitledBorder("Field "+field.name+" "));
			((CardLayout)getLayout()).show(this,"Field");
		}
	}
	/*
	// MongooseSchema.SubSchemaListener implementation
	public void subSchemaAdded(MongooseSchema subSchema){
		// MDH@24OCT2018: typeComboBox replaced by an instance of FieldTypeSelectorView
		fieldTypeSelectorView.addAdditionalFieldType(subSchema);
	}
	public void subSchemaRemoved(MongooseSchema subSchema){
		fieldTypeSelectorView.removeAdditionalFieldType(subSchema);
	}
	*/
	/* replacing:
	private void removeSubSchemaFieldTypes(DefaultComboBoxModel comboBoxModel){
		int fieldTypeIndex=comboBoxModel.getSize();
		while(--fieldTypeIndex>=0)if(((IFieldType.Description)comboBoxModel.getElementAt(fieldTypeIndex)).getFieldType() instanceof MongooseSchema)comboBoxModel.removeElementAt(fieldTypeIndex);
	}
	private void addSubSchemaFieldTypes(DefaultComboBoxModel comboBoxModel){
		try{
			for(MongooseSchema subSchema:field.getCollection().getSchema().getSubSchemas())comboBoxModel.addElement(subSchema.getDescription()); // TODO should be showing the description????
		}catch(Exception ex){}
	}
	*/
	// end MongooseSchema.SubSchemaListener implementation

	// MDH@02NOV2018: with
	private MongooseSchema schema=null;
	private void setSchema(MongooseSchema schema){
		if(schema==null&&this.schema==null)return; // both null, no change
		if(schema==null||!schema.equals(this.schema)){
			if(this.schema!=null){
				this.schema.setListener(null);
				fieldTypeSelectorView.setAdditionalFieldTypes(null);
			}
			this.schema=schema;
			if(this.schema!=null){
				this.schema.setListener(fieldTypeSelectorView);
				fieldTypeSelectorView.setAdditionalFieldTypes(this.schema.getFieldTypes());
			}
		}
	}
	public void setField(Object field){
		// MDH@24OCT2018 removing: removeSubSchemaFieldTypes(typeComboBoxModel);
		try{
			fieldTagTextField.setText("");
			//////////this.field.getCollection().getSchema().removeSubSchemaListener(this);
		}catch(Exception ex){}
		this.field=(field!=null&&field instanceof Field?(Field)field:null);
		setSchema(this.field!=null?this.field.getCollection().getSchema():null);
		if(this.field!=null){
			try{
				showField();
				// MDH@24OCT2018 removing: addSubSchemaFieldTypes(typeComboBoxModel);
				//////////this.field.getCollection().getSchema().addSubSchemaListener(this);
			}catch(Exception ex){
				System.out.println("ERROR: '"+ex.getLocalizedMessage()+"' in setting the field of the field viewer.");
			}finally{
				showType();
			}
		}else
			hideField();
	}
	public void setFieldType(IFieldType fieldType) { // to be called whenever
		try{field.setType(fieldType);}finally{showField();} // easiest way to get to see the contents of the field (without the field type itself)
	}
	// MDH@24OCT2018 removing: private DefaultComboBoxModel typeComboBoxModel;
	private JComponent getTypeView(){
		if(fieldTypeSelectorView==null)
			fieldTypeSelectorView=new FieldTypeSelectorView().setPrefix("Type: ").setFieldTypeChangeListener(this);
		return fieldTypeSelectorView;
		/*
		JPanel typePanel=SwingUtils.getTitledPanel(null);
		typePanel.add(new JLabel("Type: "),BorderLayout.WEST);
		// MDH@16OCT2018: we want to be able to add the subschema's of the owner as types to the type combo box of a field
		typePanel.add(typeComboBox=new JComboBox(typeComboBoxModel=new DefaultComboBoxModel(MongooseFieldType.values()))); // showing all possible values only...
		// respond to any new selection of another field type immediately
		typeComboBox.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange()==ItemEvent.SELECTED){
					setFieldType(((IFieldType.Description)event.getItem()).getFieldType());
				}
			}
		});
		////////////typeComboBox.setSelectedIndex(DEFAULT_TYPE_INDEX); // get the right type selected
		return typePanel;
		*/
	}
	/* MDH@24OCT2018 removing:
	// if the user selects Array as type, (s)he should be allowed to select the type of the array elements
	private DefaultComboBoxModel arrayElementTypeComboBoxModel;
	private JComboBox arrayElementTypeComboBox;
	private JComponent getArrayElementTypeView(){
		JPanel arrayElementTypePanel=SwingUtils.getTitledPanel(null);
		arrayElementTypePanel.add(new JLabel("Array element type: "),BorderLayout.WEST);
		arrayElementTypePanel.add(arrayElementTypeComboBox=new JComboBox(arrayElementTypeComboBoxModel=new DefaultComboBoxModel(MongooseFieldType.values()))); // showing all possible values only...
		arrayElementTypeComboBoxModel.removeElement(MongooseFieldType.ARRAY); // we don't want this!!!
		arrayElementTypeComboBox.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e){
				try{
					field.setArrayElementType(((IFieldType.Description)arrayElementTypeComboBox.getSelectedItem()).getFieldType());
				}catch(Exception ex){}
			}
		});
		// we can append the subschema's of the parent
		///// wait for field being selected!!! addSubSchemaFieldTypes(arrayElementTypeComboBoxModel);
		return arrayElementTypePanel;
	}
	*/
	private JComponent getDefaultView(){
		return(defaultLiteralView=new ValidatedFieldLiteralView<String>("Default: "));
	}
	private JComponent getFunctionsView(){
		JPanel functionsPanel=SwingUtils.getTitledPanel("");
		functionsPanel.add(SwingUtils.getLabelView("Functions"),BorderLayout.NORTH);
		Box functionsBox=Box.createVerticalBox();
		functionsBox.add(SwingUtils.getLeftAlignedView(new JLabel("Only the body of the function (with single argument called 'v') should be entered!")));
		functionsBox.add(getLiteralView=new ValidatedFieldLiteralView<String>("Get: "));
		functionsBox.add(setLiteralView=new ValidatedFieldLiteralView<String>("Set: "));
		functionsBox.add(validateLiteralView=new ValidatedFieldLiteralView<String>("Validate: "));
		functionsPanel.add(functionsBox);
		return functionsPanel;
	}
	private JComponent getOptionsView(){
		JPanel optionsPanel=SwingUtils.getTitledPanel("");
		Box optionsBox=Box.createVerticalBox();
		optionsBox.add(SwingUtils.getLabelView("General options"));
		// we can have required and select at the top, or perhaps the alias
		requiredCheckBox=new JCheckBox("Required");
		requiredCheckBox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				field.setRequired(requiredCheckBox.isSelected());
			}
		});
		optionsBox.add(SwingUtils.getLeftAlignedView(requiredCheckBox));
		selectCheckBox=new JCheckBox("Select");
		selectCheckBox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				field.setSelect(selectCheckBox.isSelected());
			}
		});
		optionsBox.add(SwingUtils.getLeftAlignedView(selectCheckBox));
		optionsBox.add(getDefaultView());
		optionsBox.add(aliasLiteralView=new ValidatedFieldLiteralView<String>("Alias: "));
		optionsPanel.add(optionsBox,BorderLayout.NORTH);
		return optionsPanel;
	}
	// MDDH@25OCT2018: much simpler now...
	public ValidatedFieldLiteralView<String> getIndexView(){
		return new ValidatedFieldLiteralView<String>("Index type: ",Field.INDEX_TYPE_NAMES);
	}
	/* replacing:
	JRadioButton noIndexRadioButton=new JRadioButton("(None)"),uniqueRadioButton=new JRadioButton("Unique"),indexRadioButton=new JRadioButton("Index"),sparseRadioButton=new JRadioButton("Sparse");
	public JComponent getIndexView(){
		noIndexRadioButton.addChangeListener(
				new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						if(noIndexRadioButton.isSelected()){field.setUnique(false);field.setIndex(false);field.setSparse(false);}
					}
				});
		uniqueRadioButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (uniqueRadioButton.isSelected()) field.setUnique(true);
			}
		});
		indexRadioButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(indexRadioButton.isSelected())field.setIndex(true);
			}
		});
		sparseRadioButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(sparseRadioButton.isSelected())field.setSparse(true);
			}
		});
		JPanel indexPanel=SwingUtils.getTitledPanel(null);
		Box indexBox=Box.createHorizontalBox();
		indexBox.add(new JLabel("Index type: "));
		indexBox.add(noIndexRadioButton);
		indexBox.add(uniqueRadioButton);
		indexBox.add(indexRadioButton);
		indexBox.add(sparseRadioButton);
		ButtonGroup indexButtonGroup=new ButtonGroup();
		indexButtonGroup.add(noIndexRadioButton);
		indexButtonGroup.add(uniqueRadioButton);
		indexButtonGroup.add(indexRadioButton);
		indexButtonGroup.add(sparseRadioButton);
		indexPanel.add(indexBox,BorderLayout.WEST);
		return indexPanel;
	}
	*/
	private JRadioButton caseSensitiveRadioButton,lowercaseRadioButton,uppercaseRadioButton;
	private JCheckBox trimCheckBox;
	private JButton refBrowseButton;
	public JComponent getRefView(){
		JPanel refPanel=new JPanel(new BorderLayout());
		refPanel.add(refLiteralView=new ValidatedFieldLiteralView<String>("References document: "));
		refPanel.add(refBrowseButton=new JButton("Browse"),BorderLayout.EAST);
		refBrowseButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				List<String> tableNames=MongooseSchemaFactory.getNamesOfSchemasWithIdOfType(field.getType(),field.getCollection().getSchema().getName());
				if(tableNames!=null&&!tableNames.isEmpty()){
					refLiteralView.setText(SwingUtils.getSelectedOption("Document selection","Select the document whose id is referenced.",(String[])tableNames.toArray(new String[tableNames.size()])));
				}else
					setInfo("There are no tables with an id field of type "+field.getType().toString()+".");
			}
		});
		return refPanel;
            /*
            JPanel objectIdOptionsPanel=getTitledPanel("ObjectId options");
            objectIdOptionsPanel.add(new ValidatedFieldLiteralView<>("References: "));
            return objectIdOptionsPanel;
            */
	}
	public JComponent getStringValuesView(){
		return(valuesLiteralView=new ValidatedFieldLiteralView<String[]>("Values: "));
	}
	public JComponent getMatchView(){
		return(matchLiteralView=new ValidatedFieldLiteralView<String>("Match: "));
	}
	public JComponent getMinLengthView(){
		return(minLengthLiteralView=new ValidatedFieldLiteralView<Long>("Minimum length: "));
	}
	public JComponent getMaxLengthView(){
		return(maxLengthLiteralView=new ValidatedFieldLiteralView<Long>("Maximum length: "));
	}
	public JComponent getLengthsView(){
		JPanel lengthsPanel=new JPanel(new BorderLayout());
		Box lengthsBox=Box.createVerticalBox();
		///////lengthsBox.add(new JLabel("Length: "));
		lengthsBox.add(getMinLengthView());
		lengthsBox.add(getMaxLengthView());
		lengthsPanel.add(lengthsBox);
		return lengthsPanel;
	}
	public JComponent getTrimView(){
		JPanel trimPanel=new JPanel(new BorderLayout());
		trimPanel.add(trimCheckBox=new JCheckBox("Trim"),BorderLayout.WEST);
		trimCheckBox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				field.setTrim(trimCheckBox.isSelected());
			}
		});
		return trimPanel;
	}
	public JComponent getStringOptionsView(){
		JPanel stringPanel=SwingUtils.getTitledPanel("");
		stringPanel.add(SwingUtils.getLabelView("String options"),BorderLayout.NORTH);
		JPanel northStringPanel=new JPanel(new BorderLayout());
		Box stringBox=Box.createHorizontalBox();
		// flags first
		stringBox.add(caseSensitiveRadioButton=new JRadioButton("Case-sensitive"),true);
		caseSensitiveRadioButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(caseSensitiveRadioButton.isSelected()){field.setLowercase(false);field.setUppercase(false);}
			}
		});
		stringBox.add(lowercaseRadioButton=new JRadioButton("Lowercase"));
		lowercaseRadioButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				field.setLowercase(lowercaseRadioButton.isSelected());
			}
		});
		stringBox.add(uppercaseRadioButton=new JRadioButton("Uppercase"));
		uppercaseRadioButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				field.setUppercase(uppercaseRadioButton.isSelected());
			}
		});
		ButtonGroup buttonGroup=new ButtonGroup();buttonGroup.add(caseSensitiveRadioButton);buttonGroup.add(lowercaseRadioButton);buttonGroup.add(uppercaseRadioButton);
		// we can put any values in the middle
		northStringPanel.add(stringBox,BorderLayout.WEST);
		// and min length and max length
		/////////northStringPanel.add(getLengthsView(),BorderLayout.EAST);
		Box stringVerticalBox=Box.createVerticalBox();
		stringVerticalBox.add(northStringPanel);
		stringVerticalBox.add(getTrimView());
		stringVerticalBox.add(getLengthsView());
		stringVerticalBox.add(getStringValuesView());
		stringVerticalBox.add(getMatchView());
		stringPanel.add(stringVerticalBox);
		return stringPanel;
	}
	public JComponent getNumberOptionsView(){
		JPanel numberPanel=SwingUtils.getTitledPanel("");
		numberPanel.add(SwingUtils.getLabelView("Number options"),BorderLayout.NORTH);
		// user should be allowed to set a minimum and/or maximum
		JPanel extremesPanel=new JPanel(new GridLayout(2,1));
		extremesPanel.add(minNumberLiteralView=new ValidatedFieldLiteralView<Double>("Minimum: "));
		extremesPanel.add(maxNumberLiteralView=new ValidatedFieldLiteralView<Double>("Maximum: "));
		numberPanel.add(extremesPanel);
		return numberPanel;
	}
	public JComponent getDateOptionsView(){
		JPanel datePanel=SwingUtils.getTitledPanel("");
		datePanel.setLayout(new GridLayout(3,1));
		datePanel.add(SwingUtils.getLabelView("Date options"));
		///////maxDateLiteralView.setVisible(false);
		datePanel.add(minDateLiteralView=new ValidatedFieldLiteralView<String>("Minimum date: "));
		datePanel.add(maxDateLiteralView=new ValidatedFieldLiteralView<String>("Maximum date: "));
		return datePanel;
	}
	private JTextField fieldTagTextField;
	private JComponent getTagView(){
		JPanel tagPanel=new JPanel(new BorderLayout());
		tagPanel.add(new JLabel("Description: "),BorderLayout.WEST);
		tagPanel.add(fieldTagTextField=new JTextField());
		fieldTagTextField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(KeyEvent e){
				field.setTag(fieldTagTextField.getText());
			}
		});
		return tagPanel;
	}
	private JCheckBox virtualCheckBox;
	private JComponent getVirtualView(){
		JPanel virtualPanel=new JPanel(new BorderLayout());
		virtualPanel.add(virtualCheckBox=new JCheckBox("Virtual"),BorderLayout.WEST);
		virtualCheckBox.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e){
				try{field.setVirtual(virtualCheckBox.isSelected());}finally{showField();}
			}
		});
		return virtualPanel;
	}
	private JLabel fieldNameLabel;
	public FieldView(){
		super(new CardLayout());
		add(new JLabel("The selected field will be displayed here"),"NoField");
		fieldPanel=new JPanel(new BorderLayout());
		fieldNameLabel=new JLabel();fieldNameLabel.setFont(fieldNameLabel.getFont().deriveFont(Font.BOLD));
		// let's not show the name????
		Box fieldBox=Box.createVerticalBox();
		fieldBox.add(SwingUtils.getLeftAlignedView(fieldNameLabel));
		fieldBox.add(getTagView());
		fieldBox.add(getVirtualView());
		fieldBox.add(getTypeView());
		// MDH@24OCT2018 removing: fieldBox.add(arrayElementTypeView=getArrayElementTypeView()); // MDH@16OCT2018: only to be shown when type Array is selected, so a user can select the array element type
		fieldBox.add(startAtLiteralView=getStartAtView()); // right below the type where a person can define whether to be auto-incrementing...
		fieldBox.add(refView=getRefView());
		fieldBox.add(indexLiteralView=getIndexView()); ///////fieldBox.add(indexView=getIndexView());
		// now the parts with panels around the options
		fieldBox.add(optionsView=getOptionsView()); // required and default options
		fieldBox.add(functionsView=getFunctionsView());
		// type-specific views
		fieldBox.add(numberOptionsView=getNumberOptionsView());
		fieldBox.add(stringOptionsView=getStringOptionsView());
		fieldBox.add(dateOptionsView=getDateOptionsView());
		// should we be hiding all type-specific stuff??
		fieldPanel.add(fieldBox,BorderLayout.NORTH);
		add(fieldPanel,"Field");
		/////////((CardLayout)getLayout()).show(getParent(),"NoField");
	}
}
