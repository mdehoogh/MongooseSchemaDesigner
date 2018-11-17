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

	private T initialValue=null; // whatever we parse we consider to be the initial value!!!
	protected T value=null; // starts as its 'default'
	private void parse(String valueText)throws Exception{
		T _default=getDefault();
		if(_default instanceof Boolean)setValue((T)Boolean.valueOf(valueText));else
		if(_default instanceof String)setValue((T)Utils.dequote(valueText));else
		if(_default instanceof Integer)setValue((T)Integer.valueOf(valueText));else
		if(_default instanceof Long)setValue((T)Long.valueOf(valueText));else
		if(_default instanceof Double)setValue((T)Double.valueOf(valueText));else
		if(_default instanceof Float)setValue((T)Float.valueOf(valueText));else
			throw new Exception("Unable to parse value text '"+valueText+"'.");
	}
	// obviously any value should be of type T
	// MDH@08NOV2018: typically used for setting the value from predefined options which is only possible
	public void assumeInitialized(){
		initialValue=value;
		// definititely unchanged now...
		if(optionCollection!=null)optionCollection.optionUnchanged(optionIndex);
	}
	void initialize(){
		value=initialValue;
		// definititely unchanged now...
		if(optionCollection!=null)optionCollection.optionUnchanged(optionIndex);
	}
	public void parseInitialValue(String valueText)throws Exception{
		parse(valueText);
		assumeInitialized();
	}
	public boolean isChanged(){
		boolean changed=(initialValue==null?this.value!=null:!initialValue.equals(this.value));
		if(initialValue!=null)
			Utils.consoleprintln("Initial value '"+initialValue+"' does "+(changed?"":"NOT")+" differ from '"+(value==null?"?":value.toString())+"'.");
		return changed;
	}
	public void parseValue(String valueText) throws Exception{
		parse(valueText);
		if(optionCollection!=null)if(isChanged())optionCollection.optionChanged(optionIndex);else optionCollection.optionUnchanged(optionIndex);
	}

	// let's return the default if no value was set yet
	public T getValue(){return(value==null?getDefault():value);}

	// MDH@17NOV2018: isDefault() means that the value of the parent of the option collection is used as default which might differ from the option default
	// MDH@17NOV2018: simplifying by ascertaining that value is null, when it equals the default (see setValue)
	public boolean isDefault(){
		return(value==null);
		/* replacing:
		if(value==null)return true; // any null value automatically equals the default!!
		T _default=getDefault();
		boolean result=value.equals(_default);
		Utils.consoleprintln("'"+_default.toString()+"' does "+(result?"":" NOT ")+"equal '"+value.toString()+"'.");
		return result;
		*/
	}

	public String toString(){
		T value=getValue();
		// enquote the value if it is a String
		String name=getName();
		if(value instanceof String)return getName()+":'"+value+"'";
		return name+":"+value.toString();
	}

	public Option setValue(T value){
		this.value=(value!=null&&value.equals(getDefault())?null:value);
		// MDH@16NOV2018: bit of an issue here to determine when the option is changed or unchanged...
		if(optionCollection!=null)if(isChanged())optionCollection.optionChanged(optionIndex);else optionCollection.optionUnchanged(optionIndex);
		return this;
	}

}
