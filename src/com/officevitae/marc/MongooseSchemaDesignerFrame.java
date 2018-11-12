package com.officevitae.marc;

/**
 * MDH@08OCT2018: if we want to be able to edit the schema of subdocuments we need to take out the 'MongooseSchemaView' instance
 */
/*
import com.sun.source.util.SimpleTreeVisitor;
import javafx.scene.control.TextFormatter;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.IRFactory;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.Symbol;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;
*/
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
/*
import jdk.nashorn.api.scripting.ScriptUtils;
import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.ECMAException;
import jdk.nashorn.internal.runtime.ErrorManager;
import jdk.nashorn.internal.runtime.options.Options;
*/
/*
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
*/

public class MongooseSchemaDesignerFrame extends JFrame implements IInfoViewer,MongooseSchema.SyncListener,MongooseSchemaCollection.SyncListener {

	///////////private Vector<String> toplevelSchemaNames=new Vector<String>();

    private JLabel infoLabel;
    private JComponent getInfoView(){
        JPanel infoPanel=SwingUtils.getTitledPanel(null);
        ////////infoPanel.add(new JLabel("Info: "),BorderLayout.WEST);
        infoPanel.add(infoLabel=new JLabel(" "));
        infoLabel.setToolTipText("Informative messages like notes, warnings and errors will appear here. Click to remove.");
        infoLabel.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				Utils.setInfo(MongooseSchemaDesignerFrame.this," ");
			}
		});
        infoLabel.setForeground(Color.BLUE);
        JButton infoButton;
        infoPanel.add(infoButton=new JButton("Show info log"),BorderLayout.EAST);
		infoButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){SwingUtils.showInfoFrame(MongooseSchemaDesignerFrame.this,"Information messages log");}
		});
		return infoPanel;
    }

    // IInfoViewer implementation
    public void setInfo(String info){
    	// whatever gets here was sent with source null, so we could show it to the user
    	if(info==null)return;
    	if(!info.trim().isEmpty()){
			infoLabel.setText(info);
			// store anyway
			Utils.storeInfo(this,info);
		}else
			infoLabel.setText(" ");
    }
    // end IInfoViewer implementation

    private JTextField newMongooseSchemaTextField;
    private MongooseSchemaDesignEditorView mongooseSchemaEditorView;
    private MongooseSchemaCollectionEditorView mongooseSchemaCollectionEditorView;
    private JComponent getMongooseSchemaView(){
        return (mongooseSchemaEditorView=new MongooseSchemaDesignEditorView()); // that's easy
    }
    private JComponent getMongooseSchemaCollectionView(){
    	return(mongooseSchemaCollectionEditorView=new MongooseSchemaCollectionEditorView());
	}
    private JButton newMongooseSchemaButton;
    // convenient to have a TreeNode class that we can set the sync listener on
    private class MongooseSchemaTreeNode extends DefaultMutableTreeNode{
    	public MongooseSchemaTreeNode(MongooseSchema mongooseSchema){super(mongooseSchema);}
    	public DefaultMutableTreeNode setSyncListener(MongooseSchema.SyncListener syncListener){((MongooseSchema)super.getUserObject()).addSchemaSyncListener(syncListener);return this;}
	}
    // given a (complete) Mongoose schema we can construct the associated tree node (with current children and all!!) RECURSIVE!!!
    private DefaultMutableTreeNode getMongooseSchemaTreeNode(MongooseSchema mongooseSchema){
        DefaultMutableTreeNode mongooseSchemaTreeNode=new MongooseSchemaTreeNode(mongooseSchema).setSyncListener(this); // only show the exposed ID object (which wraps the Mongoose Schema itself)
        for(MongooseSchema mongooseSubSchema:mongooseSchema.getSubSchemas())mongooseSchemaTreeNode.add(getMongooseSchemaTreeNode(mongooseSubSchema));
        return mongooseSchemaTreeNode;
    }
    private void selectTreePath(TreePath treePath){
		mongooseSchemasTree.expandPath(treePath); // force seeing it!!
		((DefaultTreeModel)mongooseSchemasTree.getModel()).reload(); /////nodeStructureChanged(selectedMongooseSchemaNode);
		mongooseSchemasTree.setSelectionPath(treePath);
	}
    private TreePath getANewMongooseSchemaTreePath(String name,MongooseSchema parent,MongooseSchemaCollection collection){
    	TreePath treePath=null;
        try {
        	// MDH@31OCT2018: I do not think we need MongooseSchemaFactory anymore as a schema is also part of a collection
            MongooseSchema newMongooseSchema=new MongooseSchema(name,parent,collection); // MDH@08OCT2018: go through the factory because that is used to retrieve the names of the mongoose schema's available!!!
            // NOTE call getMongooseSchemaTreeNode() because it will automatically append the subschema tree nodes as well!!!
            DefaultMutableTreeNode newMongooseSchemaTreeNode=getMongooseSchemaTreeNode(newMongooseSchema);
            selectedMongooseSchemaNode.add(newMongooseSchemaTreeNode);
			return new TreePath(newMongooseSchemaTreeNode.getPath());
        }catch(Exception ex){
            Utils.setInfo(null,"ERROR: '"+ex.getLocalizedMessage()+"' creating a new Mongoose schema.");
        }
        return null;
    }
	private TreePath getANewJavaScriptMongooseSchemaTreePath(String name,JavaScriptMongooseSchema parent,MongooseSchemaCollection collection){
		TreePath treePath=null;
		try {
			JavaScriptMongooseSchema newMongooseSchema=(parent!=null?new JavaScriptMongooseSchema(name,parent,collection):MongooseSchemaFactory.getANewJavaScriptMongooseSchema(name,collection)); // MDH@08OCT2018: go through the factory because that is used to retrieve the names of the mongoose schema's available!!!
			// NOTE call getMongooseSchemaTreeNode() because it will automatically append the subschema tree nodes as well!!!
			DefaultMutableTreeNode newMongooseSchemaTreeNode=getMongooseSchemaTreeNode(newMongooseSchema);
			selectedMongooseSchemaNode.add(newMongooseSchemaTreeNode);
			return new TreePath(newMongooseSchemaTreeNode.getPath());
		}catch(Exception ex){
			Utils.setInfo(null,"ERROR: '"+ex.getLocalizedMessage()+"' creating a new JavaScript Mongoose schema.");
		}
		return null;
	}
