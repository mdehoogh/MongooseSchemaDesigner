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

	private T value=null;
	// obviously any value should be of type T
	public void setValue(Object value) throws ClassCastException{
		this.value=(T)value;
	}
	// let's return the default if no value was set yet
	public T getValue(){return(value==null?_default:value);}

	public boolean isDefault(){return(value==null);}

	public String toString(){return name+"="+value.toString();}

}
