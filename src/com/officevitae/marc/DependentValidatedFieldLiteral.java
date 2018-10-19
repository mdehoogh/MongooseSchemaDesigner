package com.officevitae.marc;

public abstract class DependentValidatedFieldLiteral<V> extends ValidatedFieldLiteral<V> implements ValidatedFieldLiteralChangeListener<V>{
	public void invalidated(ValidatedFieldLiteral validatedLiteral){
		System.out.println(getId()+" invalidated by "+validatedLiteral.getId());
		if(!super.isValidated())super.invalidate();
	}
	public void enableChanged(ValidatedFieldLiteral validatedLiteral){
		System.out.println(getId()+" enable changed by "+validatedLiteral.getId());
		super.invalidate();
	}
}
