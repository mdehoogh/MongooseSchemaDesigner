package com.officevitae.marc;

/**
 * MDH@19OCT2018: initializing text to null instead of "", so we can know when we can validate the text (when it is not null)
 * @param <V>
 */
public abstract class FieldLiteral<V>{

	private String id;
	private Field field; // a field might be associated with the literal
	// MDH@19OCT2018: to prevent cyclic validation behaviour (like with min and maximum checking on each other
	//                I've come up with the idea of initializing text to null indicating that there's no text to validate yet
	//                replacing initializing text to the empty string
	private String text=null; // replacing: "";
	// MDH@19OCT2018: let's not allow returning to null???? yes, let's ALTHOUGH very unlikely...
	protected boolean setText(String text){
		if(this.text!=null||text!=null){ // not both null
			boolean different=(text==null||!text.equals(this.text));
			if(different)this.text=text;
			return different;
		}
		return false;
	}
	public FieldLiteral<V> setId(String id){this.id=id;return this;}
	public FieldLiteral<V> setField(Field field){this.field=field;return this;}
	public String getId(){return id;}
	////////public Literal(String text)throws NullPointerException{if(text==null)throw new NullPointerException("Null text not allowed!");setText(text);}
	public String getText(){return text;}
	// isUndefined() could be overwritten if need be!!
	public boolean isUndefined(){return(text==null||text.isEmpty());}
	public void updateField(){if(field!=null)field.updateChanged();}
	public abstract V getValue(); // the value the literal represents

}
