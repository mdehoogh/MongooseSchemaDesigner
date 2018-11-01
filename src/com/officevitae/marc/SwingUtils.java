package com.officevitae.marc;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Enumeration;

public class SwingUtils{

	private static JFrame showInfoFrame=null;
	private static InfoMessageViewer infoMessageViewer=null;
	public static void showInfoFrame(Object source,String title){
		if(showInfoFrame==null){
			showInfoFrame=new JFrame();
			showInfoFrame.getContentPane().setLayout(new BorderLayout());
			showInfoFrame.getContentPane().add(infoMessageViewer=new InfoMessageViewer());
			showInfoFrame.setSize(1024,768);
			showInfoFrame.setLocationRelativeTo(null); // center
			showInfoFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		}
		// ascertain to show the right messages (that of the current Mongoose schema!)
		infoMessageViewer.setSource(source);
		showInfoFrame.setTitle(title);
		showInfoFrame.setVisible(true);
	}

	public static JPanel getTitledPanel(String title){
		JPanel titledPanel=new JPanel(new BorderLayout());
		if(title!=null)titledPanel.setBorder(new TitledBorder(title));
		return titledPanel;
	}

	public static JComponent getLeftAlignedView(JComponent jComponent){
		JPanel leftAlignedPanel=new JPanel(new BorderLayout());
		leftAlignedPanel.add(jComponent,BorderLayout.WEST);
		return leftAlignedPanel;
	}

	public static JComponent getLabelView(String text){
		JLabel label=new JLabel(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		return getLeftAlignedView(label);
	}

	public static String getSelectedOption(String title,String message,String[] options){
		return JOptionPane.showInputDialog(null,message,title,JOptionPane.PLAIN_MESSAGE,null,options,0).toString();
	}

	private static java.util.Map<JRootPane,JButton> defaultButtonMap=new java.util.HashMap<JRootPane,JButton>();
	public static void setDefaultButton(JButton button){
		JRootPane buttonRootPane=SwingUtilities.getRootPane(button);
		defaultButtonMap.put(buttonRootPane,button);
		buttonRootPane.setDefaultButton(button);
	}
	public static void unsetDefaultButton(JButton button){
		JRootPane buttonRootPane=SwingUtilities.getRootPane(button);
		if(!defaultButtonMap.containsKey(buttonRootPane))return;
		if(defaultButtonMap.get(buttonRootPane).equals(button)&&defaultButtonMap.remove(buttonRootPane)!=null)buttonRootPane.setDefaultButton(null);
	}
	public static void removeDefaultButton(JButton button){
		SwingUtilities.getRootPane(button).setDefaultButton(null);
	}

	public static final Border NARROW_BORDER=BorderFactory.createEmptyBorder(8,0,8,0);

	public static JButton getButton(String buttonText,boolean transparent,boolean bold,Border border){
		JButton button=new JButton(buttonText);
		if(bold)button.setFont(button.getFont().deriveFont(Font.BOLD));
		if(border!=null)button.setBorder(border);
		if(transparent){button.setOpaque(false);button.setContentAreaFilled(false);button.setBorderPainted(false);}
		return button;
	}

	public static void expandAllNodes(JTree tree) {
		int j = tree.getRowCount();
		int i = 0;
		while(i < j) {
			tree.expandRow(i);
			i += 1;
			j = tree.getRowCount();
		}
	}

	public static void expandNode(JTree tree,DefaultMutableTreeNode treeNode){
		for(int childIndex=0;childIndex<treeNode.getChildCount();childIndex++)
			tree.expandPath(new TreePath(((DefaultMutableTreeNode)treeNode.getChildAt(childIndex)).getPath()));
	}

	public static DefaultMutableTreeNode getTreeNodeOfUserObject(DefaultMutableTreeNode root,Object userObject){
		@SuppressWarnings("unchecked")
		Enumeration<TreeNode> e=root.depthFirstEnumeration();
		DefaultMutableTreeNode node;
		while (e.hasMoreElements()){
			try{
				node=(DefaultMutableTreeNode)e.nextElement();
				if (userObject.equals(node.getUserObject()))return node;
			}catch(Exception ex){}
		}
		return null;
	}

	// return the path to the node with a given user object
	public static TreePath getTreePathOfUserObject(DefaultMutableTreeNode root,Object userObject){
		try{return new TreePath(getTreeNodeOfUserObject(root,userObject).getPath());}catch(Exception ex){}return null;
	}

}
