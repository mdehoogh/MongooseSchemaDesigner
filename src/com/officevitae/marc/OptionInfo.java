package com.officevitae.marc;

public class OptionInfo<T>{

	private T _default=null;
	public T getDefault(){return _default;}

	private String info;
	public String getInfo(){return info;}

	private String name;
	public String getName(){return name;}

	public OptionInfo(String name,String info,T _default){
		this.name=name;
		this.info=info;
		this._default=_default;
	}

}
