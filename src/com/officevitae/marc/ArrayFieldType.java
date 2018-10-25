package com.officevitae.marc;

public class ArrayFieldType implements ICompositeFieldType{

	// IFieldType implementation
	public Description getDescription(){
		return new Description(){
			public IFieldType getFieldType(){return ArrayFieldType.this;} // NO, I do not want to return MongooseFieldType.ARRAY here but myself actually
			// if the element type is undefined i.e. anything goes we return the word Array, otherwise the type of the element enclosed in square brackets...
			public String toString(){return MongooseFieldType.ARRAY.toString();} // want to make it look the same as ARRAY!!!
		};
	}

	public boolean representsAValidValue(String value){
		return true;
	}
	public boolean equals(IFieldType fieldType){
		return(fieldType instanceof ArrayFieldType?arrayElementType.equals(((ArrayFieldType)fieldType).getSubFieldType()):false);
	}
	// end IFieldType implementation

	public String toString(){return "["+(arrayElementType.equals(MongooseFieldType.MIXED)?"":arrayElementType.toString())+"]";}

	// the array element type defaults to MIXED
	private IFieldType arrayElementType=MongooseFieldType.MIXED;
	public void setSubFieldType(IFieldType subFieldType){
		/*
		if(arrayElementType==null)throw new NullPointerException("Array element type undefined!");
		if(this.arrayElementType.equals(arrayElementType))throw new IllegalArgumentException("Array element type unchanged!");
		*/
		arrayElementType=subFieldType;
		Utils.consoleprintln("Element field type of '"+toString()+"' set to '"+arrayElementType.toString()+"'.");
	}
	public IFieldType getSubFieldType(){return arrayElementType;}

}
