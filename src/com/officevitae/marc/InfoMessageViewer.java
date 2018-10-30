package com.officevitae.marc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InfoMessageViewer extends JPanel implements Utils.InfoMessageListener{

	private Object source;
	public Object getSource(){return source;}
	public void infoMessagesChanged(){
		infoMessagesTextArea.setText(String.join("\n",Utils.getInfoMessages(this.source)));
	}
	private JTextArea infoMessagesTextArea;
	private JButton clearButton;
	public InfoMessageViewer(){
		super(new BorderLayout());
		Utils.addInfoMessageListener(this); // listen in to any changes...
		add(infoMessagesTextArea=new JTextArea());
		////////infoMessagesTextArea.setEnabled(false);
		JPanel buttonPanel=new JPanel();
		buttonPanel.add(clearButton=new JButton("Clear"));
		clearButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				Utils.removeInfoMessages(source);
				infoMessagesTextArea.setText("");
			}
		});
		clearButton.setEnabled(false);
		add(buttonPanel,BorderLayout.SOUTH);
	}
	public InfoMessageViewer setSource(Object source){
		Utils.removeInfoMessageListener(this);
		this.source=source;
		if(this.source!=null){
			clearButton.setEnabled(this.source!=null&&Utils.hasInfoMessages(this.source));
			try{
				infoMessagesTextArea.setText(String.join("\n",Utils.getInfoMessages(this.source)));
			}catch(Exception ex){
				infoMessagesTextArea.setText("");
			}
			Utils.addInfoMessageListener(this); // inform me about any changes...
		}else
			infoMessagesTextArea.setText("");
		return this;
	}

}
