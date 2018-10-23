package com.officevitae.marc;

import javax.swing.*;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

// MDH@16OCT2018: any Mongoose Schema can also be used as field type (i.e. when it is a subschema!!!)
public class MongooseSchema implements IFieldChangeListener,IFieldType, ITextLinesContainer{

	private static final int DEFAULT_TYPE_INDEX=7;
	private static final java.util.List<String> FIELD_FLAG_NAMES=Arrays.asList(new String[]{"required","unique","index","sparse","lowercase","uppercase","trim"});

	// MDH@18OCT2018: keep the fields in a separate FieldCollection class
	public class FieldCollection extends Vector<Field> implements ITextLinesContainer{
		public boolean containsFieldWithName(String fieldName){
			for(Field field:this)if(field.getName().equalsIgnoreCase(fieldName))return true;
			return false;
		}
		public MongooseSchema getSchema(){return MongooseSchema.this;}
		@Override
		public boolean add(Field field){
			// we don't want null fields or fields with the same name as one of mine
			if(field==null||this.containsFieldWithName(field.getName()))return false;
			if(!super.add(field))return false;
			field.setCollection(this);
			return true; // force registering the schema that contains the field!!!
		}
		public void setTextLines(String[] textLines){
			super.clear();
			for(String textLine:textLines)add(getFieldFromContents(textLine));
		}
		public String[] getTextLines(){
			String[] textLines=new String[this.size()];
			int fieldIndex=0;for(Field field:this)textLines[fieldIndex++]=getFieldContents(field);
			return textLines;
		}
		public boolean doneEditing(){return true;}
	}
	public FieldCollection fieldCollection=new FieldCollection();
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
	boolean addSubSchema(MongooseSchema subSchema){
		if(subSchema==null)return false;
		if(subSchemas.contains(subSchema))return true;
		if(!subSchemas.add(subSchema))return false;
		for(SubSchemaListener subSchemaListener:subSchemaListeners)try{subSchemaListener.subSchemaAdded(subSchema);}catch(Exception ex){}
		return true;
	}
	boolean removeSubSchema(MongooseSchema subSchema){
		if(subSchema==null)return false;
		if(!subSchemas.contains(subSchema))return true;
		if(!subSchemas.remove(subSchema))return false;
		for(SubSchemaListener subSchemaListener:subSchemaListeners)try{subSchemaListener.subSchemaRemoved(subSchema);}catch(Exception ex){}
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

	// as we require that associatedFile is initialized in the constructor, subclasses cannot call my constructor, but may call initialize and checkFieldCollection()
	// alternatively passing the associatedFile into the constructor is perhaps also a good idea
	protected String getAssociatedFilename(){
		return "."+File.pathSeparator+name+".msd";
	}

	// where to load a subschema from depends on the parent!!
	protected MongooseSchema(){}
	public MongooseSchema(String name,MongooseSchema parentSchema){
		this.name=name;
		if(parentSchema!=null)
			setParent(parentSchema);
		else
			load();
		// MDH@15OCT2018: the automatic _id field can only be disabled on a parentless schema
		if(!fieldCollection.containsFieldWithName("_id")&&!fieldCollection.add(new Field("_id").setType(MongooseFieldType.OBJECTID).setDisabable(this.parentSchema!=null)))
			Utils.setInfo(this,"ERROR: Failed to add automatic _id field to schema '"+getRepresentation(false)+"'.");
	}
	public MongooseSchema(String name){this(name,null);} // a main schema (not a subschema!!)

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
		if(field.isChanged())
			setSynced(false);
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

	public ITextLinesContainer getModelTextLinesContainer(){
		if(modelTextFile==null){
			modelTextFile=new ITextLinesProcessor.TextFile(new java.io.File("app/models",name+"model.js"));
		}
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
		if(parentSchema!=null)representation=parentSchema.getRepresentation(false)+"$"+representation;
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
			File f=new File(routesDirectory,name+".routes.js");
			// for now let's decide to always overwrite the thing
			if(f.exists()){
				Utils.setInfo(this,"The routes file of table "+name+" already exists, and will not be overwritten.");
				return;
			}
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' in writing the routes of table "+name+".");
		}
	}

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
			File f=new File(controllersDirectory,name+".controller.js");
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

