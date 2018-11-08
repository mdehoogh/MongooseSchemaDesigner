package com.officevitae.marc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * more complex than it seems because the selector view needs to add subschema's as field type
 * MDH@02NOV2018: changed that by allowing it to listen to dynamic changes to a supposed mutable field type collection...
 */
public class FieldTypeSelectorView extends JPanel implements IMutableMongooseFieldTypeCollection.Listener{

	public interface ChangeListener{
		void fieldTypeChanged(FieldTypeSelectorView fieldTypeSelectorView,IFieldType newFieldType);
	}

	private ChangeListener changeListener=null;

	private FieldTypeSelectorView subFieldTypeView=null,parentFieldTypeSelectorView=null;

	private IFieldType selectedFieldType=MongooseFieldType.MIXED; // initially MIXED is selected as default

	// keep track of a single array and map field type instance, so that we can keep track of the selected sub type
	private IFieldType arrayFieldType=null; // the generic type which is always present initially in the combo box (itself being it's own Description)
	private IFieldType getArrayFieldType(){if(arrayFieldType==null)arrayFieldType=new ArrayFieldType();return arrayFieldType;}
	private IFieldType mapFieldType=null;
	private IFieldType getMapFieldType(){if(mapFieldType==null)mapFieldType=new MapFieldType();return mapFieldType;}
	// if an external party calls selectFieldType() with either an ArrayFieldType or a MapFieldType we use these as our ArrayFieldType/MapFieldType, so that we return the right instance when so requested!!!
	// AND we won't have to actually replace the ARRAY and MAP placeholders at all!!!
	private IFieldType setArrayFieldType(ArrayFieldType arrayFieldType){
		this.arrayFieldType=arrayFieldType;
		return MongooseFieldType.ARRAY; // returning the placeholder
	}
	private IFieldType setMapFieldType(MapFieldType mapFieldType){
		this.mapFieldType=mapFieldType;
		return MongooseFieldType.MAP; // returning the placeholder
	}

	private IFieldType fieldType=null; // what's actually selected in the list
	private IFieldType getFieldType(){
		if(fieldType!=null){
			if(fieldType.equals(MongooseFieldType.ARRAY))return getArrayFieldType();
			if(fieldType.equals(MongooseFieldType.MAP))return getMapFieldType();
		}
		return fieldType;
	}
	private void setFieldType(IFieldType fieldType){
		// Ok, if the user selects either Array or Map as primitive type, we should actually switch to the array equivalent
		// alternatively, it might be better to actually remove Array and Map from the predefined list, so we can add instances of ArrayFieldType and MapFieldType
		this.fieldType=fieldType;
		Utils.consoleprintln("Field type set to '"+this.fieldType.toString()+"'.");
		/* if the user select the generic Array or Map instance placeholders we have to replace them by a non-generic instance immediately
		if(selectedFieldType instanceof MongooseFieldType){
			if(selectedFieldType.equals(MongooseFieldType.ARRAY))selectedFieldType=setArrayFieldType(new ArrayFieldType());else
			if(selectedFieldType.equals(MongooseFieldType.MAP))selectedFieldType=setMapFieldType(new MapFieldType());
		}
		*/
		IFieldType selectedFieldType=getFieldType(); // which transforms the ARRAY and MAP placeholders to the actual Array or MapFieldType
		Utils.consoleprintln("Selected field type: '"+selectedFieldType.toString()+"'.");
		try{changeListener.fieldTypeChanged(this,selectedFieldType);}catch(Exception ex){} // works even if we do not have a change listener!!!
		if(parentFieldTypeSelectorView!=null)parentFieldTypeSelectorView.setSubFieldType(selectedFieldType); // NOTE passing the actual selected field type over and not just the selected placeholder!!!
		if(selectedFieldType instanceof ICompositeFieldType){
			if(subFieldTypeView==null){
				super.add(subFieldTypeView=new FieldTypeSelectorView().setParent(this)); // immediately add it and set its parent to be me!!
			}
			// MDH@09NOV2018: here's the thing: additional field types (in our case subschema's are allowed at the top level and on sublevels for Array only
			subFieldTypeView.setAdditionalFieldTypes(additionalFieldTypes==null||!(fieldType instanceof ArrayFieldType)&&!fieldType.equals(MongooseFieldType.ARRAY)?null:additionalFieldTypes); // MDH@06NOV2018: essential to plug in any additional field types immediately, so they will be available!!!!
			// set the prefix accordingly
			subFieldTypeView.setPrefix((selectedFieldType instanceof ArrayFieldType?"Array":(selectedFieldType instanceof MapFieldType?"Map":"Sub"))+" element type: ");
			subFieldTypeView.selectFieldType(((ICompositeFieldType)selectedFieldType).getSubFieldType());
			subFieldTypeView.setVisible(true);
		}else if(subFieldTypeView!=null){ // best to remove it...
			Utils.consoleprintln("Removing the subtype view.");
			subFieldTypeView.setVisible(false); // is this required???
			remove(subFieldTypeView);
			subFieldTypeView=null;
		}
	}
	// the sub field type view will call setSubFieldType() whenever a user selects a new type (and setSelectedFieldType() gets called!!!)
	void setSubFieldType(IFieldType subFieldType){
		IFieldType selectedFieldType=getFieldType();
		if(selectedFieldType instanceof ICompositeFieldType){
			((ICompositeFieldType)selectedFieldType).setSubFieldType(subFieldType);
			// should be treated as a change of the field type as such???
			try{changeListener.fieldTypeChanged(this,selectedFieldType);}catch(Exception ex){}
		}else
			Utils.consoleprintln("ERROR: Field type '"+selectedFieldType.toString()+"' not composite!");
		// TODO what if the currently selected field type is NOT a composite field type??
	}

