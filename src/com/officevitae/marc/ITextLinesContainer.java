package com.officevitae.marc;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * a TexTLines editor basically is something that can change the text it receives
 */
public interface ITextLinesContainer extends ITextLinesProducer,ITextLinesConsumer {

	/////////boolean doneEditing(); // ask politely to stop editing when so required...

	public class Base extends ITextLinesProducer.Base implements ITextLinesContainer{
		private String[] textLines=null; // means nothing to provide
		public void setTextLines(String[] textLines) throws Exception{
			if(textLines==null)throw new NullPointerException("Undefined text lines not allowed.");
			super.clear();
			if(textLines.length>0)super.addAll(Arrays.asList(textLines));
		}
		public String[] getTextLines()throws Exception{
			return (String[])super.toArray(new String[super.size()]);
		}
	}

}
