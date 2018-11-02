package com.officevitae.marc;

import javax.swing.*;
import java.io.*;
import java.util.*;

// MDH@16OCT2018: any Mongoose Schema can also be used as field type (i.e. when it is a subschema!!!)
public class MongooseSchema implements IFieldChangeListener,IFieldType,ITextLinesProducer,IMutableMongooseFieldTypeCollection{

	private static final int DEFAULT_TYPE_INDEX=7;
	// MDH@25OCT2018: index, sparse and unique flags now removed (as they are not mutually exclusive)
	private static final java.util.List<String> FIELD_FLAG_NAMES=Arrays.asList(new String[]{"virtual","required","select","noselect","lowercase","uppercase","trim"});

	// MDH@18OCT2018: keep the fields in a separate FieldCollection class
	public class FieldCollection extends Vector<Field> implements ITextLinesContainer{
		public boolean containsFieldWithName(String fieldName){
			for(Field field:this)if(field.getName().equalsIgnoreCase(fieldName))return true;
			return false;
		}
		public FieldCollection(){
			// ascertain to have at least the _id field, and ascertain to call the constructor AFTER calling setParent!!!
			if(!super.add(new Field("_id").setType(MongooseFieldType.OBJECTID).setDisabable(parentSchema!=null)))
				Utils.setInfo(this,"ERROR: Failed to add automatic _id field to schema '"+getRepresentation(false)+"'.");
		}
		public MongooseSchema getSchema(){return MongooseSchema.this;}
		@Override
		public boolean add(Field field){
			// we don't want null fields or fields with the same name as one of mine
			if(field==null)return false;
			String fieldName=field.getName();
			if(fieldName==null||fieldName.isEmpty()){Utils.setInfo(this,"Failed to add a field that has no name to the field collection.");return false;}
			// find out if we have one with the same name
			int identicalFieldIndex=super.size();while(--identicalFieldIndex>=0&&!super.get(identicalFieldIndex).getName().equals(fieldName));
			if(identicalFieldIndex<0){
				if(!super.add(field)){Utils.setInfo(this,"Failed to add field '"+field.getName()+"' to the field collection.");return false;}
			}else{
				super.set(identicalFieldIndex,field); // TODO shouldn't be a problem now should it???? NO can only go wrong if the index is out of range!!
				Utils.setInfo(this,"WARNING: Field with name '"+fieldName+"' replaced with a newer version.");
			}
			// I guess that we need to ascertain that any _id field added is only disablable if it is a subschema!!!!
			if("_id".equals(fieldName))field.setDisabable(parentSchema!=null);
			// replacing: if(this.containsFieldWithName(field.getName())){Utils.setInfo(this,"Field '"+field.getName()+"' already present!");return false;}
			try{
				field.setCollection(this);
			}catch(Exception ex){
				Utils.consoleprintln("ERROR: '"+ex.getLocalizedMessage()+"' registering the collection with a field.");
			}
			return true; // force registering the schema that contains the field!!!
		}
		public void setTextLines(String[] textLines){
			super.clear();
			for(String textLine:textLines)add(getFieldFromContents(textLine));
		}
		String[] textLines=null;
		public void produceTextLines()throws Exception{
			textLines=new String[this.size()];
			int fieldIndex=0;for(Field field:this)textLines[fieldIndex++]=getFieldContents(field);
		}
		public String[] getProducedTextLines(){return textLines;}
		public boolean doneEditing(){return true;}
	}
	public FieldCollection fieldCollection=null;
	public FieldCollection getFieldCollection(){return fieldCollection;}

	/* no need for the ID anymore
	public class ID{
		public String toString(){return name+(!isSynced()?"*":"");} // we want to see whether it changed!!!
		public MongooseSchema getSchema(){return MongooseSchema.this;}
	}
	private ID id;
	public ID getID(){
		if(id==null)id=new ID();
		return id;
	}
	*/

	// IFieldType implementation
	public Description getDescription(){
		return new IFieldType.Description(){
			public IFieldType getFieldType(){return MongooseSchema.this;}
			public String toString(){
				return getRepresentation(false)+"Schema";
			}
			// TODO do we need the following????
			public boolean equals(Object o){
				try{return ((IFieldType.Description)o).toString().equals(toString());}catch(Exception ex){}
				return false;
			}
		};
	}

	public boolean representsAValidValue(String value){
		boolean valid=(value!=null&&!value.trim().isEmpty());
		return valid;
	}
	// we may expect to be comparing a MongooseSchema with MongooseFieldType objects as well...
	public boolean equals(Object object){
		try{
			if(object instanceof IFieldType){
				if(object instanceof MongooseSchema)return name.equals(((MongooseSchema)object).getName());
				if(object instanceof MongooseFieldType)return false; // never less or equal
				//////return ((MongooseSchema)object).getDescription().equals(getDescription()); // only when same description
			}
		}catch(Exception ex){}
		return false;
	}
	/*
	public int compareTo(IFieldType fieldType){
		try{
			if(fieldType instanceof MongooseFieldType)return 1; // always 'above' a Mongoose Schema Type
			return this.getDescription().compareTo(fieldType.getDescription());
		}catch(Exception ex){}
		return -1; // always smaller than any other field type (MongooseSchema's)
	}
	*/
	// end IFieldType implementation

	public interface SyncListener{
		void syncChanged(MongooseSchema schema);
	}

	// MDH@02NOV2018: because subschema's are field types we expose them
	// IMutableFieldTypeCollection implementation
	private IMutableMongooseFieldTypeCollection.Listener fieldTypeCollectionListener;
	public Set<IFieldType> getFieldTypes(){return Set.of((subSchemas.isEmpty()?new IFieldType[]{}:(IFieldType[])subSchemas.toArray(new IFieldType[subSchemas.size()])));}
	public void setListener(IMutableMongooseFieldTypeCollection.Listener fieldTypeCollectionListener){
		this.fieldTypeCollectionListener=fieldTypeCollectionListener;
	}
	// end IMutableFieldTypeCollection implementation

	// subschema support
	public interface SubSchemaListener{
		void subSchemaAdded(MongooseSchema subSchema);
		void subSchemaRemoved(MongooseSchema subSchema);
	}
	private Vector<SubSchemaListener> subSchemaListeners=new Vector<SubSchemaListener>();
	public boolean addSubSchemaListener(SubSchemaListener listener){
		return(listener!=null?subSchemaListeners.contains(listener)||subSchemaListeners.add(listener):false);
	}
	public boolean removeSubSchemaListener(SubSchemaListener listener){
		return(listener!=null?!subSchemaListeners.contains(listener)||subSchemaListeners.remove(listener):false);
	}
	// can have subschema's
	private Vector<MongooseSchema> subSchemas=new Vector<MongooseSchema>();
	protected void removeAllSubSchemas(){subSchemas.clear();}
	public boolean containsASubSchemaCalled(String subSchemaName){
		for(MongooseSchema subSchema:subSchemas)if(subSchema.getName().equalsIgnoreCase(subSchemaName))return true;
		return false;
	}
	public MongooseSchema getSubSchemaCalled(String subSchemaName){
		for(MongooseSchema subSchema:subSchemas)if(subSchema.getName().equalsIgnoreCase(subSchemaName))return subSchema;
		return null;
	}
	boolean addSubSchema(MongooseSchema subSchema){
		if(subSchema==null)return false;
		if(subSchemas.contains(subSchema))return true;
		if(!subSchemas.add(subSchema))return false;
		for(SubSchemaListener subSchemaListener:subSchemaListeners)try{subSchemaListener.subSchemaAdded(subSchema);}catch(Exception ex){}
		if(fieldTypeCollectionListener!=null)
			try{fieldTypeCollectionListener.fieldTypeAdded(this,subSchema);}catch(Exception ex){}
		return true;
	}
	// MDH@24OCT2018: convenience method to be able to add a subschema if it's not there already, or otherwise create a new one and add it
	MongooseSchema newSubSchemaCalled(String subSchemaName){
		for(MongooseSchema subSchema:subSchemas)if(subSchema.getName().equalsIgnoreCase(subSchemaName))return subSchema;
		MongooseSchema subSchema=new MongooseSchema(subSchemaName,this,null); // subschema's are NOT part of a collection!!
		return(addSubSchema(subSchema)?subSchema:null);
	}
	boolean removeSubSchema(MongooseSchema subSchema){
		if(subSchema==null)return false;
		if(!subSchemas.contains(subSchema))return true;
		if(!subSchemas.remove(subSchema))return false;
		for(SubSchemaListener subSchemaListener:subSchemaListeners)try{subSchemaListener.subSchemaRemoved(subSchema);}catch(Exception ex){}
		if(fieldTypeCollectionListener!=null)try{fieldTypeCollectionListener.fieldTypeRemoved(this,subSchema);}catch(Exception ex){}
		return true;
	}
	public List<MongooseSchema> getSubSchemas(){return Collections.unmodifiableList(subSchemas);} // iterable, but not modifyable!!!
	// end subschema support