	// expose added field type
	Set<IFieldType> additionalFieldTypes=new HashSet<IFieldType>();
	// available to the sub fieldtype view are the added field types (so far)
	Set<IFieldType> getAdditionalFieldTypes(){return additionalFieldTypes;}
	private void removeAdditionalFieldTypes(){
		additionalFieldTypes.clear();
		DefaultComboBoxModel<IFieldType.Description> fieldTypeComboBoxModel=(DefaultComboBoxModel<IFieldType.Description>)fieldTypeComboBox.getModel();
		// we can speed things
		int fieldTypeIndex=fieldTypeComboBoxModel.getSize();
		IFieldType fieldType;
		while(--fieldTypeIndex>=0){
			fieldType=fieldTypeComboBoxModel.getElementAt(fieldTypeIndex).getFieldType();
			if(fieldType instanceof MongooseFieldType)continue;
			if(fieldType instanceof ICompositeFieldType)continue;
			fieldTypeComboBoxModel.removeElementAt(fieldTypeIndex);
		}
	}
	// NOTE typically setAdditionalFieldTypes is called by whoever registers myself as listener
	public void setAdditionalFieldTypes(Set<IFieldType> fieldTypes){
		// NOTE this is a bit of a nuisance as I have to remove all currently displayed additional field types from fieldTypeComboBox
		try{
			if(additionalFieldTypes!=null)removeAdditionalFieldTypes();
			if(fieldTypes!=null)additionalFieldTypes.addAll(fieldTypes); // register ALL field types
			if(!additionalFieldTypes.isEmpty())for(IFieldType fieldType:additionalFieldTypes)fieldTypeComboBox.addItem(fieldType.getDescription()); // make it show!!!
		}catch(Exception ex){
		}finally{
			// pass along my additional field types if the subtype is currently visible!!
			if(subFieldTypeView!=null&&subFieldTypeView.isVisible())subFieldTypeView.setAdditionalFieldTypes(additionalFieldTypes);
		}
	}
	private void addAdditionalFieldTypes(){
		for(IFieldType addableFieldType:parentFieldTypeSelectorView.getAdditionalFieldTypes())fieldTypeComboBox.addItem(addableFieldType.getDescription());
	}

	// IMutableMongooseFieldTypeCollection.Listener implementation
	// TODO for now we're using the 'original' methods to actual change the additional field types collection
	public void fieldTypeAdded(IMutableMongooseFieldTypeCollection fieldTypeCollection,IFieldType fieldType){
		try{
			if(fieldType==null)return;
			additionalFieldTypes.add(fieldType);
			fieldTypeComboBox.addItem(fieldType.getDescription());
			// the main problem here is that additionalFieldTypes might not contain te new field type though it should!!
			if(subFieldTypeView.isVisible())if(getFieldType() instanceof ArrayFieldType)subFieldTypeView.fieldTypeAdded(fieldTypeCollection,fieldType);
		}catch(Exception ex){}
	}
	public void fieldTypeRemoved(IMutableMongooseFieldTypeCollection fieldTypeCollection,IFieldType fieldType){
		try{
			additionalFieldTypes.remove(fieldType);
			fieldTypeComboBox.removeItem(fieldType.getDescription());
			if(subFieldTypeView.isVisible())if(getFieldType() instanceof ArrayFieldType)subFieldTypeView.fieldTypeRemoved(fieldTypeCollection,fieldType);
		}catch(Exception ex){}
	}
	// end IMutableMongooseFieldTypeCollection.Listener implementation

