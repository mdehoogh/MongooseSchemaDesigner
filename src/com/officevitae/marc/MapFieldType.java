package com.officevitae.marc;

public class MapFieldType implements ICompositeFieldType{

	private IFieldType ofFieldType=MongooseFieldType.MIXED;

	private Description description=null;
	public Description getDescription(){
		if(description==null)
			description=new Description(){
			public IFieldType getFieldType(){return MapFieldType.this;}
			// MDH@25OCT2018: the description should provide the 'internal' designer representation of the field type
			public String toString(){
				String descriptionText=MongooseFieldType.MAP.getDescription().toString(); // shorthand 'internal' notation of a Schema.Types.MAP
				if(ofFieldType.equals(MongooseFieldType.MIXED))return descriptionText;
				return descriptionText+" of "+ofFieldType.getDescription().toString();
			}
		};
		return description;
	}

	public String toString(){
		// the representation should be JavaScript style, so it can be used immediately
		// MDH@20NOV2018: strangely enough Mongoose didn't like mongoose.Schema.Types.MAP so we're sticking to just Map!!!
		String representation=MongooseFieldType.MAP.getDescription().toString(); // the full representation of the generic MAP instance
		if(ofFieldType.equals(MongooseFieldType.MIXED))return representation; // just Map
		// the External tag forces the use of getDescription().toString() instead of toString() itself (so function as a wrapper to return another text)
		return "{type:"+representation+",of:"+(ofFieldType instanceof IFieldType.External?ofFieldType.getDescription().toString():ofFieldType.toString())+"}";
		// replacing: return "("+(ofFieldType.equals(MongooseFieldType.MIXED)?"":ofFieldType.getDescription().toString())+")";
	}

	public boolean representsAValidValue(String valueText){
		return true;
	}

	public void setSubFieldType(IFieldType subFieldType){
		/*
		if(arrayElementType==null)throw new NullPointerException("Array element type undefined!");
		if(this.arrayElementType.equals(arrayElementType))throw new IllegalArgumentException("Array element type unchanged!");
		*/
		ofFieldType=subFieldType;
		Utils.consoleprintln("Map values field type set to '"+ofFieldType.toString()+".");
	}
	public IFieldType getSubFieldType(){return ofFieldType;}
}
