package com.officevitae.marc;

import java.util.Vector;

/**
 * a text lines editor group keeps track of a group of text lines providers of which only one can hold the most recent TextLines version
 */
public interface ITextLinesEditorGroup extends ITextLinesContainer{

	// given that is a group we need to be able to add and remove editors
	boolean addTextLinesEditor(ITextLinesContainer textLinesEditor);
	boolean removeTextLinesEditor(ITextLinesContainer textLinesEditor);

	// there's always exactly one current text lines editor
	// I wonder wether or not we have to ask the current text lines editor to stop editing????
	boolean setCurrentEditor(ITextLinesContainer textLinesEditor);
	ITextLinesContainer getCurrentEditor(); // exposes who the current editor is

	public class Base extends Vector<ITextLinesContainer> implements ITextLinesEditorGroup{
		private boolean done=false; // whether or not I'm done editing
		private String[] textLines=null; // the last known version
		public boolean addTextLinesEditor(ITextLinesContainer textLinesEditor){return (textLinesEditor!=null?super.contains(textLinesEditor)||super.add(textLinesEditor):false);}
		public boolean removeTextLinesEditor(ITextLinesContainer textLinesEditor){return(textLinesEditor!=null?!super.contains(textLinesEditor)||super.remove(textLinesEditor):false);}
		private ITextLinesContainer currentTextLinesEditor=this; // as long as no specific text line editor is selected, I am the current text lines editor in charge
		public boolean setCurrentEditor(ITextLinesContainer textLinesEditor){
			if(currentTextLinesEditor==textLinesEditor)return true; // NOTE not using equals here, we need an exact match
			if(this.currentTextLinesEditor==null){ //////||this.currentTextLinesEditor.doneEditing()){ // allowed to swith the current text lines editor
				try{
					// update the current version of the text lines from the current text lines editor!!
					// the text lines editor SHOULD return null if it didn't edit the text lines at all!!!
					currentTextLinesEditor.produceTextLines();
					String[] textLines=currentTextLinesEditor.getProducedTextLines();
					if(textLines!=null)this.textLines=textLines; // remember the last known (changed) text lines
					if(textLinesEditor!=null)textLinesEditor.setTextLines(this.textLines);
					currentTextLinesEditor=textLinesEditor;
				}catch(Exception ex){
					System.out.println("ERROR: '"+ex.getLocalizedMessage()+"' passing the edited text lines over.");
				}
			}
			return(currentTextLinesEditor==textLinesEditor);
		}
		public boolean doneEditing(){return(done=true);}
		public ITextLinesContainer getCurrentEditor(){return currentTextLinesEditor;}
		public void setTextLines(String[] textLines){
			// typically these text lines should go to the current text lines editor??
		}
		public void produceTextLines()throws Exception{
			// first update what we currently have if need be
			currentTextLinesEditor.produceTextLines();
			this.textLines=currentTextLinesEditor.getProducedTextLines();
		}
		public String[] getProducedTextLines(){return textLines;}
	}

}