	private JComboBox fieldTypeComboBox;
	private JLabel prefixLabel;
	private void createView(){
		// at the top we put combo box where a type can be selected and initialize with all the 'primitive' (i.e. predefined) field types
		// will add the description of all predefined Mongoose schema types (so we won't see the long names)
		DefaultComboBoxModel<IFieldType.Description> fieldTypeComboBoxModel=new DefaultComboBoxModel<IFieldType.Description>();
		fieldTypeComboBox=new JComboBox(fieldTypeComboBoxModel);
		for(IFieldType fieldType:MongooseFieldType.values())fieldTypeComboBoxModel.addElement(fieldType.getDescription());
		fieldTypeComboBox.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e){
				// I suppose we need to take care of selecting the generic Array or Map field here????
				setFieldType(((IFieldType.Description)fieldTypeComboBox.getSelectedItem()).getFieldType());
			}
		});
		// populate with the 'primitive' field types first
		// add a Map and Array composite field type
		// and the added field types if any at this moment
		if(parentFieldTypeSelectorView!=null)addAdditionalFieldTypes();
		JPanel comboBoxPanel=new JPanel(new BorderLayout());
		comboBoxPanel.add(prefixLabel=new JLabel(),BorderLayout.WEST);
		comboBoxPanel.add(fieldTypeComboBox);
		add(comboBoxPanel,BorderLayout.NORTH);
		// if a composite type is choosen we will need another subpanel to make visible
		/* can't do the following in the constructor as it would cause a stack overflow
		subFieldTypeView=new FieldTypeSelectorView().setParent(this);
		add(subFieldTypeView);
		*/
	}

	public FieldTypeSelectorView setPrefix(String prefix){
		prefixLabel.setText(prefix);
		return this;
	}
	public FieldTypeSelectorView setFieldTypeChangeListener(ChangeListener fieldTypeChangeListener){
		changeListener=fieldTypeChangeListener;
		return this;
	}

	public FieldTypeSelectorView(){
		super(new BorderLayout());
		createView();
		// initialize by selecting as default MongooseFieldType.MIXED
		fieldTypeComboBox.setSelectedItem(MongooseFieldType.MIXED);
	}

	// outsiders need to call selectFieldType() to set the field type to show
	public FieldTypeSelectorView selectFieldType(IFieldType fieldType){
		// we have to ascertain that in the combo box the appropriate placeholder will be selected if an array field type is presented!!!
		IFieldType.Description fieldTypeDescription=null;
		if(fieldType instanceof ArrayFieldType)
			fieldTypeDescription=setArrayFieldType((ArrayFieldType)arrayFieldType).getDescription();
		else if(fieldType instanceof MapFieldType)
			fieldTypeDescription=setMapFieldType((MapFieldType)fieldType).getDescription();
		else
			fieldTypeDescription=fieldType.getDescription();
		fieldTypeComboBox.setSelectedItem(fieldTypeDescription);
		return this;
	}

	// allows for adding field types typically only at the top level!!!
	public boolean addAdditionalFieldType(IFieldType fieldType){
		if(additionalFieldTypes.add(fieldType)){
			try{
				fieldTypeComboBox.addItem(fieldType.getDescription());
				// pass along to the sub field type selector (if currently visible), so it can also show this new field type in it's combo box
				if(!subFieldTypeView.isVisible()||subFieldTypeView.addAdditionalFieldType(fieldType))return true;
			}catch(Exception ex){}
		}
		return false;
	}
	public boolean removeAdditionalFieldType(IFieldType fieldType){
		if(additionalFieldTypes.remove(fieldType)){
			try{
				fieldTypeComboBox.removeItem(fieldType.getDescription());
				// pass along to the sub field type selector (if currently visible), so it can also show this new field type in it's combo box
				if(!subFieldTypeView.isVisible()||subFieldTypeView.removeAdditionalFieldType(fieldType))return true;
			}catch(Exception ex){}
		}
		return false;
	}

	// on the other hand, we can simply return the right instance if the currently selected field type is either MongooseFieldType.ARRAY or MongooseFieldType.MAP
	public FieldTypeSelectorView setParent(FieldTypeSelectorView parentFieldTypeSelectorView){
		this.parentFieldTypeSelectorView=parentFieldTypeSelectorView;
		return this;
	}

}
