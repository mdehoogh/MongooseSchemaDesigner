package com.officevitae.marc;

public class Option<T>{

	private int optionIndex;
	private OptionCollection optionCollection;

	public OptionInfo getOptionInfo(){return optionCollection.getOptionInfo(optionIndex);}

	public String getInfo(){return getOptionInfo().getInfo();}
	public String getName(){return getOptionInfo().getName();}

	// MDH@11NOV2018: the default is either the 'default' in the associated option collection
	private T getDefault(){return(T)optionCollection.getDefault(optionIndex);}

	// needs to know it's option collection to ask for the actual default to use...
	public Option(OptionCollection optionCollection,int optionIndex){
		this.optionCollection=optionCollection;
		this.optionIndex=optionIndex;
	}

	protected T value=null;
	// obviously any value should be of type T
	public void parseValue(String valueText) throws Exception{
		T _default=getDefault();
		if(_default instanceof Boolean)value=(T)Boolean.valueOf(valueText);else
		if(_default instanceof String)value=(T)Utils.dequote(valueText);else
		if(_default instanceof Integer)value=(T)Integer.valueOf(valueText);else
		if(_default instanceof Long)value=(T)Long.valueOf(valueText);else
		if(_default instanceof Double)value=(T)Double.valueOf(valueText);else
		if(_default instanceof Float)value=(T)Float.valueOf(valueText);else
		throw new Exception("Unable to parse value text '"+valueText+"'.");
		// inform OptionCollection
		if(optionCollection!=null)optionCollection.optionChanged(optionIndex);
	}
	// let's return the default if no value was set yet
	public T getValue(){return(value==null?getDefault():value);}

	public boolean isDefault(){
		if(value==null)return true; // any null value automatically equals the default!!
		T _default=getDefault();
		boolean result=value.equals(_default);
		Utils.consoleprintln("'"+_default.toString()+"' does "+(result?"":" NOT ")+"equal '"+value.toString()+"'.");
		return result;
	}

	public String toString(){
		T value=getValue();
		// enquote the value if it is a String
		String name=getName();
		if(value instanceof String)return getName()+":'"+value+"'";
		return name+":"+value.toString();
	}

	// MDH@08NOV2018: typically used for setting the value from predefined options which is only possible
	public void setValue(T value){
		this.value=(value!=null&&value.equals(getDefault())?null:value);
	}

}
