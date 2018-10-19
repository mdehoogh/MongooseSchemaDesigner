package com.officevitae.marc;

public interface ValidatedFieldLiteralChangeListener<V>{
	void invalidated(ValidatedFieldLiteral<V> validatedLiteral);
	void enableChanged(ValidatedFieldLiteral<V> validatedLiteral);
}
