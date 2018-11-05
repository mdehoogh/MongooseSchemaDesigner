package com.officevitae.marc;

/**
 * wraps a String without looking like one!!
 */

public class JavaScriptObject{
	// TODO ascertain that object actually does represent a JavaScript object literal...
	protected String object;
	public JavaScriptObject(String object){this.object=object;}
	public String toString(){return object;}
	public boolean equals(Object o){
		try{return((JavaScriptObject)o).object.equals(object);}catch(Exception ex){}
		return false;
	}
}