/*
    private boolean isExistingMongooseSchemaname(String name){
        for(int schemaIndex=0;schemaIndex<mongooseSchemasList.getModel().getSize();schemaIndex++)
            if (mongooseSchemasList.getModel().getElementAt(schemaIndex).toString().equals(name))
                return true;
        return false;
    }
*/
    private MongooseSchema getSelectedMongooseSchema(){
        try{return((MongooseSchema)selectedMongooseSchemaNode.getUserObject());}catch(Exception ex){}
        return null;
    }
    private JComponent getNewMongooseSchemaView(){
        JPanel newMongooseSchemaPanel=new JPanel(new BorderLayout());
        newMongooseSchemaPanel.add(newMongooseSchemaButton=new JButton("Add"),BorderLayout.WEST);
        newMongooseSchemaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	Object selectedObject=getSelectedObject();
            	MongooseSchemaCollection collection=null;try{collection=(MongooseSchemaCollection)getSelectedObject();}catch(Exception ex){}
            	// allowing multiple schema's comma-delimited
				String[] newSchemaNameTexts=newMongooseSchemaTextField.getText().trim().split(",");
				TreePath newSchemaTreePath,lastCreatedSchemaTreePath=null;
				// iterate over the names and remember the last successively created tree path (to select)
				MongooseSchema selectedMongooseSchema=getSelectedMongooseSchema();
				String newSchemaName;
				for(String newSchemaNameText:newSchemaNameTexts){
					newSchemaName=newSchemaNameText.trim();
					if(newSchemaName.isEmpty()) continue;
					if(newSchemaName.indexOf('.')<0){
						if(!isExistingSchemaName(newSchemaName,selectedObject)){
							newSchemaTreePath=getANewMongooseSchemaTreePath(newSchemaName,selectedMongooseSchema,collection);
							if(newSchemaTreePath!=null)lastCreatedSchemaTreePath=newSchemaTreePath;
						}else Utils.setInfo(this,"Schema '"+newSchemaName+"' not created: it already exists.");
					}else
						Utils.setInfo(this,"New schema name '"+newSchemaName+"' skipped: it contains an invalid period character.");
				}
				// selecting the new tree path will result in a new schema to be selected and evaluate all names entered and determine whether to enable/disable the button
				// theoretically we should leave the button enabled if we fail to actually create any new tree paths, so benefit of the doubt!!!!
                if(lastCreatedSchemaTreePath!=null)selectTreePath(lastCreatedSchemaTreePath);///// benefit of the doubt: else newMongooseSchemaButton.setEnabled(false);
                /////SwingUtils.setDefaultButton(null);
            }
        });
        newMongooseSchemaButton.setEnabled(false); // text field not set, so assume not a valid new table name!!!
        newMongooseSchemaPanel.add(newMongooseSchemaTextField=new JTextField());
        newMongooseSchemaTextField.setToolTipText("Use a comma to delimit the names of the schemas to create.");
        newMongooseSchemaTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
            	checkForNewSchemaNames();
            }
        });
        return newMongooseSchemaPanel;
    }
    private MongooseSchemaCollection getSelectedMongooseSchemaCollection(){return(MongooseSchemaCollection)selectedMongooseSchemaNode.getUserObject();}
    private JButton saveMongooseSchemaCollectionButton;
    private JComponent getSaveMongooseSchemaCollectionView(){
    	JPanel saveMongooseSchemaCollectionPanel=new JPanel(new BorderLayout());
    	saveMongooseSchemaCollectionPanel.add(saveMongooseSchemaCollectionButton=new JButton("Save schema collection"),BorderLayout.WEST);
    	saveMongooseSchemaCollectionButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				String[] unsavedMongooseSchemaNames=getSelectedMongooseSchemaCollection().unsaved();
				if(unsavedMongooseSchemaNames.length>0)Utils.setInfo(null,"ERROR: Failed to save schema(s) "+String.join(", ",unsavedMongooseSchemaNames)+".");
			}
		});
    	saveMongooseSchemaCollectionButton.setEnabled(false); // assuming there's no currently selected Mongoose Schema collection
    	return saveMongooseSchemaCollectionPanel;
	}
    private JComponent getMongooseSchemaButtonView(){
    	JPanel mongooseSchemaButtonPanel=new JPanel(new BorderLayout());
    	mongooseSchemaButtonPanel.add(getNewMongooseSchemaView(),BorderLayout.NORTH);
    	mongooseSchemaButtonPanel.add(getSaveMongooseSchemaCollectionView(),BorderLayout.SOUTH);
    	return mongooseSchemaButtonPanel;
	}
    /*
    private JList mongooseSchemasList;
    private MongooseSchema getMongooseSchemaWithName(String tableName){
        ListModel<MongooseSchema> mongooseSchemaListModel=mongooseSchemasList.getModel();
        int tableIndex=mongooseSchemaListModel.getSize();
        MongooseSchema mongooseSchema;
        while(--tableIndex>=0){
            mongooseSchema=mongooseSchemaListModel.getElementAt(tableIndex);
            if(mongooseSchema.getName().equalsIgnoreCase(tableName))return mongooseSchema;
        }
        return null;
    }
    */
    private JScrollPane mongooseSchemasTreeScrollPane;
    /*
    public class CustomTreeUI extends BasicTreeUI{
        @Override
        protected AbstractLayoutCache.NodeDimensions createNodeDimensions(){
            return new NodeDimensionsHandler(){
                @Override
                public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded, Rectangle size) {
                    Rectangle dimensions = super.getNodeDimensions(value, row, depth, expanded, size);
                    dimensions.width =mongooseSchemasTreeScrollPane.getWidth() - getRowX(row, depth);
                    return dimensions;
                }
            };
        }
        @Override
        protected void paintHorizontalLine(Graphics g, JComponent c, int y, int left, int right) {
            // do nothing.
        }
        @Override
        protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets, TreePath path) {
            // do nothing.
        }
    }
    */
    // MDH@15OCT2018: preferable to use a Tree view instead of a list so we can also show subschema's!!!
    private JTree mongooseSchemasTree=null;
    private DefaultMutableTreeNode schemasTreeRootNode=new DefaultMutableTreeNode(new MongooseSchemaCollection(".",this)); // indicate the schemas folder itself
    private DefaultMutableTreeNode selectedMongooseSchemaNode=schemasTreeRootNode;

    // MongooseSchema.SyncListener implementation
	public void syncChanged(MongooseSchema mongooseSchema){
		Utils.consoleprintln("Mongoose schema designs editor responding to the sync status of the schema with representation '"+mongooseSchema.getRepresentation(true)+"'.");
		// find the node in the tree view (if any), and if so make it update itself
		TreeNode mongooseSchemaTreeNode=SwingUtils.getTreeNodeOfUserObject(schemasTreeRootNode,mongooseSchema);
		if(mongooseSchemaTreeNode!=null){
			//////mongooseSchemasTree.getModel().valueForPathChanged(mongooseSchemaTreePath,mongooseSchema);
			((DefaultTreeModel)mongooseSchemasTree.getModel()).reload(mongooseSchemaTreeNode); /////nodeStructureChanged(selectedMongooseSchemaNode);
		}
		else Utils.consoleprintln("NOTE: The Mongoose schema design that changed ("+mongooseSchema.getName()+") not found in the Mongoose schema design tree view.");
	}
	// end MongooseSchema.SyncListener impl.
	// MongooseSchemaCollection.SyncListener implementation
	public void syncChanged(MongooseSchemaCollection mongooseSchemaCollection){
		mongooseSchemaCollectionEditorView.refresh();
	}
	// end MongooseSchemaCollection.SyncListener implementation

	// on any occasion it should not be allowed to add (sub)schemas with existing schema names
	private boolean isExistingSchemaName(String schemaName,Object parent){
		if(parent==null)return MongooseSchemaFactory.isExistingMongooseSchemaName(schemaName);
		if(parent instanceof MongooseSchema)return ((MongooseSchema)parent).containsASubSchemaCalled(schemaName);
		if(parent instanceof MongooseSchemaCollection)return((MongooseSchemaCollection)parent).containsASchemaCalled(schemaName);
		return false; // benefit of the doubt
	}
	private Object getSelectedObject(){
		Object selectedObject=null;
		try{selectedObject=selectedMongooseSchemaNode.getUserObject();}catch(Exception ex){} // this is correct because the top level root tree node does NOT have a user object that is a MongooseSchema instance
		return selectedObject;
	}
	// check for new schema names if either the entered names change or another schema is selected
	private void checkForNewSchemaNames(){
		boolean anyNewSchemaName=false;
		String newSchemaNamesText=newMongooseSchemaTextField.getText().trim();
		if(!newSchemaNamesText.isEmpty()){
			// if no mongoose schema is currently selected check against the schema's defined at the top level
			// the problem however is with the selected mongoose schema because sometimes the Schemas root is selected
			StringBuilder noNewSchemaNames=new StringBuilder();
			boolean aNewSchemaName;
			for(String newSchemaName:newSchemaNamesText.split(",")){
				if(newSchemaName.trim().isEmpty()) continue; // skip missing schema names!!
				if(newSchemaName.indexOf('.')<0){
					aNewSchemaName=!isExistingSchemaName(newSchemaName.trim(),getSelectedObject());
					if(aNewSchemaName) anyNewSchemaName=true;
					else noNewSchemaNames.append(", "+newSchemaName.trim());
				}else
					Utils.setInfo(this,"Schema name '"+newSchemaName+"' skipped, as it contains an invalid period character.");
			}
			if(noNewSchemaNames.length()>2){
				String noNewSchemaNamesText=noNewSchemaNames.substring(2);
				int lastCommaPos=noNewSchemaNamesText.lastIndexOf(", ");
				if(lastCommaPos>0)noNewSchemaNamesText=noNewSchemaNamesText.substring(0,lastCommaPos)+" and "+noNewSchemaNamesText.substring(lastCommaPos+2);
				Utils.setInfo(null,"WARNING: "+noNewSchemaNamesText+" already exist.");
			}
		}
		newMongooseSchemaButton.setEnabled(anyNewSchemaName);
		if(anyNewSchemaName)SwingUtils.setDefaultButton(newMongooseSchemaButton);else SwingUtils.unsetDefaultButton(newMongooseSchemaButton);
	}
	public String toString(){return "Office Vitae Mongoose Schema Designer";}
	private void setSelectedMongooseSchema(MongooseSchema mongooseSchema){
		mongooseSchemaEditorView.setMongooseSchema(mongooseSchema);
		if(mongooseSchema==null)return;
		Utils.setInfo(this,"Selected schema: '"+mongooseSchema.getRepresentation(false)+"'.");
		checkForNewSchemaNames();
		((CardLayout)mongooseSchemaTreeElementEditorView.getLayout()).show(mongooseSchemaTreeElementEditorView,"MongooseSchema");
	}
	private void setSelectedMongooseSchemaCollection(MongooseSchemaCollection mongooseSchemaCollection){
		mongooseSchemaCollectionEditorView.setMongooseSchemaCollection(mongooseSchemaCollection);
		saveMongooseSchemaCollectionButton.setEnabled(mongooseSchemaCollection!=null);
		if(mongooseSchemaCollection==null)return;
		Utils.setInfo(this,"Selected schema collection: '"+mongooseSchemaCollection.toString()+"'.");
		((CardLayout)mongooseSchemaTreeElementEditorView.getLayout()).show(mongooseSchemaTreeElementEditorView,"MongooseSchemaCollection");
	}
    private void setSelectedMongooseSchemaNode(DefaultMutableTreeNode mongooseSchemaNode){
		// when a node gets selected and thus a new mongoose schema possibly the entered new names need to be reevaluated and need to be ALL new
		this.selectedMongooseSchemaNode=(mongooseSchemaNode!=null?mongooseSchemaNode:schemasTreeRootNode);
		// if the selected mongoose schema node is NOT the root node, we select the associated mongoose schema
		Object selectedUserObject=this.selectedMongooseSchemaNode.getUserObject();
		// MDH@08NOV2018: now allowing editing the collection itself now (to start with editing of the schema options)
		// TODO: what if we have neither????
		setSelectedMongooseSchemaCollection(selectedUserObject instanceof MongooseSchemaCollection?(MongooseSchemaCollection)selectedUserObject:null);
		setSelectedMongooseSchema(selectedUserObject instanceof MongooseSchema?(MongooseSchema)selectedUserObject:null);
		/* replacing:
		if(!schemasTreeRootNode.equals(this.selectedMongooseSchemaNode))setSelectedMongooseSchema(((MongooseSchema)(this.selectedMongooseSchemaNode.getUserObject())));
		else checkForNewSchemaNames(); // IMPORTANT I have to do this because getSelectedMongooseSchema() will return null when this happens!!!
		*/
	}
    private JComponent getMongooseSchemaTreeView(){
        JPanel mongooseSchemaListPanel=SwingUtils.getTitledPanel(null);
        JLabel schemasLabel;
        mongooseSchemaListPanel.add(schemasLabel=new JLabel("Schemas"),BorderLayout.NORTH);schemasLabel.setFont(schemasLabel.getFont().deriveFont(Font.BOLD));
        ////////mongooseSchemaListPanel.add(SwingUtils.getLeftAlignedView(SwingUtils.getButton("Schemas",true,true, SwingUtils.NARROW_BORDER)),BorderLayout.NORTH);
        mongooseSchemasTreeScrollPane=new JScrollPane(mongooseSchemasTree=new JTree(new DefaultTreeModel(schemasTreeRootNode)));
        ////////mongooseSchemasTree.setUI(new CustomTreeUI());
        mongooseSchemaListPanel.add(mongooseSchemasTreeScrollPane);
        // getting rid of all the icons used for nodes
        DefaultTreeCellRenderer renderer=(DefaultTreeCellRenderer)mongooseSchemasTree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        mongooseSchemasTree.setCellRenderer(renderer);
        mongooseSchemasTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION); // at most one (path) selection
        mongooseSchemasTree.addTreeSelectionListener(new TreeSelectionListener(){
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                try{setSelectedMongooseSchemaNode((DefaultMutableTreeNode)mongooseSchemasTree.getLastSelectedPathComponent());}catch(Exception ex){}
            }
        });
        mongooseSchemaListPanel.add(getMongooseSchemaButtonView(),BorderLayout.SOUTH);
        return mongooseSchemaListPanel;
    }
    /* replacing:
    private JComponent getMongooseSchemaListView(){
        JPanel mongooseSchemaListPanel=SwingUtils.getTitledPanel(null);
        mongooseSchemaListPanel.add(SwingUtils.getLeftAlignedView(SwingUtils.getButton("Schemas",true,true, SwingUtils.NARROW_BORDER)),BorderLayout.NORTH);
        mongooseSchemaListPanel.add(new JScrollPane(mongooseSchemasList=new JList(new DefaultListModel<MongooseSchema>())));
        mongooseSchemasList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(e.getValueIsAdjusting())return;
                ///////if(e.getFirstIndex()!=e.getLastIndex())return;
                try{mongooseSchemaEditorView.setMongooseSchema((MongooseSchema)mongooseSchemasList.getSelectedValue());}catch(Exception ex){}
            }
        });
        mongooseSchemaListPanel.add(getNewMongooseSchemaView(),BorderLayout.SOUTH);
        return mongooseSchemaListPanel;
    }
    */
    private JComponent mongooseSchemaTreeElementEditorView;
    private JComponent getMongooseSchemaTreeElementEditorView(){
    	JPanel mongooseSchemaTreeElementEditorPanel=new JPanel(new CardLayout());
		mongooseSchemaTreeElementEditorPanel.add(new JLabel("Select either a Mongoose schema or Mongoose schema collection."),"None");
		mongooseSchemaTreeElementEditorPanel.add(getMongooseSchemaView(),"MongooseSchema");
		mongooseSchemaTreeElementEditorPanel.add(getMongooseSchemaCollectionView(),"MongooseSchemaCollection");
		return mongooseSchemaTreeElementEditorPanel;
	}
    private JComponent getMongooseSchemasView(){
        JPanel tablesPanel=new JPanel(new BorderLayout());
        JSplitPane splitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setLeftComponent(getMongooseSchemaTreeView());
        splitPane.setRightComponent(mongooseSchemaTreeElementEditorView=getMongooseSchemaTreeElementEditorView());
        tablesPanel.add(splitPane);
        splitPane.setDividerLocation(200);
        return tablesPanel;
    }
    private JComponent getView(){
        // I suppose we can have multiple table definitions as well
        JPanel panel=new JPanel(new BorderLayout());
        panel.add(getInfoView(),BorderLayout.NORTH);
        panel.add(getMongooseSchemasView());
        //////////panel.add(getButtonView(),BorderLayout.SOUTH);
        return panel;
    }

    // MDH@30OCT2018: let's associate directories in subfolder schemas with schema collections

	private TreePath readMongooseSchemas(DefaultMutableTreeNode mongooseSchemaCollectionTreeNode){
    	// 1. populate the tree node with the schema's in the folder associated with the Mongoose schema collection
		// NOTE the schema's themselves will take care of reading any subschema's
		TreePath lastSchemaTreePath=null;
		MongooseSchemaCollection mongooseSchemaCollection=((MongooseSchemaCollection)mongooseSchemaCollectionTreeNode.getUserObject());
		File mongooseSchemaCollectionFolder=mongooseSchemaCollection.getAssociatedFolder();
		File[] schemaFiles=mongooseSchemaCollectionFolder.listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir,String name){
				return name.endsWith(".msd")&&name.indexOf('.')==name.lastIndexOf('.'); // NOT a copy of a schema with only one period in it!!
			}
		});
		if(schemaFiles!=null&&schemaFiles.length>0){
			selectedMongooseSchemaNode=mongooseSchemaCollectionTreeNode; // getANewMongooseSchemaTreePath hangs its new node under the selectedMongooseSchemaNode!!!
			TreePath newSchemaTreePath;
			String schemaName;
			for(File schemaFile:schemaFiles)if(!schemaFile.isDirectory()){
				boolean canRead=false;
				try{
					if(schemaFile.canRead())
						canRead=true;
					else
						Utils.setInfo(null,"Cannot create the schema defined in '"+schemaFile.getAbsolutePath()+"': it is not readable.");
				}catch(Exception ex){
					Utils.setInfo(null,"ERROR: '"+ex.getLocalizedMessage()+"' checking the readability of '"+schemaFile.getAbsolutePath()+"'.");
				}
				if(canRead){
					schemaName=schemaFile.getName().substring(0,schemaFile.getName().indexOf('.'));
					Utils.setInfo(null,"Loading schema '"+schemaName+"' in collection '"+mongooseSchemaCollection.toString()+"'...");
					newSchemaTreePath=getANewMongooseSchemaTreePath(schemaName,null,mongooseSchemaCollection);
					if(newSchemaTreePath!=null)lastSchemaTreePath=newSchemaTreePath;
				}
			}
		}
		// 2. iterate over all subfolders and do the same
		File[] subfolders=mongooseSchemaCollectionFolder.listFiles(File::isDirectory);
		String subfolderName;
		Utils.consoleprintln("Number of subfolders: "+subfolders.length+".");
		for(File subfolder:subfolders){
			try{
				subfolderName=subfolder.getName();
				Utils.consoleprintln("Subfolder: '"+subfolderName+"' ("+subfolder.getCanonicalPath()+").");
				mongooseSchemaCollection=new MongooseSchemaCollection(subfolder.getName(),this); // the subfolder name
				DefaultMutableTreeNode subfolderTreeNode=new DefaultMutableTreeNode(mongooseSchemaCollection);
				mongooseSchemaCollectionTreeNode.add(subfolderTreeNode);
				readMongooseSchemas(subfolderTreeNode);
			}catch(Exception ex){}
		}
		return lastSchemaTreePath;
	}

    private PrintStream console=null;
    public MongooseSchemaDesignerFrame(){
        this.getContentPane().add(getView());
        Utils.setInfoViewer(this); // register myself as info viewer
        this.setTitle("Mongoose Schema Designer \u00A9 2018 Office Vitae");
        this.setSize(1280,1024);
        this.setLocationRelativeTo(null); // center on screen please
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // in this case we should save all unsaved tables immediately
                Vector<String> unsavedMongooseSchemaNames=new Vector<String>();
                MongooseSchema mongooseSchema=null;
                // iterate over all top-level nodes and save them
                int schemaIndex=schemasTreeRootNode.getChildCount();
                while(--schemaIndex>=0){
                    try{
                        mongooseSchema=((MongooseSchema)((DefaultMutableTreeNode)schemasTreeRootNode.getChildAt(schemaIndex)).getUserObject());
                        if(!mongooseSchema.isSynced())mongooseSchema.save();
                    }catch(Exception ex){
                        Utils.consoleprintln("ERROR: '"+ex.getLocalizedMessage()+"' saving"+(mongooseSchema!=null?" schema "+mongooseSchema.getName():"")+".");
                    }
                    if(mongooseSchema!=null&&!mongooseSchema.isSynced())unsavedMongooseSchemaNames.insertElementAt(mongooseSchema.getName(),0);
                    mongooseSchema=null;
                }
                // do NOT dispose when there are still tables unsaved (you can delete them to get rid of them before trying again!!)
                if(!unsavedMongooseSchemaNames.isEmpty()){
                    int dialogResult=JOptionPane.showConfirmDialog(null,"Failed to save schema(s) "+String.join(", ",unsavedMongooseSchemaNames.toArray(new String[]{}))+". Exit anyway?","Warning",JOptionPane.YES_NO_OPTION);
                    if(dialogResult==JOptionPane.NO_OPTION)return;
                }
                if(console!=null)System.setOut(console); // restore System.out redirection!!
                dispose();
            }
        });

		// if we do not have a console, let's redirect to a file
		if(System.console()==null){
			// redirect System.out to the log file
			System.out.println("Trying to redirect output to Office Vitae Mongoose Schema Designer.log.");
			try{
				PrintStream o=new PrintStream(new File("./OfficeVitaeMongooseSchemaDesigner.log"));
				// Store current System.out before assigning a new value
				Utils.setConsole(System.out);
				System.setOut(o);
			}catch(Exception ex){
				System.err.println("'"+ex.getLocalizedMessage()+"' redirecting System.out.");
			}
		}
		System.out.println("Start of a Office Vitae Mongoose Schema Designer session.");

		// read any (top-level) schema definitions, and remember the last created schema tree path for selection
		File jarPath=Utils.getPath();
		Utils.consoleprintln("Current path: "+jarPath);

		TreePath newSchemaTreePath,lastCreatedSchemaTreePath=null;
		// iterate over all subdirectories in the schemas subfolder
		File schemasFolder=((MongooseSchemaCollection)schemasTreeRootNode.getUserObject()).getAssociatedFolder();
		if(schemasFolder.exists()||schemasFolder.mkdirs())
			lastCreatedSchemaTreePath=readMongooseSchemas(schemasTreeRootNode);
		else
			Utils.setInfo(null,"ERROR: Failed to access schemas folder '"+schemasFolder.getAbsolutePath()+"'.");

		// external (JavaScript) model files we want to import automatically
		// TODO if there's NO sync listener an option collection should NOT be created, OR????
		MongooseSchemaCollection mongooseSchemaCollection=new MongooseSchemaCollection("app/models",null);
		File[] jsschemaFiles=new File("app/models").listFiles(new FilenameFilter(){
			@Override
			public boolean accept(File dir,String name){
				// any JavaScript file published by this app starts with ovmsd (short for Office Vitae Mongoose Schema Designer), and should NOT be included!!!!
				return !name.startsWith("ovmsd.")&&name.endsWith(".model.js");
			}
		});
		if(jsschemaFiles!=null&&jsschemaFiles.length>0){
			String jsschemaName;
			for(File jsschemaFile:jsschemaFiles)if(!jsschemaFile.isDirectory()){
				boolean canRead=false;
				try{
					if(jsschemaFile.canRead())
						canRead=true;
					else
						Utils.setInfo(null,"Cannot create the schema defined in '"+jsschemaFile.getAbsolutePath()+"': it is not readable.");
				}catch(Exception ex){
					Utils.setInfo(null,"ERROR: '"+ex.getLocalizedMessage()+"' checking the readability of '"+jsschemaFile.getAbsolutePath()+"'.");
				}
				if(canRead){
					jsschemaName=jsschemaFile.getName().substring(0,jsschemaFile.getName().indexOf('.'));
					newSchemaTreePath=getANewJavaScriptMongooseSchemaTreePath(jsschemaName,null,mongooseSchemaCollection);
					if(newSchemaTreePath!=null)lastCreatedSchemaTreePath=newSchemaTreePath;
				}
			}
		}
		if(lastCreatedSchemaTreePath!=null)selectTreePath(lastCreatedSchemaTreePath);

		// and ascertain to see all nodes (although perhaps we do not want to extend the nodes)
		SwingUtils.expandNode(mongooseSchemasTree,schemasTreeRootNode);

    }

    public static void main(String[] args) {
	// write your code here
        /////Display.setAppName("");
        ///System.setProperty("apple.laf.useScreenMenuBar", "true");
        ///System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Mongoose Schema Designer");
        System.out.println("Starting Mongoose Schema Designer...");
        new MongooseSchemaDesignerFrame().setVisible(true);
    }
}
