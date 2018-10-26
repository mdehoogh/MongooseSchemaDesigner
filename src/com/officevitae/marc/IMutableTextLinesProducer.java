package com.officevitae.marc;

/**
 * a version that allows change listeners if the text lines change...
 */
public interface IMutableTextLinesProducer extends ITextLinesProducer{

	public interface ChangeListener{
		void textLinesChanged(IMutableTextLinesProducer textLinesProducer);
	}

	boolean addChangeListener(IMutableTextLinesProducer.ChangeListener changeListener);
	boolean removeChangeListener(IMutableTextLinesProducer.ChangeListener changeListener);

}
