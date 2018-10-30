package com.officevitae.marc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MongooseSchemaFactory {

	private static Map<String,MongooseSchema> mongooseSchemaMap=new HashMap<String,MongooseSchema>();
	public static MongooseSchema getANewMongooseSchema(String schemaName,MongooseSchemaCollection collection){
		if(mongooseSchemaMap.containsKey(schemaName)){
			if(mongooseSchemaMap.get(schemaName) instanceof JavaScriptMongooseSchema)return null;
		}else
			mongooseSchemaMap.put(schemaName,new MongooseSchema(schemaName,collection));
		return mongooseSchemaMap.get(schemaName);
	}
	// MDH@23OCT2018: special consideration for returning JavaScriptMongooseSchema's
	//                NOTE could've created a separate JavaScriptMongooseSchemaFactory BUT we do NOT want schema's from both types with the same name!!!!
	public static JavaScriptMongooseSchema getANewJavaScriptMongooseSchema(String schemaName,MongooseSchemaCollection collection){
		if(mongooseSchemaMap.containsKey(schemaName)){
			if(!(mongooseSchemaMap.get(schemaName) instanceof JavaScriptMongooseSchema)) return null;
		}else
			mongooseSchemaMap.put(schemaName,new JavaScriptMongooseSchema(schemaName,null,collection));
		return (JavaScriptMongooseSchema)mongooseSchemaMap.get(schemaName);
	}

	public static MongooseSchema getMongooseSchemaWithName(String schemaName){
		return(mongooseSchemaMap.containsKey(schemaName)?mongooseSchemaMap.get(schemaName):null);
	}

	public static boolean isExistingMongooseSchemaName(String schemaName){return mongooseSchemaMap.containsKey(schemaName);}

	public static List<String> getNamesOfSchemasWithIdOfType(IFieldType fieldType,String requestingTableName){
		Vector<String> mongooseSchemaNames=new Vector<String>();
		String mongooseSchemaName;
		MongooseSchema mongooseSchema;
		for(Map.Entry<String,MongooseSchema> mongooseSchemaEntry:mongooseSchemaMap.entrySet()){
			mongooseSchemaName=mongooseSchemaEntry.getKey();
			if(mongooseSchemaName.equalsIgnoreCase(requestingTableName))continue;
			mongooseSchema=mongooseSchemaEntry.getValue();
			if(mongooseSchema.getTypeOfIdField().equals(fieldType))if(!mongooseSchemaNames.add(mongooseSchemaName))break;
		}
		return mongooseSchemaNames;
	}
}
