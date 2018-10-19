package com.officevitae.marc;

public class ArrayFieldType implements IFieldType{

	// IFieldType implementation
	public Description getDescription(){
		return new Description(){
			public IFieldType getFieldType(){return MongooseFieldType.ARRAY;} // this is a bit weird I suppose!!
			// if the element type is undefined i.e. anything goes we return the word Array, otherwise the type of the element enclosed in square brackets...
			public String toString(){return "["+(arrayElementType.equals(MongooseFieldType.MIXED)?"":arrayElementType.getDescription().toString())+"]";}
		};
	}
	public boolean representsAValidValue(String value){
		return true;
	}
	public boolean equals(IFieldType fieldType){
		return(fieldType instanceof ArrayFieldType?arrayElementType.equals(((ArrayFieldType)fieldType).getArrayElementType()):false);
	}
	// end IFieldType implementation

	// the array element type defaults to MIXED
	private IFieldType arrayElementType=MongooseFieldType.MIXED;
	public ArrayFieldType setArrayElementType(IFieldType arrayElementType)throws Exception{
		if(arrayElementType==null)throw new NullPointerException("Array element type undefined!");
		if(this.arrayElementType.equals(arrayElementType))throw new IllegalArgumentException("Array element type unchanged!");
		return this;
	}
	public IFieldType getArrayElementType(){return arrayElementType;}

}
