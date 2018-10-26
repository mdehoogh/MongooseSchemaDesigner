package com.officevitae.marc;

import java.io.File;
import java.io.PrintWriter;

public interface ITextLinesConsumer{

	void setTextLines(String[] textLines) throws Exception;

	public class TextFile implements ITextLinesConsumer{
		private File textFile=null;
		public TextFile(File textFile){this.textFile=textFile;}
		public void setTextLines(String[] textLines)throws Exception{
			File parentFile=textFile.getParentFile();
			// if the parent file does not exist, force creating it
			if(parentFile!=null&&!parentFile.exists()&&!parentFile.mkdirs())throw new Exception("Failed to create directory '"+parentFile.getAbsolutePath()+"'.");
			PrintWriter pw=new PrintWriter(textFile);
			for(String textLine:textLines)pw.println(textLine);
			pw.close();
		}
	}
}