	private MongooseSchema parentSchema=null;
	// setParent is created as public to allow setting the parent if we hadn't before (through the constructor)
	// NO we're sticking to
	private boolean setParent(MongooseSchema parentSchema){
		if(parentSchema!=null&&this.parentSchema!=null)return false; // can only set the parent once!!!!
		if(parentSchema!=null){ // parent schema getting set
			if(!parentSchema.addSubSchema(this))return false; // if we fail to register it with the parent NO go
		}else{ // parent schema getting retracted
			if(!parentSchema.removeSubSchema(this))return false;
		}
		this.parentSchema=parentSchema;
		return true;
	}
	public MongooseSchema getParent(){return parentSchema;}

	private Field autoIncrementedField=null; // the field designated as auto-increment field!!!
	private String name;

	public String getName(){
		return name;
	}

	private boolean synced=false; // assume not synced initially (not until after the contents was loaded)

	public boolean isSynced(){
		// if one of the sub schema's is NOT synced, I'm not synced
		if(!subSchemas.isEmpty())for(MongooseSchema subSchema:subSchemas)if(!subSchema.isSynced()){Utils.consoleprintln("Sub schema '"+subSchema.getRepresentation(false)+"' not synced!");return false;}
		// all subschema's are synced!!!
		return synced; // my synced flag determines the result!!!
	}

	private Vector<SyncListener> tableSyncListeners=new Vector<SyncListener>();
	public boolean addSchemaSyncListener(SyncListener tableSyncListener){
		return (tableSyncListener!=null?tableSyncListeners.contains(tableSyncListener)||tableSyncListeners.add(tableSyncListener):false);
	}
	public boolean deleteSchemaSyncListener(SyncListener tableSyncListener){
		return (tableSyncListener!=null?!tableSyncListeners.contains(tableSyncListener)||tableSyncListeners.remove(tableSyncListener):false);
	}

	private String tag="";
	public void setTag(String tag){
		if(tag==null)return;
		if(tag.equals(this.tag))return;
		this.tag=tag;
		setSynced(false);
	}
	public String getTag(){return tag;}

	// as we require that associatedFile is initialized in the constructor, subclasses cannot call my constructor, but may call initialize and checkFieldCollection()
	// alternatively passing the associatedFile into the constructor is perhaps also a good idea
	protected String getAssociatedFilename(){
		return name+".msd";
	}

	// where to load a subschema from depends on the parent!!
	protected MongooseSchema(){}
	public MongooseSchema(String name,MongooseSchema parentSchema,MongooseSchemaCollection collection){
		this.name=name;
		if(collection!=null)if(collection.add(this))this.collection=collection;else Utils.setInfo(this,"Failed to register schema '"+name+"' with its collection.");
		if(parentSchema!=null)setParent(parentSchema);
		fieldCollection=new FieldCollection(); // constructor looks at parentSchema to determine what type of _id to add!!! BUT we need it BEFORE calling load()!!!
		if(parentSchema==null)load(); // load the contents of the schema using the associated text file lines producer (to start with)
		/* MDH@02NOV2018: this would NOT be the right place to add the _id field, because parseTextLines() might get called AFTERWARDS, moved over to the constructor of FieldCollection, plus allowing replacement
		// MDH@15OCT2018: the automatic _id field can only be disabled on a parentless schema
		if(!fieldCollection.containsFieldWithName("_id")&&!fieldCollection.add(new Field("_id").setType(MongooseFieldType.OBJECTID).setDisabable(this.parentSchema!=null)))
			Utils.setInfo(this,"ERROR: Failed to add automatic _id field to schema '"+getRepresentation(false)+"'.");
		*/
	}
	public MongooseSchema(String name,MongooseSchemaCollection collection){this(name,null,collection);} // a main schema (not a subschema!!)

	// MDH@29OCT2018: instead of 'knowing' something changed we check it by constructing the text output and comparing it with what was retrieved
	private void checkSynced(){
		if(loadException!=null)return; // no need to check because synced is False now...
		// if the file did not exist, there's no need to check as well
	}

	public void fieldChanged(Field field){
		if(!fieldCollection.contains(field))return;
		// shouldn't happen though
		Utils.consoleprintln("External field '"+field.getName()+"' considered"+(field.isChanged()?" ":" NOT")+" changed!");
		// Ok, if another field registered as auto-increment field as well, we have to turn the auto increment off of that other field!!!!
		if(field.isAutoIncremented()){ // if the changed field now has the auto-incremented flag set
			if(autoIncrementedField!=null&&!field.equals(autoIncrementedField))
				autoIncrementedField.startAtLiteral.setDisabled(true); // disabling the start at literal of the autoIncrementedField will do the trick!!!
		}else{ // no longer marked to be an auto-increment field!!
			if(field.equals(autoIncrementedField)) autoIncrementedField=null;
		}
		// MDH@15OCT2018 BUG FIX: only do the following IFF field.isChanged() is True!!!
		if(field.isChanged())checkSynced(); // replacing:setSynced(false);
	}

	// exposes if requested the list of types
	public List<Field> getFieldsOfType(IFieldType fieldType){
		Vector<Field> fields=new Vector<Field>();
		for(Field field:fieldCollection)if(field.getType().equals(fieldType))if(!fields.add(field)) break;
		return fields;
	}

	// and if need be the type of the _id field
	public IFieldType getTypeOfIdField(){
		for(Field field:fieldCollection)if(field.getName().equalsIgnoreCase("_id"))return field.getType();
		return MongooseFieldType.OBJECTID;
	}

	// MDH@15OCT2018: should be able to install a default field change listener
	private IFieldChangeListener fieldChangeListener=null;
	void setFieldChangeListener(IFieldChangeListener fieldChangeListener){
		if(this.fieldChangeListener!=null){
			for(Field field:fieldCollection)field.deleteFieldChangeListener(this.fieldChangeListener);
		}
		this.fieldChangeListener=fieldChangeListener;
		if(this.fieldChangeListener!=null){
			for(Field field:fieldCollection)field.addFieldChangeListener(this.fieldChangeListener);
		}
	}
	private boolean add(Field field){
		if(field==null)return false;
		if(fieldCollection.add(field)) {
			// listen in to changes to the field from now on (although I'm now synced as we speak because we have an unsaved field!!!)
			if(!field.addFieldChangeListener(this))Utils.setInfo(this,"ERROR: Mongoose schema '"+name+"' failed to start listening to changes to field '"+field.getName()+"'.");
			if(fieldChangeListener!=null&&!field.addFieldChangeListener(fieldChangeListener))Utils.setInfo(this,"ERROR: Default field change listener failed to start listening to changes to field '"+field.getName()+"'.");
			setSynced(false);
			return true;
		}
		Utils.setInfo(this,"ERROR: Failed to add field '"+field.getName()+"'.");
		return false;
	}

	Vector<String> controllerTextLines=null;
	private void addControllerTextLine(String textLine)throws Exception{
		if(!controllerTextLines.add(textLine))throw new Exception("Failed to add controller text line '"+textLine+"'.");
	}
	private String[] getControllerTextLines() throws Exception{
		if(controllerTextLines!=null)controllerTextLines.clear();else controllerTextLines=new Vector<String>();
		addControllerTextLine("/*");
		addControllerTextLine(" * Generated by: Office Vitae Mongoose Schema Designer");
		addControllerTextLine(" * At: "+Utils.getTimestamp());
		addControllerTextLine(" * Author: <Enter your name here>");
		addControllerTextLine(" */");
		addControllerTextLine("const "+Utils.capitalize(name)+"=require('../models/"+name+".model.js);");
		// this allows one to create instances of this model with data, and store it by calling save
		// we're going to write skeloton methods for create, findOne, findAll, update and delete
		addControllerTextLine("exports.create=function(req,res){");
		addControllerTextLine("\t// Step 1. Check the received input (in req.body) and return a res.status(400).send({error:<error message>}); with an appropriate error message when something is wrong!");
		addControllerTextLine("\t");
		addControllerTextLine("\t// Step 2. Create a new object with the received data (properties of reg.body));");
		addControllerTextLine("\tconst "+name+"=new "+Utils.capitalize(name)+"({");
		// I suppose I can write the names of the required fields here passing in a null value initially???
		boolean optionalfields=false;
		for(Field field:fieldCollection)
			if(field.isRequired())addControllerTextLine("\t\t\t\t"+field.getName().toLowerCase()+":null, // TODO replace null with the property from req.body that contains the required data");
			else if(!field.isAutoIncremented())optionalfields=true;
		if(optionalfields){
			StringBuilder controllerTextLine=new StringBuilder();
			for(Field field:fieldCollection)if(!field.isRequired()&&!field.getName().equals("_id"))controllerTextLine.append(", "+field.getName().toLowerCase());
			//////if(controllerTextLine.indexOf(',')!=controllerTextLine.lastIndexOf(','))
			addControllerTextLine("\t\t\t\t// TODO insert initialization of optional fields ("+(controllerTextLine.charAt(0)==','?controllerTextLine.substring(2):controllerTextLine.toString())+") here");
		}
		addControllerTextLine("\t\t\t});");
		addControllerTextLine("\t// Step 3. Save the newly created instance");
		addControllerTextLine("\t"+name+".save().then(data=>{");
		addControllerTextLine("\t\tres.send(data); // or whatever else you want to send");
		addControllerTextLine("\t}).catch(err=>{");
		addControllerTextLine("\t\tres.status(500).send({");
		addControllerTextLine("\t\t\terror:err.message||'Some error occurred trying to add a "+name+"'.");
		addControllerTextLine("\t\t});");
		addControllerTextLine("\t});");
		addControllerTextLine("}");
		addControllerTextLine("exports.findAll=function(req,res){");
		addControllerTextLine("}");
		addControllerTextLine("exports.findOne=function(req,res){");
		addControllerTextLine("}");
		addControllerTextLine("exports.update=function(req,res){");
		addControllerTextLine("}");
		addControllerTextLine("exports.delete=function(req,res){");
		addControllerTextLine("}");
		return(String[])controllerTextLines.toArray(new String[controllerTextLines.size()]);
	}
	private ITextLinesProducer controllerTextLinesProducer=null;
	ITextLinesProducer getControllerTextLinesProducer(){
		if(controllerTextLinesProducer==null)
			controllerTextLinesProducer=new ITextLinesProducer(){
				String[] controllerTextLines;
				public void produceTextLines()throws Exception{controllerTextLines=getControllerTextLines();}
				public String[] getProducedTextLines(){return controllerTextLines;}
			};
		return controllerTextLinesProducer;
	}

