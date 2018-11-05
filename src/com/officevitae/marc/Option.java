package com.officevitae.marc;

public class Option<T>{

	private T _default=null;
	public T getDefault(){return _default;}

	private String info;
	public String getInfo(){return info;}

	private String name;
	public String getName(){return name;}

	public Option(String name,String info,T _default){
		this.name=name;
		this.info=info;
		this._default=_default;
	}

	protected T value=null;
	// obviously any value should be of type T
	public void parseValue(String valueText) throws Exception{
		if(_default instanceof Boolean)value=(T)Boolean.valueOf(valueText);else
		if(_default instanceof String)value=(T)Utils.dequote(valueText);else
		if(_default instanceof Integer)value=(T)Integer.valueOf(valueText);else
		if(_default instanceof Long)value=(T)Long.valueOf(valueText);else
		if(_default instanceof Double)value=(T)Double.valueOf(valueText);else
		if(_default instanceof Float)value=(T)Float.valueOf(valueText);else
		throw new Exception("Unable to parse value text '"+valueText+"'.");
	}
	// let's return the default if no value was set yet
	public T getValue(){return(value==null?_default:value);}

	public boolean isDefault(){
		if(value==null)return true; // any null value automatically equals the default!!
		boolean result=value.equals(_default);
		Utils.consoleprintln("'"+_default.toString()+"' does "+(result?"":" NOT ")+"equal '"+value.toString()+"'.");
		return result;
	}
	public String toString(){
		T value=getValue();
		// enquote the value if it is a String
		if(value instanceof String)return name+":'"+value+"'";
		return name+":"+value.toString();
	}

}
