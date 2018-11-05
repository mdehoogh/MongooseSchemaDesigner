package com.officevitae.marc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Stack;
import java.util.Vector;

/**
 * can be used to edit any text lines (like e.g. the field collection of a Mongoose Schema)
 */
public class JTextLinesEditor extends JPanel implements IMutableTextLinesProducer{

	private static final String LINE_SEPARATOR=System.lineSeparator();

	// support for change listeners
	private Vector<ChangeListener> changeListeners=new Vector<ChangeListener>();
	public boolean addChangeListener(ChangeListener changeListener){return(changeListener!=null?changeListeners.contains(changeListener)||changeListeners.add(changeListener):false);}
	public boolean removeChangeListener(ChangeListener changeListener){return(changeListener==null||!changeListeners.contains(changeListener)||changeListeners.remove(changeListener));}

	private void informChangeListeners(){for(ChangeListener changeListener:changeListeners)try{changeListener.textLinesChanged(this);}catch(Exception ex){}}

	// ALWAYS call setText when you want to set the text showing in textArea so the change listeners can be informed when that happens!!
	private void setText(String text){
		if(textArea.getText().equals(text))return;
		textArea.setText(text);
		informChangeListeners();
	}

	private class HistoryStack extends Stack<String>{
		private boolean synced(){
			return(empty()?false:textArea.getText().equals(history.peek()));
		}
		private void update(){
			if(empty()){
				((CardLayout)(contentsView.getLayout())).show(contentsView,"NoTextLines");
			}else{
				((CardLayout)(contentsView.getLayout())).show(contentsView,"TextLines");
				boolean notsynced=!synced();
				storeButton.setEnabled(notsynced); // when text and top of stack out of sync
				undoButton.setEnabled(history.size()>1?true:notsynced); // if not synced there's something to return to, or else, if we have
			}
		}
		public void syncText(){
			try{
				setText(history.peek());
			}finally{
				update();
			}
		}
		@Override
		public void clear(){
			try{
				super.clear();
				undoButton.setEnabled(false);
			}finally{
				update();
			}
		}
		@Override
		public String push(String text){
			String result=null;
			// if what we're pushing actually is what we already have
			if(text!=null&&(empty()||!text.equals(this.peek()))){ // a new text to push on the stack
				try{
					setText(super.push(text));
					result=textArea.getText();
				}catch(Exception ex){
				}finally{
					update();
				}
			}
			return result; // when the push failed, or was not necessary!!
		}
		@Override
		public String pop(){
			String result=(empty()?null:super.pop());
			setText(result);
			update();
			return result;
		}
	}
	private HistoryStack history=new HistoryStack(); // the history of texts that we may return to
	private JTextArea textArea;
	private JButton undoButton,storeButton;
	private void remember(){ // if we succeed in pushing update
		if(history.push(textArea.getText())!=null)undoButton.setEnabled(history.size()>1);
	}
	private void recall(){
		if(!history.synced())history.syncText();else history.pop();
	}
	private JComponent getButtonView(){
		JPanel undoPanel=new JPanel(new BorderLayout());
		undoPanel.add(undoButton=new JButton("Recall"),BorderLayout.WEST);
		undoButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				recall();
			}
		});
		// if we decide to save, we cannot undo what we did anymore...
		undoPanel.add(storeButton=new JButton("Remember"),BorderLayout.EAST);
		storeButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				remember();
			}
		});
		return undoPanel;
	}
	private JComponent getTextView(){
		JPanel textPanel=new JPanel(new BorderLayout());
		textPanel.add(new JScrollPane(textArea=new JTextArea()));
		// force a mono spaced font!!
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		textArea.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(KeyEvent e){
				informChangeListeners(); // MDH@25OCT2018: assumed change to the text!!!
				// allow remembering it if different from the last item in the history
				history.update();
			}
		});
		textPanel.add(getButtonView(),BorderLayout.SOUTH);
		return textPanel;
	}
	private JLabel noTextLinesLabel,infoLabel;
	private JComponent getInfoView(){
		JPanel infoPanel=new JPanel(new BorderLayout());
		infoPanel.add(infoLabel=new JLabel());
		infoLabel.setToolTipText("Messages will appear here.");
		return infoPanel;
	}
	private void info(String info){infoLabel.setText(info);}
	private JComponent getContentsView(){
		JPanel contentsPanel=new JPanel(new CardLayout());
		contentsPanel.add(noTextLinesLabel=new JLabel("Text lines will appear here when available!"),"NoTextLines");
		contentsPanel.add(getTextView(),"TextLines");
		return contentsPanel;
	}
	private JComponent contentsView;
	public JTextLinesEditor(){
		super(new BorderLayout());
		add(getInfoView(),BorderLayout.NORTH);
		add(contentsView=getContentsView());
	}

	// two methods for either updating from the container, or update the container from me
	public boolean read(){
		// i.e. start over with the
		try{
			textLinesProducer.produceTextLines();
			setText(String.join("\n",textLinesProducer.getProducedTextLines())); // update the view
			history.clear();
			remember(); // keep the current value in the history (so we can go back to it)
			return true;
		}catch(Exception ex){
			System.out.println("'"+ex.getLocalizedMessage()+"' in obtaining the text lines.");
		}
		return false;
	}
	public boolean write(){
		// it's well possible that the user changed the text, so if so we will remember it before storing it
		// WHAT IF the text has NOT changed? this is the case if the current text displayed equals the first element in the history
		// MDH@05NOV2018: should return false when NOT actually calling setTextLines() on the textLinesConsumer and thus indicating change
		if(textLinesConsumer==null)return false;
		try{
			String text=textArea.getText();
			if(!history.isEmpty()){
				if(text.equals(history.firstElement()))return false; // NO change since we started
				if(!text.equals(history.peek()))remember();
			}else
				remember();
			// update textLinesContainer with the new contents
			textLinesConsumer.setTextLines(text.split("\n"));
			return true;
		}catch(Exception ex){
			Utils.consoleprintln("ERROR: '"+ex.getLocalizedMessage()+"' in updating the Mongoose schema from the text.");
		}
		return false;
	}
	private ITextLinesProducer textLinesProducer=null;
	private ITextLinesConsumer textLinesConsumer=null;
	public void setTextLinesProducer(ITextLinesProducer textLinesProducer){
		this.textLinesProducer=textLinesProducer;
		history.clear();
	}
	public void setTextLinesConsumer(ITextLinesConsumer textLinesConsumer){this.textLinesConsumer=textLinesConsumer;}
	// not much to do now we have read() and write() in place and callable, and working!!!
	public void setTextLinesContainer(ITextLinesContainer textLinesContainer){
		setTextLinesConsumer(textLinesContainer);
		setTextLinesProducer(textLinesContainer);
	}

	// ITextLinesProducer implementation
	String[] textLines=null;
	public String[] getProducedTextLines(){return textLines;}
	public void produceTextLines()throws Exception{textLines=textArea.getText().split("\n");}
	// end ITextLinesProducer implementation

	/* replacing:
	public void setTextLinesContainer(ITextLinesContainer textLinesContainer){
		info("Switching to another text lines store.");
		// NOTE textLines==null indicates no text lines available!!
		if(this.textLinesContainer!=null&&!history.empty()){
			if(!textArea.getText().equals(history.peek()))history.push(textArea.getText());
			if(history.size()>=2&&!history.lastElement().equals(history.firstElement())){
				try{
					this.textLinesContainer.setTextLines(textArea.getText().split("\n"));
				}catch(Exception ex){
					info("ERROR: '"+ex.getLocalizedMessage()+"' storing the text lines.");
				}
			}else
				info("Nothing changed!");
		}
		// show NoTextLines card so we will see any error message in noTextLinesLabel
		((CardLayout)(contentsView.getLayout())).show(contentsView,"NoTextLines");
		this.textLinesContainer=textLinesContainer;
		history.clear(); // clear the history...
		if(this.textLinesContainer!=null){
			try{
				String[] textLines=this.textLinesContainer.getTextLines();
				if(textLines==null)
					info("ERROR: No text lines available.");
				else // start with new history
					history.push(String.join("\n",textLines));
			}catch(Exception ex){
				info("ERROR: '"+ex.getLocalizedMessage()+"' loading the text lines.");
			}
		}
		if(!history.empty()){
			// nothing to remember or recall yet
			undoButton.setEnabled(false);
			storeButton.setEnabled(false); // no need to store when unchanged
			textArea.setText(history.peek());
			((CardLayout)(contentsView.getLayout())).show(contentsView,"TextLines");
		}
	}
	*/
}
