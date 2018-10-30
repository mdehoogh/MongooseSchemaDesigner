package com.officevitae.marc;

// MDH@16OCT2018: because the user can expand the basic field types with its own field types (by defining subschemas)
public enum MongooseFieldType implements IFieldType{

	// MDH@30OCT2018: it's INTEGER but it should use the Long extension to the schema types provided by mongoose-long
	ARRAY("Array"),BOOLEAN("Boolean"),BUFFER("Buffer"),DATE("Date"),DECIMAL128("Decimal128"),INTEGER("Long"),MAP("Map"),MIXED("Mixed"),NUMBER("Number"),OBJECTID("ObjectId"),STRING("String");

	// we need a fixed (and final) description object, so we create it only once
	private final Description description;
	MongooseFieldType(String description){
		this.description=new Description(){
			public IFieldType getFieldType(){return MongooseFieldType.this;}
			public String toString(){return description;}
		};
	}

	// IFieldType implementation
	public Description getDescription(){return description;}

	public boolean representsAValidValue(String value){
		boolean valid=(value!=null); // null is always invalid!!
		if(valid){
			// the type determines whether a text representation represents a valid value
			switch(ordinal()){
				case 0: // Array, the value needs to represent an array literal
					break;
				case 1: // Boolean
					try{
						Boolean.parseBoolean(value);
					}catch(Exception ex){
						valid=false;
					}
					break;
				case 2: // Buffer
					break;
				case 3: // Date
					break;
				case 4: // Decimal128
					break;
				case 5: // Integer
					break;
				case 6: // Map
					break;
				case 7: // Mixed
					break;
				case 8: // Number
					break;
				case 9: // ObjectId
					break;
				case 10: // String
					break;
			}
		}
		return valid;
	}

	public boolean equals(IFieldType fieldType){
		try{
			return((MongooseFieldType)fieldType).ordinal()==this.ordinal();
		}catch(Exception ex){}
		return false;
	}
	/*
	public int compareTo(IFieldType fieldType){
		try{
			return this.ordinal()-((MongooseFieldType)fieldType).ordinal();
		}catch(Exception ex){}
		return -1; // always smaller than any other field type (MongooseSchema's)
	}
	*/
	// end IFieldType implementation
	public String toString(){return "mongoose.Schema.Types."+description.toString();} // use the full name (assuming the mongoose object is indeed called 'mongoose' as returned by the require('mongoose') call)

}