	private boolean containsFieldWithName(String fieldName){return fieldCollection.containsFieldWithName(fieldName);}
	public Field getFieldCalled(String fieldName){
		for(Field field:fieldCollection)if(field.getName().equalsIgnoreCase(fieldName))return field;
		return null;
	}
	// MDH@16OCT2018: writing the schemas means writing the subschemas first, followed by writing the main schema
	//                I suppose it's the schema's that can be overwritten, so should be marked somehow????
	//                the problem is that additional code can be placed between the schema definitions, so I guess we can put the schema names in the annotation: /*MSD:<schemaname>*/

	void writeSchemas(PrintWriter pw){
		// MDH@15OCT2018: should we write the subschema's now????
		for(MongooseSchema subSchema:subSchemas)subSchema.writeSchemas(pw); // pretty straightforward!!
		// if we want to be able to re-create the Mongoose Schema from the model we need to mark the lines written somehow
		// naming is a bit of an issue with subschemas with the same name as the main schema, I guess we cannot use a period in the schema name, instead we use underscores...
		pw.println("const "+getDescription().toString().toLowerCase()+"=mongoose.Schema({"); // the description appends the word 'Schema' itself!!
		// write one field per line following
		Field autoIncrementedField=null;
		for(Field field:fieldCollection){
			// MDH@16OCT2018: do NOT publish disabled fields!
			if(!field.isEnabled())continue;
			// the most simple form is just the name of the field, a colon and the type
			// but for now let's not do that
			// using the description to represent the type (so we'd get a$aSchema instead of a$a)
			pw.print("\t\t\t\t"+field.getName().toLowerCase()+":{type:"+field.getType().getDescription().toString()); // writing the name (forced to lowercase) and the type
			if(!field.isAutoIncremented()){
				// if a ref property is defined write that before anything else
				if(!field.refLiteral.isDisabled()&&field.refLiteral.isValid()) pw.print(",ref:"+field.refLiteral.getValue());
				// I suppose the index is also quite important
				if(field.isUnique()) pw.print(",unique:true");
				else if(field.isIndex()) pw.print(",index:true");
				else if(field.isSparse()) pw.print(",sparse:true");
				// general option flags
				if(field.isRequired()) pw.print(",required:true");
				if(field.isSelect()) pw.print(",select:true");
				// general options
				if(field.aliasLiteral.isValid()&&!field.aliasLiteral.isDisabled()) pw.print(",alias:'"+field.aliasLiteral.getValue()+"'");
				if(!field.defaultLiteral.isDisabled()&&field.defaultLiteral.isValid()) pw.print(",default:"+field.defaultLiteral.getValue()); // assuming getValue will quote the text if it's a String default????
				// type-specific options
				IFieldType fieldType=field.getType();
				if(fieldType instanceof MongooseFieldType)
				switch(((MongooseFieldType)fieldType).ordinal()){
					case Field.DATE_FIELD:
						if(!field.minDateLiteral.isDisabled()&&field.minDateLiteral.isValid()) pw.print(",min:"+field.minDateLiteral.getValue());
						if(!field.maxDateLiteral.isDisabled()&&field.maxDateLiteral.isValid()) pw.print(",max:"+field.maxDateLiteral.getValue());
						break;
					case Field.NUMBER_FIELD:
						if(!field.minNumberLiteral.isDisabled()&&field.minNumberLiteral.isValid()) pw.print(",min:"+field.minNumberLiteral.getValue());
						if(!field.maxNumberLiteral.isDisabled()&&field.maxNumberLiteral.isValid()) pw.print(",max:"+field.maxNumberLiteral.getValue());
						break;
					case Field.STRING_FIELD:
						if(field.isLowercase()) pw.print(",lowercase:true");
						else if(field.isUppercase()) pw.print(",uppercase:true");
						if(field.isTrim()) pw.print(",trim:true");
						if(!field.minLengthLiteral.isDisabled()&&field.minLengthLiteral.isValid()) pw.print("minlength:"+field.minLengthLiteral.getValue());
						if(!field.maxLengthLiteral.isDisabled()&&field.maxLengthLiteral.isValid()) pw.print("maxlength:"+field.minLengthLiteral.getValue());
						if(!field.matchLiteral.isDisabled()&&field.matchLiteral.isValid()) pw.print(",match:new RegExp("+field.matchLiteral.getValue()+")"); // assuming the user did NOT enclose the regular expression between / and /
						if(!field.valuesLiteral.isDisabled()&&field.valuesLiteral.isValid()) pw.print("enum:['"+String.join("','",field.valuesLiteral.getValue())+"'']");
						break;
				}
			}else autoIncrementedField=field;
			pw.print("},"); // ready for the next field!!
			pw.println(" /*MSD:F*/"); // marks a MSD field line
		}
		pw.print("\t\t\t}"); // close the first argument to mongoose.Schema
		// only subschema's can NOT have _id fields in which case the user disabled it (which is not possible for main schema's)
		// TODO there might be other options that we'd like to be able to set
		if(this.getParent()!=null)if(!this.containsFieldWithName("_id")||!this.getFieldCalled("_id").isEnabled())pw.print(",{_id:false}");
		pw.println("); /*MSD:O*/"); // close the schema assignment (on the same line, so we save a little space!!! let's call this the O(ptions) line
		if(autoIncrementedField!=null){
			pw.println();
			// NO LONGER REQUIRED WITH mongoose-plugin-autoinc: pw.println("// DO NOT FORGET TO initialize THE PLUGIN IN YOUR app.js WITH mongoose.connection!");
			pw.println("const autoIncrement=require('mongoose-plugin-autoinc');"); // MDH@24SEP2018: switched to an updated version of mongoose-auto-increment (which is 3 years old and requires a version 4 of Mongoose)
			pw.println(name.toLowerCase()+"Schema.plugin(autoIncrement,{model:'"+Utils.capitalize(name)+"',field:'"+autoIncrementedField.getName()+"',startAt:"+autoIncrementedField.startAtLiteral.getValue()+",incrementBy:1});");
			// being able to reset the counter would be nice, for which we need the connection and apparently mongoose.connection holds the connection (see app.js)
			// TODO figure out when exactly the reset occurs????
			pw.println("/* TO RESET AUTO-INCREMENT FIELD "+autoIncrementedField.getName()+" SAVE AN EMPTY INSTANCE, LIKE THIS:");
			pw.println("const "+Utils.capitalize(name)+"=mongoose.model('"+Utils.capitalize(name)+"',"+Utils.capitalize(name)+"Schema);"); // mongoose.model will use the default mongoose connection i.e. mongoose.connection!!!
			pw.println(name+"=new "+Utils.capitalize(name)+"();"); // get an instance
			pw.println(name+".save(function(err){"+name+".nextCount(function(err,count){"+name+".resetCount(function(err,nextCount){});});});");
			pw.println("*/");
		}
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
	private ITextLinesProcessor.TextFile modelTextFile;
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
			File f=new File(modelsDirectory,name+".model.js");
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

		}
		// let's start with ascertaining that we have an app subfolder
		File appDirectory=new File(".","app");
		boolean appDirectoryExists=Utils.directoryShouldExist(appDirectory);
		if(!appDirectoryExists){
			Utils.setInfo(this,"The 'app' subdirectory does not exist, or is not a directory.");
			return;
		}
		writeModel(appDirectory);
		writeRoutes(appDirectory);
		writeController(appDirectory);
	}

	private String getFieldContents(Field field){
		return (field==null?"":field.getTextRepresentation(true));
	}

	private IFieldType getFieldType(String fieldTypeName){
		if(fieldTypeName.isEmpty())return MongooseFieldType.MIXED; // e.g. as generic array element type!!!
		if(fieldTypeName.equalsIgnoreCase("array")) return MongooseFieldType.ARRAY;
		//////////if(fieldTypeName.equalsIgnoreCase("auto incremented integer"))return FieldType.AUTO_INCREMENT;
		if(fieldTypeName.equalsIgnoreCase("boolean")) return MongooseFieldType.BOOLEAN;
		if(fieldTypeName.equalsIgnoreCase("buffer")) return MongooseFieldType.BUFFER;
		if(fieldTypeName.equalsIgnoreCase("date")) return MongooseFieldType.DATE;
		if(fieldTypeName.equalsIgnoreCase("decimal128")) return MongooseFieldType.DECIMAL128;
		if(fieldTypeName.equalsIgnoreCase("map")) return MongooseFieldType.MAP;
		if(fieldTypeName.equalsIgnoreCase("mixed")) return MongooseFieldType.MIXED;
		if(fieldTypeName.equalsIgnoreCase("number")) return MongooseFieldType.NUMBER;
		if(fieldTypeName.equalsIgnoreCase("objectid")) return MongooseFieldType.OBJECTID;
		if(fieldTypeName.equalsIgnoreCase("string")) return MongooseFieldType.STRING;
		// TODO could be the name of a sub schema????
		if(!subSchemas.isEmpty())for(MongooseSchema subSchema:subSchemas)if(subSchema.getDescription().equals(fieldTypeName))return subSchema;
		return null;
	}

	private Field getFieldFromContents(String fieldcontents){
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
				if(fieldTypeName.length()>1&&fieldTypeName.charAt(0)=='['&&fieldTypeName.endsWith("]")){
					field.setType(MongooseFieldType.ARRAY);
					field.setArrayElementType(getFieldType(fieldTypeName.substring(1,fieldTypeName.length()-1)));
				}else
					field.setType(getFieldType(fieldTypeName));
				field.setEnabled(nametypeParts[0].charAt(0)=='+');
				String fieldPropertyName, fieldPropertyValue;
				boolean first=true;
				for(String contentPart: contentParts){
					if(first==true){
						first=false;
						continue;
					} // skip first element (processed before)
					if(contentPart.trim().isEmpty()) continue;
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
						}else
							Utils.setInfo(this,"ERROR: Invalid field property name '"+fieldPropertyName+"' in field property assignment '"+contentPart+"'.");
					}else if(contentPart.charAt(0)=='-'){ // something that is a flag
						switch(FIELD_FLAG_NAMES.indexOf(contentPart.substring(1))){
							case 0:
								field.setRequired(true);
								break;
							case 1:
								field.setUnique(true);
								break;
							case 2:
								field.setIndex(true);
								break;
							case 3:
								field.setSparse(true);
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
								Utils.setInfo(this,"ERROR: Invalid field property flag '"+contentPart.substring(1)+".");
						}
					}
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
	// ITextLinesContainer implementation
	public String[] getTextLines() throws Exception{
		if(parentSchema==null)if(isSynced())return null; // nothing changed, so return null (MDH@19OCT2018: but only when a main schema, subschema's shouldn't be that fresh!!)
		Vector<String> textLines=new Vector<String>();
		// first to collect all the subschema text lines
		String[] subSchemaTextLines;
		String subSchemaName;
		for(MongooseSchema subSchema:subSchemas){
			subSchemaName=subSchema.getName();
			if(!textLines.add(subSchemaName))throw new Exception("Failed to add the name of subschema '"+subSchema.getName()+"'.");
			subSchemaTextLines=subSchema.getTextLines();
			// we have to indent all the returned text lines
			for(String subSchemaTextLine:subSchemaTextLines)if(!textLines.add("\t"+subSchemaTextLine))throw new Exception("Failed to add line '"+subSchemaTextLine+"' of subschema '"+subSchemaName+"'.");
		}
		// now ready to append the fields (perhaps referring to the subschemas)
		for(Field field:fieldCollection)if(!textLines.add(getFieldContents(field)))throw new Exception("Failed to append field '"+field.getName()+"'.");
		// if nothing changed, since we started on editing this thing, return null, otherwise get the current representation and split it!!
		return(textLines.isEmpty()?new String[]{}:textLines.toArray(new String[textLines.size()]));
	}
	////////public boolean doneEditing(){return true;}
	// MDH@15OCT2018: load() returns True when something was actually loaded in which case _id does not need to be added!!!
	// MDH@17OCT2018: there's no need to actually load() from the file (through the ITextLinesProducer.File instance)
	// MDH@18OCT2018: this is going to be a bit more complicated, as we also include the subschema definitions in the same text lines
	//                subschema's should be defined BEFORE being used actually in the fields definitions
	//                although technically they can be combined
	//                we can distinguish them because fields start with + or - so anything that doesn't rules out
	public void setTextLines(String[] lines) throws Exception{
		// we do not want to 'load' more than once!!!
		if(lines==null)throw new NullPointerException("No text defined to initialize Mongoose schema "+getRepresentation(false)+" from.");
		int lineCount=lines.length;
		if(lineCount>0){ // at least a single line with the subschema names
			String line,subSchemaName,subSchemaLineWhitespace;
			Vector<String> subSchemaLines;
			MongooseSchema subSchema;
			int slw;
			for(int lineIndex=0;lineIndex<lineCount;lineIndex++){
				line=lines[lineIndex].trim(); // trimming because at the top level all lines should not start with tabs (as subschema lines do)
				Utils.consoleprintln("Processing line #"+(lineIndex+1)+" ("+line+") of schema '"+getRepresentation(false)+"'.");
				if(line.length()==0||line.charAt(0)=='#'||line.startsWith("//"))continue; // skip comment lines
				if(line.charAt(0)!='-'&&line.charAt(0)!='+'){ // a subschema (name) line
					subSchemaName=line;
					Utils.consoleprintln("Subschema of schema '"+getRepresentation(false)+"': '"+subSchemaName+"'.");
					subSchema=new MongooseSchema(subSchemaName,this); // create the subschema with the private name
					// get all following lines that are indented, let's allow indenting with any character regulated by the first character on the next line
					if(lineIndex<lineCount-1){ // there is a next line
						line=lines[lineIndex+1];
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
								line=lines[lineIndex+1];
								if(!line.startsWith(subSchemaLineWhitespace))break; // NOT a subschema line
							}
							for(String subSchemaLine:subSchemaLines)Utils.consoleprintln("\tLine: '"+subSchemaLine+"'.");
							// there will be at least one line in subSchemaLines (if we get here!!)
							subSchema.setTextLines((String[])subSchemaLines.toArray(new String[subSchemaLines.size()]));
						}
					}
					/////// already done by the constructor??? if(!subSchemas.add(subSchema))throw new Exception("Failed to register subschema '"+subSchemaName+"' with schema '"+getRepresentation(false)+"'.");
				}else // a field definition line
					fieldCollection.add(getFieldFromContents(line));
			}
			/* replacing:
			String subSchemaLine=lines[0];
			if(!subSchemaLine.trim().isEmpty()){
				Utils.consoleprintln("Parsing subschemas from '"+subSchemaLine+"' of schema '"+name+"'.");
				for(String subSchemaName: subSchemaLine.split("\t")) new MongooseSchema(subSchemaName,this);
			}
			String fieldLine;
			for(int lineIndex=1;lineIndex<lineCount;lineIndex++){
				fieldLine=lines[lineIndex].trim();
				if(fieldLine.length()>0&&fieldLine.charAt(0)!='#') this.add(getFieldFromContents(fieldLine).setSchema(this));
			}
			*/
		}
			/* replacing:
			// if the file doesn't exist, can't become synced!!!
			boolean somethingloaded=false;
			File file=new File("./"+getRepresentation(false)+".msd"); // extension means short for Mongoose Schema Design
			if(file.exists()){
				try{
					// if the file does not exist we're definitely NOT synced
					BufferedReader br=new BufferedReader(new FileReader(file));
					// read the names of the subschema's so we can load them as well, which should be present on the first line
					String subSchemaLine=br.readLine();
					if(subSchemaLine!=null){ // at least one line!!
						somethingloaded=true; // i.e. we should have lines in lines!!!
						lines=new ITextProvider.Lines(); // I suppose we may assume we should collect
						if(!lines.addLine(subSchemaLine))lines=null; // if this fails subSchemaLine will be null and a NullPointerException will occur!!
						// now we managed to read a line we may assume
						// from the names of the subschema's we may create the MongooseSchema instances
						// however, we're calling load in the constructor before parent is set in which case the name of the files won't come out right, so we need to set the parent immediately
						// in the constructor
						// each line contains the definition of a single field
						String fieldline=br.readLine();
						while(fieldline!=null&&!fieldline.isEmpty()){
							if(lines!=null&&!lines.addLine(fieldline))lines=null; // failing to actually register the line read!!
							if(fieldline.length()>0&&fieldline.charAt(0)!='#')this.add(getFieldFromContents(fieldline).setSchema(this));
							fieldline=br.readLine();
						}
					}
					br.close();
					setSynced(true);
				}catch(Exception ex){
					Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' in reading table "+name+".");
				}
			}
		}
		// TODO how about status of lines????
		return somethingloaded;
		*/
	}

	// NOTE not to call setSynced() on all subschema, but instead call assumeSynced() as that method will also assume sync all of its subschema's like we want to as well!!!
	void assumeSynced(){try{for(MongooseSchema subSchema:subSchemas)subSchema.assumeSynced();}finally{setSynced(true);}}

	// TODO we can keep the following stuff private as long as subclasses call setAssociatedFile/getAssociatedFile() to return the text file associated with them
	private ITextLinesProcessor mongooseSchemaTextFileProcessor; // MDH@21OCT2018: does not need to be declared as a TextFile per se here (so subclasses can have other ones)
	private ITextLinesProducer getTextLinesProducer(){
		if(mongooseSchemaTextFileProcessor==null)mongooseSchemaTextFileProcessor=new ITextLinesProcessor.TextFile(new File(getAssociatedFilename()));
		return mongooseSchemaTextFileProcessor;
	}
	private ITextLinesConsumer getTextLinesConsumer(){
		if(mongooseSchemaTextFileProcessor==null)mongooseSchemaTextFileProcessor=new ITextLinesProcessor.TextFile(new File(getAssociatedFilename()));
		return mongooseSchemaTextFileProcessor;
	}
	// MDH@21OCT2018: we can leave load() and save() the same as what they were, but override getTextLinesProducer() and getTextLinesConsumer() in subclasses
	public final boolean load(){
		// this is now relatively easy
		try{
			setTextLines(getTextLinesProducer().getTextLines());
			assumeSynced();
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' loading the Mongoose schema design '"+name+"'.");
		}
		return isSynced();
	}
	public final boolean save(){
		// this is now relatively easy
		// CAREFUL NOW, if this is a subschema, we're going to return what the parent says, which is a very convenient method to also get the root schema saved (and all its subschema's as well!)
		// NOTE that this way only root schema's will try to load themselves from disk
		if(parentSchema!=null)return parentSchema.save();
		try{
			getTextLinesConsumer().setTextLines(getTextLines());
			assumeSynced();
		}catch(Exception ex){
			Utils.setInfo(this,"ERROR: '"+ex.getLocalizedMessage()+"' saving the Mongoose schema design '"+name+"'.");
		}
		return isSynced();
	}
	// convenience methods
	public int getIndexOfField(Field field){return(field!=null?fieldCollection.indexOf(field):-1);}
	public boolean addField(Field field){return(field!=null&&fieldCollection.add(field));} // NOTE fieldCollection will take care of registering itself as the collection of field!!!
}