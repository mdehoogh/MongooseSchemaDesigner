package com.officevitae.marc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MongooseSchemaFactory {

	private static Map<String,MongooseSchema> mongooseSchemaMap=new HashMap<String,MongooseSchema>();
	public static MongooseSchema getANewMongooseSchema(String schemaName){
		if(!mongooseSchemaMap.containsKey(schemaName))mongooseSchemaMap.put(schemaName,new MongooseSchema(schemaName));
		return mongooseSchemaMap.get(schemaName);
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
