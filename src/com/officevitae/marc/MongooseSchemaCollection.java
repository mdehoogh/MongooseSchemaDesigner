package com.officevitae.marc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Vector;

public class MongooseSchemaCollection extends Vector<MongooseSchema> implements OptionCollection.OptionChangeListener{

	// exposes it's associated directory to be used by the contained Mongoose schema's to know where to save into
	private String name;

	private OptionCollection optionCollection=null;

	// MDH@08NOV2018: read option collection JIT
	//                ok, we force creating an option collection JIT to allow external editing (otherwise there would be nothing to show!!)
	//                if we didn't DO that AND readOptionCollection() failed with file 'options' not present, there would be no collection to edit!!!
	public OptionCollection getOptionCollection(){return optionCollection;}

	private boolean syncable=true; // a flag that will indicate whether or not we can save the options at all...
	public boolean isSyncable(){return syncable;}
	private boolean synced=true;
	public boolean isSynced(){return synced;}

	public interface SyncListener{
		void syncChanged(MongooseSchemaCollection mongooseSchemaCollection);
	}
	private SyncListener syncListener;
	public void setSyncListener(SyncListener syncListener){this.syncListener=syncListener;}
	public void informSyncListener(){
		if(this.syncListener!=null)try{syncListener.syncChanged(this);}catch(Exception ex){}
	}
	public void setSynced(boolean synced){this.synced=synced;informSyncListener();}
	// readOptionCollection() returns the read option collection (or null if nothing was read, or some error occurred)
	private void readOptionCollection(){
		// the basic problem here is that a basic OptionCollection is synced because all its values are at defaults
		// unless there's a file to read its contents from
		try{
			this.optionCollection=null;
			File optionsFile=new File(getAssociatedFolder(),"options");
			// if the file does NOT exist, we're synced, otherwise we're unsynced until the file was read successfully
			// BUT the problem is that we cannot overwrite the file if we fail to read the options from it!!!
			if(optionsFile.exists()){
				syncable=false; // not syncable until the file was read
				synced=false; // we're unsynced until everything was read successfully...
				if(optionsFile.canRead()){
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
					// managed to process the file (i.e. read all the lines), which means we're both synced and syncable!!!
					synced=true;
					syncable=true;
					Utils.setInfo(null,"Option collection of Mongoose schema collection "+name+" read.");
				}
			}
		}catch(Exception ex){
			Utils.setInfo(null,"'"+ex.getLocalizedMessage()+"' reading the options of Mongoose schema collection "+name+".");
		}finally{
			informSyncListener();
		}
	}
	public boolean saveOptionCollection(){
		// if not syncable don't even try!!
		if(syncable){
			if(!synced){
				try{
					PrintWriter pw=new PrintWriter(new File(getAssociatedFolder(),"options"));
					// if the option's value is the default we prepend the option line with #, so the file is easy to maintain!!
					for(Option option:optionCollection) pw.println((option.isDefault()?"#":"")+option.toString());
					pw.close();
					setSynced(true);
				}catch(Exception ex){
					Utils.setInfo(null,"'"+ex.getLocalizedMessage()+"' writing the options of Mongoose schema collection "+name+".");
				}
			}else
				Utils.setInfo(null,"No need to write the options of Mongoose schema collection "+name+": it has not changed!");
		}else
			Utils.setInfo(null,"Unable to write the options of Mongoose schema collection "+name+": failed to read them initially.");
		return isSynced();
	}

	// OptionCollection.OptionChangeListener implementation
	public void optionChanged(int optionIndex){
		setSynced(false);
		saveOptionCollection(); // go ahead try to save the option collection immediately...
	}
	// end OptionCollection.OptionChangeListener implementation
	public String toString(){return name;}

	// a single attempt to read the associated option collection????
	public MongooseSchemaCollection(String name,SyncListener syncListener){
		this.name=name;
		optionCollection=new OptionCollection(this);
		this.syncListener=syncListener;
		readOptionCollection();
	}

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

}