	private Vector<String> routesTextLines=null;
	private void addRoutesTextLine(String routesTextLine)throws Exception{
		if(!routesTextLines.add(routesTextLine))throw new Exception("Failed to add routes text line '"+routesTextLine+".");
	}
	private String[] getRoutesTextLines()throws Exception{
		if(routesTextLines==null)routesTextLines=new Vector<String>();else routesTextLines.clear();
		// DONE add routes text lines here!!!
		// MDH@11SEP2018: linking routes to functions for CRUD operations on USERS
		/*

module.exports=(app)=>{

    const users=require('../controllers/user.controller.js');

    app.post('/users',users.create);

    app.get('/users',users.findAll);

    app.get('/users/:userId',users.findOne);

    app.put('/users/:userId',users.update);

    app.delete('/users/:userId',users.delete);
}
*/
		String lowerName=name.toLowerCase();
		addRoutesTextLine("/*");
		addRoutesTextLine(" * Generated by: Office Vitae Mongoose Schema Designer");
		addRoutesTextLine(" * At: "+Utils.getTimestamp());
		addRoutesTextLine(" * Author: <Enter your name here>");
		addRoutesTextLine(" */");
		addRoutesTextLine("module.exports=(app)=>{");
		addRoutesTextLine("\tconst "+lowerName+"s=require('../controllers/"+lowerName+".controller.js');");
		addRoutesTextLine("\tapp.post('/"+lowerName+"s',"+lowerName+"s.create);");
		addRoutesTextLine("\tapp.get('/"+lowerName+"s',"+lowerName+"s.findAll);");
		addRoutesTextLine("\tapp.get('/"+lowerName+"s/:"+lowerName+"Id',"+lowerName+"s.findOne);");
		addRoutesTextLine("\tapp.put('/"+lowerName+"s/:"+lowerName+"Id',"+lowerName+"s.update);");
		addRoutesTextLine("\tapp.delete('/"+lowerName+"s/:"+lowerName+"Id',"+lowerName+"s.delete);");
		addRoutesTextLine("}");
		return (String[])routesTextLines.toArray(new String[routesTextLines.size()]);
	}
	private ITextLinesProducer routesTextLinesProducer=null;
	ITextLinesProducer getRoutesTextLinesProducer(){
		if(routesTextLinesProducer==null)
			routesTextLinesProducer=new ITextLinesProducer(){
				String[] routesTextLines;
				public void produceTextLines()throws Exception{routesTextLines=getRoutesTextLines();}
				public String[] getProducedTextLines(){return routesTextLines;}
			};
		return routesTextLinesProducer;
	}

