package com.officevitae.marc;

public class MapFieldType implements ICompositeFieldType{

	private IFieldType ofFieldType=MongooseFieldType.MIXED;

	public Description getDescription(){
		return new Description(){
			public IFieldType getFieldType(){return MapFieldType.this;}
			public String toString(){return MongooseFieldType.MAP.toString();} // make it look the same as Map!!!
		};
	}

	public String toString(){return "("+(ofFieldType.equals(MongooseFieldType.MIXED)?"":ofFieldType.getDescription().toString())+")";}

	public boolean representsAValidValue(String valueText){
		return true;
	}

	public void setSubFieldType(IFieldType subFieldType){
		/*
		if(arrayElementType==null)throw new NullPointerException("Array element type undefined!");
		if(this.arrayElementType.equals(arrayElementType))throw new IllegalArgumentException("Array element type unchanged!");
		*/
		ofFieldType=subFieldType;
		Utils.consoleprintln("Map element field type of '"+toString()+"' set to '"+ofFieldType.toString()+".");
	}
	public IFieldType getSubFieldType(){return ofFieldType;}
}
