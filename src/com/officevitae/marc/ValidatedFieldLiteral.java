package com.officevitae.marc;

import java.util.Vector;

/**
 * MDH@19OCT2018: adapted to deal with the situation that text is still null!!
 * @param <V>
 */
public abstract class ValidatedFieldLiteral<V> extends FieldLiteral<V>{
	// allow for change listeners which we can tell that the literal was invalidated
	private Vector<ValidatedFieldLiteralChangeListener> changeListeners=new Vector<ValidatedFieldLiteralChangeListener>(); // anybody can listen does not need to be a V instance!!
	private boolean disabled=false; // MDH@63th birthday: I suppose we're going to track the enabled flag as well, to be controlled by the user with the checkbox!!
	private String validText=null; // by keeping track of the last validText we know when to allow the user to turn on the check box manually even if the current text is invalid!!!
	private boolean validated=false; // assume initially validated
	private boolean valid=false; // assume invalid with the current text (default): an empty String
	// protected methods
	// any subclass can call setValid() but NOT override it!!
	// when the valid flag is set, remember the current text as the last known valid text
	protected final void setValid(boolean valid){
		this.valid=valid;
		// maintaining validText means remembering the current text as the valid text, when the current text is considered valid, but also clearing validText if it matches the current text tht is considered invalid!!!
		if(this.valid)validText=super.getText();else if(super.getText().equals(validText))validText=null;
		validated=true;
	} // whenever valid is being set, we assume the text to be validated!!
	// instead of validating with a void method validate() we call it getValidText() which returns the actual text when considered valid
	// obviously getValidatedLiteral() MUST always make validated equal to true
	protected boolean isConsideredValid(){
		// the base implementation simply considers the text to be valid when non-empty!!!
		String text=super.getText();
		return(text!=null&&!text.trim().isEmpty());
	}
	protected final synchronized void invalidate(){ // if the text could change validity call invalidate()
		if(!validated)return; // do not allow invalidating more than once (to prevent circular calls!!!)
		validated=false;
		// inform all change listeners that I was invalidated!!!
		// perhaps I shouldn't use an iterator here???
		int changeListenerIndex=changeListeners.size();
		while(--changeListenerIndex>=0)try{changeListeners.get(changeListenerIndex).invalidated(this);}catch(Exception ex){}
		/* replacing:
		synchronized(changeListeners){
			for(ValidatedFieldLiteralChangeListener changeListener:changeListeners)try{changeListener.invalidated(this);}catch(Exception ex){}
		}
		*/
	}
	// end protected methods
	// public methods
	public String getValidText(){return validText;} // exposes the last known valid text which we may return to
	public boolean isValidated(){return validated;} // I suppose I can expose wether the current value is supposed to be valid
	// isValid() is final because it's essential to call setValid() with the assesment of the validity!!!!
	// MDH@19OCT2018: we have to shortcircuit isValid() when super.text is still null, in which case we should NOT try to validate
	public final boolean isValid(){
		// when valid is requested, validate first if so needed
		if(!validated)
			if(super.getText()!=null) // MDH@19OCT2018 INSERTED: if the text is not yet set, do not try to validate (and typically the thing stays invalid)
				setValid(isConsideredValid());
		return valid;
	}
	@Override
	public final synchronized boolean setText(String text){
		// MDH@19OCT2018 REMOVED because super.setText() will also check that but takes care of null values as well: if(text.equals(super.getText()))return false;
		boolean result=super.setText(text);
		if(result){
			// TODO should we do both????? in most cases enableChanged() and invalidate() is responded to in the same way
			if(isUndefined()&&disabled)setDisabled(false);
			invalidate(); // called because validated MUST become false!!!
		}
		return result;
	}
	// call makeValid() to reset the text to the last registered valid Text
	public final void makeValid() throws NullPointerException{if(validText==null)throw new NullPointerException("BUG: Unable to revert to the last known valid text. No valid text defined!");setText(validText);}

	public final boolean addValidatedFieldLiteralChangeListener(ValidatedFieldLiteralChangeListener changeListener){return(changeListener!=null?changeListeners.contains(changeListener)||changeListeners.add(changeListener):false);}

	public final boolean removeValidatedFieldLiteralChangeListener(ValidatedFieldLiteralChangeListener changeListener){return(changeListener!=null?!changeListeners.contains(changeListener)||changeListeners.remove(changeListener):false);}

	public final boolean isDisabled(){return disabled;}
	public void setDisabled(boolean disabled){
		if(this.disabled==disabled)return;
		this.disabled=disabled;
		for(ValidatedFieldLiteralChangeListener changeListener:changeListeners)try{changeListener.enableChanged(this);}catch(Exception ex){}
	}
}