	// yes, we make a distinction between a text lines producer (to generate the model text lines) which would be me) and a text lines consumer which should be the file the text lines are to be written to
	// NOTE for a JavaScriptMongooseSchema the producer would be the text file as well
	// TODO perhaps we only need to create it once
	private ITextLinesProducer modelTextLinesProducer=null;
	ITextLinesProducer getModelTextLinesProducer(){
		if(modelTextLinesProducer==null)
			modelTextLinesProducer=new ITextLinesProducer(){
				String[] modelTextLines;
				public void produceTextLines()throws Exception{modelTextLines=getModelTextLines();}
				public String[] getProducedTextLines(){return modelTextLines;}
			};
		return modelTextLinesProducer;
	}
	ITextLinesConsumer getModelTextLinesConsumer(){
		if(modelTextFile==null)modelTextFile=new ITextLinesConsumer.TextFile(new java.io.File("./app/models","ovmsd."+name+".model.js"));
		return modelTextFile;
	}
	private void setSynced(boolean synced){
		/////////if(this.synced==synced)return;
		this.synced=synced;
		Utils.consoleprintln("Schema '"+this.getRepresentation(false)+"'"+(this.synced?" ":" NOT ")+"synced.");
		if(this.synced) for(Field field:fieldCollection)field.setChanged(false); // all fields should now be considered unchanged!!!
		if(tableSyncListeners!=null)for(SyncListener tableSyncListener:tableSyncListeners)try{tableSyncListener.syncChanged(this);}catch(Exception ex){}
	}
	public String getRepresentation(boolean showSynced){
		// toString() should return the full name
		// but now we have a problem in that the synced state depends on the parent state as well?????
		// a parent sync state should depend on that of its children as well, but not vice versa
		// assuming the children are saved in files of their own!!!
		String representation=name;
		if(showSynced&&!isSynced())representation+="*"; // MDH@19OCT2018: much easier than doing it all ourselves!!!
		// if any parent, prepend the representation of the parent (without sync info!!)
		// MDH@16OCT2018: switched to using a $ between the different parts because that is a character we can use in JavaScript names (and a period we can't!!)
		if(parentSchema!=null)representation=parentSchema.getRepresentation(false)+"_"+representation; // changing $ to _ as that looks nicer
		return representation;
	}
	public String toString(){
		String representation=name;
		if(!isSynced())representation+="*";
		return representation;
		// replacing: return getRepresentation(true);
	}
	/*
	public boolean equals(Object o){
		try{return name.equalsIgnoreCase(((MongooseSchema)o).getName());}catch(Exception ex){}
		return false;
	}
	*/
	/*
	private void writeRoutes(File appDirectory){
		try{
			// ascertain that the routes subdirectory exists
			File routesDirectory=new File(appDirectory,"routes");
			boolean routesDirectoryExists=Utils.directoryShouldExist(routesDirectory);
			if(!routesDirectoryExists){
				Utils.setInfo(this,"The 'app/routes' subdirectory does not exist, or is not a directory.");
				return;
			}
			// this is not just writing the entire model file although it could but alternatively replace the mongoose schema declaration
			// BUT we only allow that on a model file with a schema that has been created with
			File f=new File(routesDirectory,"ovmsd."+name+".routes.js"); // MDH@24OCT2018: created by this Office Vitae Mongoose Schema Designer
			// for now let's decide to always overwrite the thing
			if(f.exists()){
				Utils.setInfo(this,"The routes file of table "+name+" already exists, and will not be overwritten.");
				return;
			}
			PrintWriter pw=new PrintWriter(new FileWriter(f,false));
			// TODO write the routes
			pw.close();
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' in writing the routes of table "+name+".");
		}
	}
	*/
	/*
	private void writeController(File appDirectory){
		try{
			// ascertain that the models subdirectory exists
			File controllersDirectory=new File(appDirectory,"controllers");
			boolean controllersDirectoryExists=Utils.directoryShouldExist(controllersDirectory);
			if(!controllersDirectoryExists){
				Utils.setInfo(this,"The 'app/controllers' subdirectory does not exist, or is not a directory.");
				return;
			}
			// this is not just writing the entire model file although it could but alternatively replace the mongoose schema declaration
			// BUT we only allow that on a model file with a schema that has been created with
			File f=new File(controllersDirectory,"ovmsd."+name+".controller.js"); // MDH@24OCT2018: same here...
			// for now let's decide to always overwrite the thing
			if(f.exists()){
				Utils.setInfo(this,"The controller file of table "+name+" already exists, and will not be replaced.");
				return;
			}
			PrintWriter pw=new PrintWriter(new FileWriter(f,false));
			// the first line it to get the model (as an object constructor)
			pw.println("const "+Utils.capitalize(name)+"=require('../models/"+name+".model.js);");
			// this allows one to create instances of this model with data, and store it by calling save
			// we're going to write skeloton methods for create, findOne, findAll, update and delete
			pw.println("exports.create=function(req,res){");
			pw.println("\t// Step 1. Check the received input (in req.body) and return a res.status(400).send({error:<error message>}); with an appropriate error message when something is wrong!");
			pw.println("\t");
			pw.println("\t// Step 2. Create a new object with the received data (properties of reg.body));");
			pw.println("\tconst "+name+"=new "+Utils.capitalize(name)+"({");
			// I suppose I can write the names of the required fields here passing in a null value initially???
			boolean optionalfields=false;
			for(Field field:fieldCollection)
				if(field.isRequired()) pw.println("\t\t\t\t"+field.getName().toLowerCase()+":null, // TODO replace null with the property from req.body that contains the required data");
				else if(!field.isAutoIncremented()) optionalfields=true;
			if(optionalfields){
				pw.print("\t\t\t\t// TODO insert initialization of optional fields");
				for(Field field:fieldCollection) if(!field.isRequired()) pw.print(" "+field.getName().toLowerCase());
				pw.println(" here");
			}
			pw.println("\t\t\t});");
			pw.println("\t// Step 3. Save the newly created instance");
			pw.println("\tuser.save().then(data=>{");
			pw.println("\t\tres.send(data); // or whatever else you want to send");
			pw.println("\t}).catch(err=>{");
			pw.println("\t\tres.status(500).send({");
			pw.println("\t\t\terror:err.message||'Some error occurred trying to add a "+name+".");
			pw.println("\t\t});");
			pw.println("\t});");
			pw.println("}");
			pw.println("exports.findAll=function(req,res){");
			pw.println("}");
			pw.println("exports.findOne=function(req,res){");
			pw.println("}");
			pw.println("exports.update=function(req,res){");
			pw.println("}");
			pw.println("exports.delete=function(req,res){");
			pw.println("}");

			pw.close();
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' in writing the controller of table "+name+".");
		}
	}
	*/
	private boolean containsFieldWithName(String fieldName){return fieldCollection.containsFieldWithName(fieldName);}
	public Field getFieldCalled(String fieldName){
		for(Field field:fieldCollection)if(field.getName().equalsIgnoreCase(fieldName))return field;
		return null;
	}
	// MDH@16OCT2018: writing the schemas means writing the subschemas first, followed by writing the main schema
	//                I suppose it's the schema's that can be overwritten, so should be marked somehow????
	//                the problem is that additional code can be placed between the schema definitions, so I guess we can put the schema names in the annotation: /*MSD:<schemaname>*/
	public boolean hasSubSchemas(){return(subSchemas!=null&&!subSchemas.isEmpty());} // convenient method to determine if it has any subschema's defined at all!!!
	List<String> getModelSchemaCreationLines(){
		Utils.setInfo(this,"Constructing the schema creation and model text of schema '"+getName()+"'.");
		Vector<String> modelSchemaCreationLines=new Vector<String>();
		Utils.setInfo(this,"\tNumber of subschemas: "+subSchemas.size()+".");
		if(!subSchemas.isEmpty())for(MongooseSchema subSchema:subSchemas)modelSchemaCreationLines.addAll(subSchema.getModelSchemaCreationLines());
		// now we can write the lines that define this subschema
		String mongooseSchemaDeclaration="const "+getDescription().toString()+"=mongoose.Schema({";
		String mongooseSchemaDeclarationPrefix=String.join("",Collections.nCopies(mongooseSchemaDeclaration.length()," "));
		modelSchemaCreationLines.add(mongooseSchemaDeclaration); // the description appends the word 'Schema' itself!!
		// write one field per line following
		Field autoIncrementedField=null;
		String fieldName;
		boolean explicitIdField;
		boolean hasVirtualFields=false;
		for(Field field:fieldCollection){
			// MDH@16OCT2018: do NOT publish disabled fields!
			if(!field.isEnabled())continue;
			if(field.isVirtual()){hasVirtualFields=true;continue;}
			// MDH@01NOV2018: any field collection now contains a field called _id that could become the explicit declared _id field replacing the implicit _id field but only when it is different
			//                it is considered to be different if the type is different (i.e. not ObjectId) or some property is set
			//                for now we're only allowing a change when _id has another type
			fieldName=field.getName()/*.toLowerCase()*/;
			// if we have this enabled _id and its type is NOT ObjectId we allow it to replace the implicit (default) _id field...
			// NOTE that we have forced _id to be both unique and required, so that it will always function as a unique _id
			explicitIdField=(fieldName.equals("_id")); // mark this field as the explicit _id field if it's name is _id
			if(explicitIdField)if(field.getType().equals(MongooseFieldType.OBJECTID))continue; // no need for an explicit _id definition as the implicit _id definition suffices!!!
			// TODO prevent certain properties to be written when dealing with an explicit Id field like default, alias, ref
			// the most simple form is just the name of the field, a colon and the type
			// but for now let's not do that
			// using the description to represent the type (so we'd get a$aSchema instead of a$a)
			StringBuilder fieldTextRepresentation=new StringBuilder();
			// MDH@25OCT2018: a Map will wrap itself in an object {type:Map,of:<value rep>} and we may safely remove this DONE could we move this to Field as in getFieldTypeRepresentation??
			////////replacing: String fieldTypeRepresentation=field.getType().toString();
			fieldTextRepresentation.append("type:"+field.getTypeRepresentation(false)); // MDH@25OCT2018: again, NOT writing the description but the FULL name (particularly important for MapFieldType instances)
			if(!field.isAutoIncremented()){
				// if a ref property is defined write that before anything else
				if(!field.refLiteral.isDisabled()&&field.refLiteral.isValid())fieldTextRepresentation.append(",ref:"+field.refLiteral.getValue());
				// I suppose the index is also quite important
				// MDH@25OCT2018: any valid index value is either text 'unique', 'index' or 'sparse' (currently)
				if(!field.indexTypeLiteral.isDisabled()&&field.indexTypeLiteral.isValid())fieldTextRepresentation.append(","+field.indexTypeLiteral.getText().toLowerCase()+":true");
				/* replacing
				if(field.isUnique()) fieldTextRepresentation.append(",unique:true");
				else if(field.isIndex()) fieldTextRepresentation.append(",index:true");
				else if(field.isSparse()) fieldTextRepresentation.append(",sparse:true");
				*/
				// general option flags
				if(field.isRequired())fieldTextRepresentation.append(",required:true");
				if(!field.isSelect())fieldTextRepresentation.append(",select:false"); // select:true is the default!!!
				// general options
				if(field.aliasLiteral.isValid()&&!field.aliasLiteral.isDisabled())fieldTextRepresentation.append(",alias:'"+field.aliasLiteral.getValue()+"'");
				if(!field.defaultLiteral.isDisabled()&&field.defaultLiteral.isValid())fieldTextRepresentation.append(",default:"+field.defaultLiteral.getValue()); // assuming getValue will quote the text if it's a String default????
				// type-specific options
				IFieldType fieldType=field.getType();
				if(fieldType instanceof MongooseFieldType) switch(((MongooseFieldType)fieldType).ordinal()){
					case Field.DATE_FIELD:
						if(!field.minDateLiteral.isDisabled()&&field.minDateLiteral.isValid())fieldTextRepresentation.append(",min:"+field.minDateLiteral.getValue());
						if(!field.maxDateLiteral.isDisabled()&&field.maxDateLiteral.isValid())fieldTextRepresentation.append(",max:"+field.maxDateLiteral.getValue());
						break;
					case Field.NUMBER_FIELD:
						if(!field.minNumberLiteral.isDisabled()&&field.minNumberLiteral.isValid())fieldTextRepresentation.append(",min:"+field.minNumberLiteral.getValue());
						if(!field.maxNumberLiteral.isDisabled()&&field.maxNumberLiteral.isValid())fieldTextRepresentation.append(",max:"+field.maxNumberLiteral.getValue());
						break;
					case Field.STRING_FIELD:
						if(field.isLowercase())fieldTextRepresentation.append(",lowercase:true");
						else if(field.isUppercase())fieldTextRepresentation.append(",uppercase:true");
						if(field.isTrim())fieldTextRepresentation.append(",trim:true");
						if(!field.minLengthLiteral.isDisabled()&&field.minLengthLiteral.isValid())
							fieldTextRepresentation.append("minlength:"+field.minLengthLiteral.getValue());
						if(!field.maxLengthLiteral.isDisabled()&&field.maxLengthLiteral.isValid())
							fieldTextRepresentation.append("maxlength:"+field.minLengthLiteral.getValue());
						if(!field.matchLiteral.isDisabled()&&field.matchLiteral.isValid()) fieldTextRepresentation.append(",match:new RegExp("+field.matchLiteral.getValue()+")"); // assuming the user did NOT enclose the regular expression between / and /
						if(!field.valuesLiteral.isDisabled()&&field.valuesLiteral.isValid()) fieldTextRepresentation.append("enum:['"+String.join("','",field.valuesLiteral.getValue())+"'']");
						break;
				}
			}else
				autoIncrementedField=field;
			int firstColonPos=fieldTextRepresentation.indexOf(":");
			String fieldTag=field.getTag(); // MDH@30OCT2018: like to see the tag written as comment as well!!
			if(firstColonPos!=fieldTextRepresentation.lastIndexOf(":")) // not just the the type is present in the field text representation
				modelSchemaCreationLines.add(mongooseSchemaDeclarationPrefix+fieldName+":{"+fieldTextRepresentation.toString()+"},"+(fieldTag!=null&&!fieldTag.isEmpty()?" // "+fieldTag:""));
			else // just the type is present and we can use the shorthand notation
				modelSchemaCreationLines.add(mongooseSchemaDeclarationPrefix+fieldName+fieldTextRepresentation.substring(firstColonPos)+","+(fieldTag!=null&&!fieldTag.isEmpty()?" // "+fieldTag:""));
			///////fieldTextRepresentation.append(" /*MSD:F*/"); // marks a MSD field line
		}
		StringBuilder optionsTextRepresentation=new StringBuilder(mongooseSchemaDeclarationPrefix.substring(1)+"}");
		// add any options if we have them
		// only subschema's can NOT have _id fields in which case the user disabled it (which is not possible for main schema's)
		// TODO collect all the options from the schema
		Vector<String> optionTexts=new Vector<String>();
		if(this.getParent()!=null){
			if(!this.containsFieldWithName("_id")||!this.getFieldCalled("_id").isEnabled())optionTexts.add("_id:false");
		}else{

		}
		if(!optionTexts.isEmpty())optionsTextRepresentation.append(",{"+String.join(",",optionTexts)+"}");
		optionsTextRepresentation.append(");");
		////////optionsTextRepresentation.append(" /*MSD:O*/"); // close the schema assignment (on the same line, so we save a little space!!! let's call this the O(ptions) line
		modelSchemaCreationLines.add(optionsTextRepresentation.toString());
		if(autoIncrementedField!=null){
			modelSchemaCreationLines.add("");
			// NO LONGER REQUIRED WITH mongoose-plugin-autoinc: pw.println("// DO NOT FORGET TO initialize THE PLUGIN IN YOUR app.js WITH mongoose.connection!");
			modelSchemaCreationLines.add("import { autoIncrement } from 'mongoose-plugin-autoinc';"); // MDH@24SEP2018: switched to an updated version of mongoose-auto-increment (which is 3 years old and requires a version 4 of Mongoose)
			modelSchemaCreationLines.add(name.toLowerCase()+"Schema.plugin(autoIncrement,{model:'"+Utils.capitalize(name)+"',field:'"+autoIncrementedField.getName()+"',startAt:"+autoIncrementedField.startAtLiteral.getValue()+",incrementBy:1});");
			// being able to reset the counter would be nice, for which we need the connection and apparently mongoose.connection holds the connection (see app.js)
			// TODO figure out when exactly the reset occurs????
			///modelSchemaCreationLines.add("/* TO RESET AUTO-INCREMENT FIELD "+autoIncrementedField.getName()+" SAVE AN EMPTY INSTANCE, LIKE THIS:");
			///modelSchemaCreationLines.add("const "+Utils.capitalize(name)+"=mongoose.model('"+Utils.capitalize(name)+"',"+Utils.capitalize(name)+"Schema);"); // mongoose.model will use the default mongoose connection i.e. mongoose.connection!!!
			///modelSchemaCreationLines.add(name+"=new "+Utils.capitalize(name)+"();"); // get an instance
			///modelSchemaCreationLines.add(name+".save(function(err){"+name+".nextCount(function(err,count){"+name+".resetCount(function(err,nextCount){});});});");
			///modelSchemaCreationLines.add("*/");
		}
		if(hasVirtualFields){
			// adding a single line for each field that is a virtual!!!
			StringBuilder virtualFieldText;
			for(Field field:fieldCollection)if(field.isVirtual()){
				virtualFieldText=new StringBuilder(name+"Schema.virtual('"+field.getName()+"')");
				if(!field.getLiteral.isDisabled()&&field.getLiteral.isValid())virtualFieldText.append(".get("+field.getLiteral.getValue()+")");
				if(!field.setLiteral.isDisabled()&&field.setLiteral.isValid())virtualFieldText.append(".set("+field.setLiteral.getValue()+")");
				virtualFieldText.append(";");
				modelSchemaCreationLines.add(virtualFieldText.toString());
			}
		}
		Utils.setInfo(this,"\tNumber of model lines: "+modelSchemaCreationLines.size()+".");
		return modelSchemaCreationLines;
	}
	// keeping the model in a text file now the question when to actually read and write it
	// I suppose read at start, so we have access to the text lines, and write at save!!
	// in between we allow editing the text, extracting
	// basically the .msd file is leading, defining the schema and subschemas
	// but fields can also be parsed from the model file which is problematic if we have both!!!
	// unless we decide to also write the internal field definition to the model file allowing as to import them back from the model file
	// we can do that on the same line of the field as a commment at the end or on the line before!!!
	// which is like embedding the .msd in the model file, I'd have to think about that...
	// it's better to just mark the field lines in the model file, so they can be read if .msd is missing!!!
	private String[] getModelTextLines(){
		Vector<String> modelTextLines=new Vector<String>();
		modelTextLines.add("/*");
		modelTextLines.add(" * Generated by: Office Vitae Mongoose Schema Designer");
		modelTextLines.add(" * At: "+Utils.getTimestamp());
		modelTextLines.add(" * Author: <Enter your name here>");
		if(tag!=null&&!tag.trim().isEmpty())modelTextLines.add(" * Description: "+tag); // the tag provides us with a description...
		modelTextLines.add(" */");
		modelTextLines.add("");
		modelTextLines.add("const mongoose=require('mongoose');");
		modelTextLines.add("require('mongoose-long')(mongoose);"); // Long (=integer) support TODO can we make this optional
		modelTextLines.add("var Int32=require('mongoose-int32');"); // Int32 support TODO can we make this optional (when it is actually used in the schema??)
		modelTextLines.add("");
		// write all subschema (and myself of course)
		modelTextLines.addAll(getModelSchemaCreationLines());
		modelTextLines.add("");
		// the first argument is the singular form of the document collection (table), typically what is returned (the model) is used as constructor therefore usually assigned to something that starts with a capital (e.g. Areamap,User,etc.)
		modelTextLines.add("module.exports=mongoose.model('"+name.toLowerCase()+"',"+name.toLowerCase()+"Schema);"); // the exports statement with first argument the singular version of the collection e.g. areamap (so the collection used will be areamaps)
		return(modelTextLines.isEmpty()?new String[]{}:(String[])modelTextLines.toArray(new String[modelTextLines.size()]));
	}
	private ITextLinesConsumer.TextFile modelTextFile;
	/* MDH@24OCT2018 not writing the model ourselves directly anymore!!!
	private void writeModel(File appDirectory){
		// basically
		try{
			// ascertain that the models subdirectory exists
			File modelsDirectory=new File(appDirectory,"models");
			boolean modelsDirectoryExists=Utils.directoryShouldExist(modelsDirectory);
			if(!modelsDirectoryExists){
				Utils.setInfo(this,"The 'app/models' subdirectory does not exist, or is not a directory.");
				return;
			}
			// this is not just writing the entire model file although it could but alternatively replace the mongoose schema declaration
			// BUT we only allow that on a model file with a schema that has been created with
			File f=new File(modelsDirectory,"ovmsd."+name+".model.js"); // MDH@24OCT2018: prefix the name with ovmsd. to indicate that it was created by this Office Vitae Mongoose Schema Designer app, so we will know which JavaScript model files are external to this app
			// for now let's decide to always overwrite the thing
			if(f.exists()){
				int dialogResult=JOptionPane.showConfirmDialog(null,"Replace the model file?","Warning",JOptionPane.YES_NO_OPTION);
				if(dialogResult!=JOptionPane.YES_OPTION){
					Utils.setInfo(this,"Writing the model canceled by the user.");
					return;
				}
			}
			PrintWriter pw=new PrintWriter(new FileWriter(f,false));
			pw.println("// MSD@"+Utils.getTimestamp()+": defines a "+name.toLowerCase()+" schema");
			pw.println();
			// 'include' the packages we need
			pw.println("const mongoose=require('mongoose');");
			// TODO make importing auto increment depend on whether there's an auto-increment field!!
			pw.println("const mongooseLong=require('mongoose-long');");
			pw.println();
			writeSchemas(pw);
			pw.println();
			pw.println("module.exports=mongoose.model('"+Utils.capitalize(name)+"',"+getDescription().toString()+");");
			pw.close();
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' in writing the model of table "+name+".");
		}
	}
	public void publish()throws IllegalCallerException{
		if(parentSchema!=null)throw new IllegalCallerException("A subschema cannot be published separately.");
		// we can write a sample app.js for testing purposes
		File appFile=new File("app.js");
		if(!appFile.exists()){
			// TODO what are we going to put in the app file????
		}
		// let's start with ascertaining that we have an app subfolder
		File appDirectory=new File(".","app");
		boolean appDirectoryExists=Utils.directoryShouldExist(appDirectory);
		if(!appDirectoryExists){
			Utils.setInfo(this,"The 'app' subdirectory does not exist, or is not a directory.");
			return;
		}
		// writing the model can now be achieved by passing the model text lines produces to the model text lines consumer (which is the model file)
		try{
			getModelTextLinesConsumer().setTextLines(getModelTextLinesProducer().getProducedTextLines()); // to write the model!!!
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' in writing the model file.");
		}
		////////replacing: writeModel(appDirectory);
		writeRoutes(appDirectory);
		writeController(appDirectory);
	}
	*/
	private String getFieldContents(Field field){return (field==null?"":field.getTextRepresentation(true,true));}

