package com.officevitae.marc;

import java.io.File;
import java.util.Vector;

public class MongooseSchemaCollection extends Vector<MongooseSchema>{
	// exposes it's associated directory to be used by the contained Mongoose schema's to know where to save into
	private String name;
	public String toString(){return name;}
	public MongooseSchemaCollection(String name){this.name=name;}

	///////public String getAssociatedFoldername(){return new File("./schemas",name).getAbsolutePath();}

	public File getAssociatedFolder(){return new File("schemas",name);}

	public boolean add(MongooseSchema mongooseSchema){
		boolean result=super.add(mongooseSchema);
		///////if(result)mongooseSchema.setCollection(this);
		return result;
	}
	public boolean containsASchemaCalled(String schemaName){
		for(MongooseSchema mongooseSchema:this)if(mongooseSchema.getName().equals(schemaName))return true;
		return false;
	}
}
