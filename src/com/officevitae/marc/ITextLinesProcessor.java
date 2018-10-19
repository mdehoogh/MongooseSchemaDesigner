package com.officevitae.marc;

import java.io.File;
import java.io.PrintWriter;

/**
 * a TextLinesProcessor processes the text lines provided but does not remember them
 */

public interface ITextLinesProcessor extends ITextLinesProducer,ITextLinesConsumer{

	public class TextFile extends ITextLinesProducer.TextFile implements ITextLinesProcessor{
		// the producer takes care of reading the text lines
		public TextFile(File file){super(file);}
		// NOTE that the container does NOT keep the text files (as one would expect from a 'Container')
		public void setTextLines(String[] textLines)throws Exception{
			PrintWriter pw=new PrintWriter(getFile());
			for(String textLine:textLines)pw.println(textLine);
			pw.close();
		}
	}

}