	protected IFieldType getMongooseFieldType(String fieldTypeName){
		if(fieldTypeName.isEmpty())return MongooseFieldType.MIXED; // e.g. as generic array element type!!!
		if(fieldTypeName.equalsIgnoreCase("array")) return MongooseFieldType.ARRAY;
		//////////if(fieldTypeName.equalsIgnoreCase("auto incremented integer"))return FieldType.AUTO_INCREMENT;
		if(fieldTypeName.equalsIgnoreCase("boolean")||fieldTypeName.equals("mongoose.Schema.Types.Boolean"))return MongooseFieldType.BOOLEAN;
		if(fieldTypeName.equalsIgnoreCase("buffer")||fieldTypeName.equals("mongoose.Schema.Types.Buffer"))return MongooseFieldType.BUFFER;
		if(fieldTypeName.equalsIgnoreCase("date")||fieldTypeName.equals("mongoose.Schema.Types.Date"))return MongooseFieldType.DATE;
		if(fieldTypeName.equalsIgnoreCase("int32"))return MongooseFieldType.INT32; // MDH@31OCT2018: long name NOT possible...
		if(fieldTypeName.equalsIgnoreCase("long")||fieldTypeName.equals("mongoose.Schema.Types.Long"))return MongooseFieldType.LONG;
		if(fieldTypeName.equalsIgnoreCase("map")||fieldTypeName.equals("mongoose.Schema.Types.Map"))return MongooseFieldType.MAP;
		if(fieldTypeName.equalsIgnoreCase("mixed")||fieldTypeName.equals("mongoose.Schema.Types.Mixed"))return MongooseFieldType.MIXED;
		if(fieldTypeName.equalsIgnoreCase("number")||fieldTypeName.equals("mongoose.Schema.Types.Number"))return MongooseFieldType.NUMBER;
		if(fieldTypeName.equalsIgnoreCase("objectid")||fieldTypeName.equals("mongoose.Schema.Types.ObjectId"))return MongooseFieldType.OBJECTID;
		if(fieldTypeName.equalsIgnoreCase("string")||fieldTypeName.equals("mongoose.Schema.Types.String"))return MongooseFieldType.STRING;
		return null;
	}
	private IFieldType getFieldType(String fieldTypeName){
		// we need to distinguish between Mongoose field types, composite field types (Array and Map) and subschema field types
		IFieldType mongooseFieldType=getMongooseFieldType(fieldTypeName);
		if(mongooseFieldType!=null)return mongooseFieldType;
		// perhaps an array???
		if(fieldTypeName.length()>=2&&fieldTypeName.charAt(0)=='['&&fieldTypeName.endsWith("]")){
			IFieldType arrayFieldType=new ArrayFieldType();
			if(fieldTypeName.length()>2)((ArrayFieldType)arrayFieldType).setSubFieldType(getFieldType(fieldTypeName.substring(1,fieldTypeName.length()-1)));
			return arrayFieldType;
		}
		// perhaps a Map???
		String mapFieldTypePrefix=MongooseFieldType.MAP.getDescription().toString();
		if(fieldTypeName.startsWith(mapFieldTypePrefix)){
			IFieldType mapFieldType=new MapFieldType();
			if(" of ".equals(fieldTypeName.substring(mapFieldTypePrefix.length(),mapFieldTypePrefix.length()+4)))
				((MapFieldType)mapFieldType).setSubFieldType(getFieldType(fieldTypeName.substring(mapFieldTypePrefix.length()+4)));
			return mapFieldType;
		}
		// should be a subschema!!!
		if(!subSchemas.isEmpty()){
			for(MongooseSchema subSchema:subSchemas){
				String subSchemaDescriptionName=subSchema.getDescription().toString();
				if(subSchemaDescriptionName.equals(fieldTypeName))return subSchema;
			}
		}
		Utils.setInfo(this,"WARNING: Type '"+fieldTypeName+"' not found!");
		return null;
	}

