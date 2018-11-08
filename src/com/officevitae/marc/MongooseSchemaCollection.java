package com.officevitae.marc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Vector;

public class MongooseSchemaCollection extends Vector<MongooseSchema>{

	// exposes it's associated directory to be used by the contained Mongoose schema's to know where to save into
	private String name;

	// readOptionCollection() returns the read option collection (or null if nothing was read, or some error occurred)
	private void readOptionCollection(){
		try{
			this.optionCollection=null;
			File optionsFile=new File(getAssociatedFolder(),"options");
			if(optionsFile.exists()&&optionsFile.canRead()){
				OptionCollection optionCollection=new OptionCollection(this);
				BufferedReader br=new BufferedReader(new FileReader(optionsFile));
				String optionLine=br.readLine();
				while(optionLine!=null){
					try{
						if(!optionLine.trim().startsWith("#"))
							optionCollection.parseOptionValue(optionLine.trim().split(":"));
					}finally{
						optionLine=br.readLine();
					}
				}
				this.optionCollection=optionCollection;
				br.close();
				Utils.setInfo(null,"Option collection of Mongoose schema collection "+name+" read.");
			}
		}catch(Exception ex){
			Utils.setInfo(null,"'"+ex.getLocalizedMessage()+"' reading the options of Mongoose schema collection "+name+".");
		}
	}
	private boolean writeOptionCollection(OptionCollection optionCollection){
		try{
			PrintWriter pw=new PrintWriter(new File(getAssociatedFolder(),"options"));
			// if the option's value is the default we prepend the option line with #, so the file is easy to maintain!!
			for(Option option:optionCollection)pw.println((option.isDefault()?"#":"")+option.toString());
			pw.close();
			return true;
		}catch(Exception ex){
			Utils.setInfo(null,"'"+ex.getLocalizedMessage()+"' writing the options of Mongoose schema collection "+name+".");
		}
		return false;
	}

	public String toString(){return name;}

	// a single attempt to read the associated option collection????
	public MongooseSchemaCollection(String name){this.name=name;readOptionCollection();}

	///////public String getAssociatedFoldername(){return new File("./schemas",name).getAbsolutePath();}

	public File getAssociatedFolder(){return new File("schemas",name);}

	public boolean add(MongooseSchema mongooseSchema){
		boolean result=super.add(mongooseSchema);
		// if we succeeded AND we have an option collection (with defaults to use), initialize the Mongoose schema with our option collection!!
		//////////if(result)if(optionCollection!=null)mongooseSchema.updateOptions(optionCollection);
		return result;
	}
	public boolean containsASchemaCalled(String schemaName){
		for(MongooseSchema mongooseSchema:this)if(mongooseSchema.getName().equals(schemaName))return true;
		return false;
	}
	public MongooseSchema getSchemaWithName(String schemaName){
		for(MongooseSchema mongooseSchema:this)if(mongooseSchema.getName().equals(schemaName))return mongooseSchema;
		return null;
	}
	public String[] unsaved(){
		Vector<String> unsavedMongooseSchemaNames=new Vector<String>(this.size());
		for(MongooseSchema mongooseSchema:this)if(!mongooseSchema.save())unsavedMongooseSchemaNames.add(mongooseSchema.getName());
		return(unsavedMongooseSchemaNames.isEmpty()?new String[]{}:(String[])unsavedMongooseSchemaNames.toArray(new String[unsavedMongooseSchemaNames.size()]));
	}

	private OptionCollection optionCollection=null;
	// when the option collection is set, we should save it with the collection and start using it, which might be a problem though because in that case it is not used by other Mongoose schemas yet!!!
	// ok, a MongooseSchema can
	public boolean saveOptionCollection(){return(optionCollection!=null&&writeOptionCollection(optionCollection));}

	// MDH@08NOV2018: read option collection JIT
	//                ok, we force creating an option collection JIT to allow external editing (otherwise there would be nothing to show!!)
	//                if we didn't DO that AND readOptionCollection() failed with file 'options' not present, there would be no collection to edit!!!
	public OptionCollection getOptionCollection(){if(optionCollection==null)optionCollection=new OptionCollection(this);return optionCollection;}

}