	protected Field getFieldFromContents(String fieldcontents){
		Field field=null;
		Utils.consoleprintln("Parsing field from '"+fieldcontents+"'.");
		String[] contentParts=fieldcontents.split("\t");
		if(contentParts.length>0){ // at least name and type required!!!
			// the name and type and optional default should be in the first element
			String[] nametypeParts=contentParts[0].split(":");
			if(nametypeParts.length>1&&nametypeParts[0].length()>1&&nametypeParts[1].length()>0){
				int equalPos;
				// MDH@15OCT2018: the name is preceded by the enabled flag!!
				field=new Field(nametypeParts[0].substring(1));
				String fieldTypeName=nametypeParts[1];
				// MDH@25OCT2018: interpreting the field type is a bit of a challenge now that we're using full Mongoose schema names (like Schema.Types.ARRAY)
				if(fieldTypeName.length()>1&&fieldTypeName.charAt(0)=='['&&fieldTypeName.endsWith("]")){
					field.setType(MongooseFieldType.ARRAY);
					field.setArrayElementType(getFieldType(fieldTypeName.substring(1,fieldTypeName.length()-1)));
				}else
					field.setType(getFieldType(fieldTypeName));
				field.setEnabled(nametypeParts[0].charAt(0)=='+');
				String fieldPropertyName, fieldPropertyValue;
				boolean first=true;
				for(String contentPart:contentParts){
					if(first==true){first=false;continue;} // skip first element (processed before)
					if(contentPart.trim().isEmpty())continue;
					// MDH@30OCT2018: anything starting with $ is considered a tag...
					if(contentPart.charAt(0)=='$'){setTag(contentPart.substring(1));continue;}
					equalPos=contentPart.indexOf('=');
					if(equalPos>1){ // not a flag
						// the first character indicates the flag
						boolean fieldPropertyFlag=(contentPart.charAt(0)=='-');
						fieldPropertyName=contentPart.substring(1,equalPos).trim();
						fieldPropertyValue=contentPart.substring(equalPos+1).trim();
                            /*
                            if(fieldPropertyName.equalsIgnoreCase("name"))field.name=fieldPropertyValue;else
                            if(fieldPropertyName.equalsIgnoreCase("type"))field.type=getFieldType(fieldPropertyValue);else
                            if(fieldPropertyName.equalsIgnoreCase("default"))field.defaultLiteral=fieldPropertyValue;else
                            */
						if(fieldPropertyName.equalsIgnoreCase("ref")){
							field.refLiteral.setDisabled(fieldPropertyFlag);
							field.refLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("alias")){
							field.aliasLiteral.setDisabled(fieldPropertyFlag);
							field.aliasLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("indextype")){ // MDH@25OCT2018
							field.indexTypeLiteral.setDisabled(fieldPropertyFlag);
							field.indexTypeLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("default")){
							field.defaultLiteral.setDisabled(fieldPropertyFlag);
							field.defaultLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("startat")){
							field.startAtLiteral.setDisabled(fieldPropertyFlag);
							field.startAtLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("values")){
							field.valuesLiteral.setDisabled(fieldPropertyFlag);
							field.valuesLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("minlength")){
							field.minLengthLiteral.setDisabled(fieldPropertyFlag);
							field.minLengthLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("maxlength")){
							field.maxLengthLiteral.setDisabled(fieldPropertyFlag);
							field.maxLengthLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyValue.equalsIgnoreCase("mindate")){
							field.minDateLiteral.setDisabled(fieldPropertyFlag);
							field.minDateLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("maxdate")){
							field.maxDateLiteral.setDisabled(fieldPropertyFlag);
							field.maxDateLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("minnumber")){
							field.minNumberLiteral.setDisabled(fieldPropertyFlag);
							field.minNumberLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("maxnumber")){
							field.maxNumberLiteral.setDisabled(fieldPropertyFlag);
							field.maxNumberLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("get")){
							field.getLiteral.setDisabled(fieldPropertyFlag);
							field.getLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("set")){
							field.setLiteral.setDisabled(fieldPropertyFlag);
							field.setLiteral.setText(fieldPropertyValue);
						}else if(fieldPropertyName.equalsIgnoreCase("validate")){
							field.validateLiteral.setDisabled(fieldPropertyFlag);
							field.validateLiteral.setText(fieldPropertyValue);
						}else
							Utils.setInfo(this,"ERROR: Invalid field property name '"+fieldPropertyName+"' in field property assignment '"+contentPart+"'.");
					}else /////if(contentPart.charAt(0)=='-'){ // something that is a flag
						switch(FIELD_FLAG_NAMES.indexOf(contentPart)){
							case 0:
								field.setVirtual(true);
								break;
							case 1:
								field.setRequired(true);
								break;
							case 2:
								field.setSelect(true);
								break;
							case 3:
								field.setSelect(false);
								break;
							case 4:
								field.setLowercase(true);
								break;
							case 5:
								field.setUppercase(true);
								break;
							case 6:
								field.setTrim(true);
								break;
							default:
								Utils.setInfo(this,"ERROR: Invalid field property flag '"+contentPart+"'.");
						}
					/////}
				}
			}
		}
		return field;
	}

	/*
	// I suppose we can save a table to a single file with the extension mt (Mongoose Table)
	public void save(){
		// TODO if we want to check whether saving a subschema succeeded check it's sync status!!!
		try{
			String fullSchemaName=getRepresentation(false);
			Utils.consoleprintln("Saving '"+fullSchemaName+"'.");
			if(!subSchemas.isEmpty())for(MongooseSchema subSchema:subSchemas)subSchema.save();
			// should use the extended name
			PrintWriter pw=new PrintWriter(new FileWriter("./"+fullSchemaName+".msd",false));
			// print the names of the subschema's!!
			if(!subSchemas.isEmpty()){
				StringBuilder subSchemaNames=new StringBuilder();
				for(MongooseSchema subSchema: subSchemas) subSchemaNames.append("\t"+subSchema.getName());
				pw.println(subSchemaNames.substring(1)); // write the names of the
			}else
				pw.println();
			// the only I need to do is write the fields
			for(Field field: this) pw.println(getFieldContents(field));
			pw.close();
			setSynced(true);
			if(!isSynced())
				Utils.consoleprintln("Schema '"+fullSchemaName+"' saved, but NOT (completely) synced!");
			else
				Utils.consoleprintln("Schema '"+fullSchemaName+"' and all its sub schemas saved!");
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' in saving table '"+name+"'.");
		}
	}
	*/
	// if we replace load() by registerSubSchema() and parseField() and external party can do the loading...

	/*
	private ITextLinesProducer.TextFile textLinesProvider=null; // where we're going to keep the lines but initially undefined
	// someone who wants to show the text (or parse it into properties like load() below!!!)should call getTextLines
	// ITextLinesProducer implementation
	// NOTE
	private String[] getSubSchemaNames(){
		String[] subSchemaNames=new String[subSchemas.size()];
		int subSchemaIndex=0;
		for(MongooseSchema subSchema:subSchemas)subSchemaNames[subSchemaIndex++]=subSchema.getName();
		return subSchemaNames;
	}
	*/
	private List<String> lastSubSchemasTextLines=null,lastFieldsTextLines=null; // the last constructed subschemas and fields text lines
	private List<String> getSubSchemasTextLines() throws Exception{
		Vector<String> allSubSchemaTextLines=new Vector<String>();
		String[] subSchemaTextLines;
		String subSchemaName;
		for(MongooseSchema subSchema:subSchemas){
			subSchemaName=subSchema.getName();
			if(!allSubSchemaTextLines.add(subSchemaName))throw new Exception("Failed to add the name of subschema '"+subSchema.getName()+"'.");
			subSchema.produceTextLines(); // MDH@02NOV2018: this might do the trick (see comment below)
			subSchemaTextLines=subSchema.getProducedTextLines();
			if(subSchemaTextLines==null)continue; // MDH@02NOV2018: can we prevent this from happening????
			// we have to indent all the returned text lines
			for(String subSchemaTextLine:subSchemaTextLines)
				if(!allSubSchemaTextLines.add("\t"+subSchemaTextLine))
					throw new Exception("Failed to add line '"+subSchemaTextLine+"' of subschema '"+subSchemaName+"'.");
		}
		return(lastSubSchemasTextLines=allSubSchemaTextLines);
	}
	private List<String> getFieldsTextLines() throws Exception{
		Vector<String> fieldsTextLines=new Vector<String>();
		for(Field field:fieldCollection)if(!fieldsTextLines.add(getFieldContents(field)))throw new Exception("Failed to append field '"+field.getName()+"'.");
		return(lastFieldsTextLines=fieldsTextLines);
	}
	// ITextLinesContainer implementation
	public void produceTextLines()throws Exception{
		//////if(parentSchema==null)if(isSynced())return null; // nothing changed, so return null (MDH@19OCT2018: but only when a main schema, subschema's shouldn't be that fresh!!)
		///if(textLines==null){ // no current contents known
			// initialize textLines to return with the text lines of all subschema's
			Vector<String> textLinesVector=new Vector<String>();
			if(tag!=null&&!tag.trim().isEmpty())textLinesVector.add("$"+tag); // MDH@30OCT2018: write the tag line as the first line
			textLinesVector.addAll(getSubSchemasTextLines()); // adding all the subschemas text lines
			textLinesVector.addAll(getFieldsTextLines()); // adding all the fields text lines
			textLines=(textLinesVector.isEmpty()?new String[]{}:textLinesVector.toArray(new String[textLinesVector.size()]));
		///}
	}
	public String[] getProducedTextLines(){return textLines;}
	// end ITextLinesContainer implementation

	// keep track of the last saved fields text lines and subschemas text lines separately
	private List<String> lastSavedFieldsTextLines=null,lastSavedSubSchemasTextLines=null;

	// a method to determine whether or not the text that would be written with certain new fields text lines would be unsaved at the moment
	public boolean unsavedWithFieldsTextLines(String[] fieldsTextLines){
		return!Utils.equalTextLists(fieldsTextLines==null||fieldsTextLines.length==0?null:Arrays.asList(fieldsTextLines),lastSavedFieldsTextLines);
	}

	private String[] textLines=null; // are locally produced (or loaded) text lines...

	protected void parseTextLines(String[] textLines)throws Exception{
		// we do not want to 'load' more than once!!!
		///////if(textLines==null)throw new NullPointerException("No text defined to initialize Mongoose schema "+getRepresentation(false)+" from.");
		lastFieldsTextLines=new Vector<String>();
		lastSubSchemasTextLines=new Vector<String>();
		int lineCount=(textLines!=null?textLines.length:0);
		Utils.consoleprintln("Number of lines to process in initializing schema '"+getRepresentation(false)+"': "+lineCount+".");
		if(lineCount>0){ // at least a single line with the subschema names
			String line,subSchemaName,subSchemaLineWhitespace;
			Vector<String> subSchemaLines;
			MongooseSchema subSchema;
			int slw;
			for(int lineIndex=0;lineIndex<lineCount;lineIndex++){
				line=textLines[lineIndex].trim(); // trimming because at the top level all lines should not start with tabs (as subschema lines do)
				Utils.consoleprintln("Processing line #"+(lineIndex+1)+" ("+line+") of schema '"+getRepresentation(false)+"'.");
				if(line.length()==0||line.charAt(0)=='#'||line.startsWith("//"))continue; // skip comment lines
				if(line.charAt(0)=='$')setTag(line.substring(1));else // any line starting with a dollar sign is considered the tag!!
				if(line.charAt(0)!='-'&&line.charAt(0)!='+'){ // a subschema (name) line
					subSchemaName=line;
					Utils.consoleprintln("Subschema of schema '"+getRepresentation(false)+"': '"+subSchemaName+"'.");

					subSchema=new MongooseSchema(subSchemaName,this,null); // create the subschema with the private name
					if(!lastSubSchemasTextLines.add(line))Utils.consoleprintln("ERROR: Failed to register subschema name definition line '"+line+"'.");
					// get all following lines that are indented, let's allow indenting with any character regulated by the first character on the next line
					if(lineIndex<lineCount-1){ // there is a next line
						line=textLines[lineIndex+1];
						lastSubSchemasTextLines.add(line);
						// technically all heading whitespace should be considered subschema lines
						subSchemaLineWhitespace=Utils.getHeadingWhitespace(line); // this is what we want a subschema line to start with!!!
						slw=subSchemaLineWhitespace.length();
						Utils.consoleprintln("Sub schema indentation length: "+slw+".");
						if(slw>0){ // there are sub schema lines
							Utils.consoleprintln("First subschema line: '"+line+"'.");
							subSchemaLines=new Vector<String>();
							while(true){
								lineIndex++;
								if(!subSchemaLines.add(line.substring(slw)))throw new Exception("Failed to register a text line defining sub schema "+subSchema.getRepresentation(false)+".");
								if(lineIndex>=lineCount)break; // no further lines
								line=textLines[lineIndex+1];
								if(!line.startsWith(subSchemaLineWhitespace))break; // NOT a subschema line
								if(!lastSubSchemasTextLines.add(line))throw new Exception("Failed to register a text line defining sub schema "+subSchema.getRepresentation(false));
							}
							for(String subSchemaLine:subSchemaLines)Utils.consoleprintln("\tLine: '"+subSchemaLine+"'.");
							// there will be at least one line in subSchemaLines (if we get here!!)
							// MDH@28OCT2017: setTextLines() has now been removed because setTextLines() would remember the text lines
							//                therefore a MongooseSchema does not need to be a consumer anymore... and setTextLines() not implemented!!!
							subSchema.parseTextLines((String[])subSchemaLines.toArray(new String[subSchemaLines.size()]));
						}
					}
					/////// already done by the constructor??? if(!subSchemas.add(subSchema))throw new Exception("Failed to register subschema '"+subSchemaName+"' with schema '"+getRepresentation(false)+"'.");
				}else{ // a field definition line
					if(!lastFieldsTextLines.add(line))throw new Exception("Failed to remember the field defined in line '"+line+"'.");
					Field field=getFieldFromContents(line);
					if(field==null)throw new Exception("Failed to initialize a field from text '"+line+"'.");
					if(!fieldCollection.add(field))throw new Exception("Failed to register the field defined in line '"+line+"' in schema '"+name+"'.");
				}
			}
		}
	}
	////////public boolean doneEditing(){return true;}
	// MDH@15OCT2018: load() returns True when something was actually loaded in which case _id does not need to be added!!!
	// MDH@17OCT2018: there's no need to actually load() from the file (through the ITextLinesProducer.File instance)
	// MDH@18OCT2018: this is going to be a bit more complicated, as we also include the subschema definitions in the same text lines
	//                subschema's should be defined BEFORE being used actually in the fields definitions
	//                although technically they can be combined
	//                we can distinguish them because fields start with + or - so anything that doesn't rules out
	// NOTE not to call setSynced() on all subschema, but instead call assumeSynced() as that method will also assume sync all of its subschema's like we want to as well!!!
	void assumeSynced(){
		try{for(MongooseSchema subSchema:subSchemas)subSchema.assumeSynced();}finally{setSynced(true);}
	}

	// possibly part of a collection associated with a directory...
	private MongooseSchemaCollection collection=null;
	public void setCollection(MongooseSchemaCollection mongooseSchemaCollection){collection=mongooseSchemaCollection;}

	public void showInExternalEditor(){
		try{java.awt.Desktop.getDesktop().edit(associatedFile);}catch(IOException ex){Utils.setInfo(this,"'"+ex.getLocalizedMessage()+"' in showing the Mongoose schema file.");}
	}
	public boolean isAssociatedFileReadable(){
		createMongooseSchemaTextFileProcessor();
		return(associatedFile!=null&&associatedFile.exists()&&!associatedFile.isDirectory()&&associatedFile.canRead());
	}

	// TODO we can keep the following stuff private as long as subclasses call setAssociatedFile/getAssociatedFile() to return the text file associated with them
	private ITextLinesProcessor mongooseSchemaTextFileProcessor; // MDH@21OCT2018: does not need to be declared as a TextFile per se here (so subclasses can have other ones)
	private File associatedFile=null;
	private void createMongooseSchemaTextFileProcessor(){
		File associatedFolder=(collection!=null?collection.getAssociatedFolder():null);
		associatedFile=new File(associatedFolder,getAssociatedFilename());
		// if the associated file does not exist and is not writable we definitely know we will not be able to save the schema!!
		if(associatedFile.exists()&&!associatedFile.canWrite())
			Utils.setInfo(this,"WARNING: Will not be able to save schema '"+name+"': the associated file '"+associatedFile.getAbsolutePath()+"' is not writable!");
		mongooseSchemaTextFileProcessor=new ITextLinesProcessor.TextFile(associatedFile);
	}
	private ITextLinesProducer getTextLinesProducer(){
		// if the file exists, it should be readabl
		if(mongooseSchemaTextFileProcessor==null)createMongooseSchemaTextFileProcessor();return mongooseSchemaTextFileProcessor;
	}
	private ITextLinesConsumer getTextLinesConsumer(){
		if(mongooseSchemaTextFileProcessor==null)createMongooseSchemaTextFileProcessor();return mongooseSchemaTextFileProcessor;
	}
	// MDH@25OCT2018: keep track of the last saved text lines
	private String[] lastSavedTextLines=null;
	public String[] getLastSavedTextLines(){return lastSavedTextLines;}
	// MDH@21OCT2018: we can leave load() and save() the same as what they were, but override getTextLinesProducer() and getTextLinesConsumer() in subclasses
	// MDH@27OCT2018: do we want getTextLines() to raise an Exception without knowing the partial loaded list????
	//                can we do both with a single call? NO how about produceTextLines which might return an Exception if something goes wrong
	//                loadException() or parseException() indicate that something might've gone wrong reading and parsing the design
	private Exception loadException=null,parseException=null,saveException=null;
	private boolean isWriteable(){try{return associatedFile.canWrite();}catch(Exception ex){}return false;}
	public boolean isSaveable(){
		// this is the best I can do
		// - should NOT be a subschema
		// - should have an associated file that exists (will be created by load if need be)
		// - and when it exists (as it should) it should be writable!!
		return(this.parentSchema==null&&associatedFile!=null&&associatedFile.exists()&&loadException==null&&isWriteable());
	}
	private final boolean load(){
		// this is a retry of 'syncing' what's stored on disk and what we know to be the textLines internally...
		synced=false;
		textLines=null;
		loadException=null;
		parseException=null;
		getTextLinesProducer();
		File newFile=null; // any new file we need to write what was obtained...
		try{
			if(associatedFile.exists())
				mongooseSchemaTextFileProcessor.produceTextLines();
			else if(associatedFile.createNewFile())
				textLines=new String[]{};
			else
				loadException=new Exception("Unable to create the associated file.");
		}catch(Exception ex){
			loadException=ex;
			Utils.setInfo(this,"ERROR: '"+loadException.getLocalizedMessage()+"' in load schema '"+name+"'.");
		}
		// if we have an associated file AND it does exist, we may be able to make it saveable by renaming it, and that way make it saveable!!!
		if(!isSaveable()&&associatedFile.exists()){
			String associatedFilename=getAssociatedFilename();
			// we can try to get rid of this exception if we rename the original file (as it is), so it's contents can be retained!!!
			// solution temporary is to rename the input file, write what we read to the file with the same name
			String newFilenamePrefix=associatedFilename.substring(0,associatedFilename.lastIndexOf("."));
			// the problem is finding a unique input name fast enough, how about using Fibonacci I suppose we can use any random integer BUT we want a larger random integer
			// how about keeping track of the largest number assigned so far??? NO, let's leave it to the user to get rid of the copies
			int renameFileIndex=0;
			while(true){
				renameFileIndex+=1;
				newFile=new File(newFilenamePrefix+"."+renameFileIndex+".msd");
				if(!newFile.exists()) break;
			}
			if(!associatedFile.renameTo(newFile)){ // rename failed
				newFile=null; // can't remove loadException, therefore the schema will NOT be saveable
				Utils.setInfo(this,"ERROR: Failed to move the original contents of the file of schema '"+name+"' to '"+newFile.getAbsolutePath()+"'.");
			}else{
				loadException=null;
				Utils.setInfo(this,"WARNING: As a precaution the original contents of the file of schema '"+name+"' was moved to '"+newFile.getAbsolutePath()+"'.");
			}
		}
		try{
			// we need to remember the text lines as parseTextLines() doesn't!!!!
			textLines=mongooseSchemaTextFileProcessor.getProducedTextLines();
			parseTextLines(textLines);
		}catch(Exception ex){
			parseException=ex;
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' initializing Mongoose schema design '"+name+"'.");
		}
		// if newFile is defined, the original file was renamed, and we should write the lines we did read, if we succeed loadException will be removed to make the file moveable...
		if(newFile!=null){ // which means loadException was set to
			try{
				mongooseSchemaTextFileProcessor.setTextLines(textLines);
				loadException=null;
			}catch(Exception ex){
			}
		}
		if(loadException==null&&parseException==null){
			assumeSynced();
			lastSavedSubSchemasTextLines=lastSubSchemasTextLines;
			lastSavedFieldsTextLines=lastFieldsTextLines;
			return true;
		}
		lastSavedSubSchemasTextLines=null;
		lastSavedFieldsTextLines=null;
		return false;
	}
	public final void write(ITextLinesConsumer textLinesConsumer)throws Exception{
		produceTextLines();
		textLinesConsumer.setTextLines(getProducedTextLines()); // if no text lines consumer defined, use the default one
	}
	public final boolean save(){
		// this is now relatively easy
		// CAREFUL NOW, if this is a subschema, we're going to return what the parent says, which is a very convenient method to also get the root schema saved (and all its subschema's as well!)
		// NOTE that this way only root schema's will try to load themselves from disk
		if(parentSchema!=null)return parentSchema.save();
		saveException=null;
		synced=false; // this is a sync attempt
		try{
			write(getTextLinesConsumer()); // if no text lines consumer defined, use the default one
		}catch(Exception ex){
			saveException=ex;
			// assume failure in which case we should assume undeterminable different!!
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' saving Mongoose schema design '"+name+"'.");
		}
		if(saveException==null){
			assumeSynced();
			lastSavedSubSchemasTextLines=lastSubSchemasTextLines;
			lastSavedFieldsTextLines=lastFieldsTextLines;
			return true;
		}
		lastSavedSubSchemasTextLines=null;
		lastSavedFieldsTextLines=null;
		return false;
	}
	// convenience methods
	public int getIndexOfField(Field field){return(field!=null?fieldCollection.indexOf(field):-1);}
	public boolean addField(Field field){return(field!=null&&fieldCollection.add(field));} // NOTE fieldCollection will take care of registering itself as the collection of field!!!
}