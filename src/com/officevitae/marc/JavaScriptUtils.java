package com.officevitae.marc;

import jdk.nashorn.api.tree.*;

public class JavaScriptUtils{

	public static int getNumberOfJavaScriptSourceTrees(String JSname,String JStext){
		try {
			Parser parser = Parser.create();

			// NOT passing a diagnostic listener to print error messages, so if an error occurs it will be caught below...
			CompilationUnitTree cut = parser.parse(JSname,JStext,null); //////, (d) -> { System.out.println("ERROR: "+d); });

			if (cut != null) {
				System.out.println("AST tree kind: "+cut.getKind()+".");
				for(Tree sourceTree:cut.getSourceElements())System.out.println("\tSource tree: "+sourceTree.toString()+" of kind "+sourceTree.getKind());
				// call Tree.accept method passing a SimpleTreeVisitor
				cut.accept(new SimpleTreeVisitorES6<Void, Void>() {
					// visit method for 'with' statement
					public Void visitWith(WithTree wt,Void v) {
						// print warning on 'with' statement
						System.out.println("Warning: using 'with' statement!");
						return null;
					}
				}, null);
				return cut.getSourceElements().size();
			}
		}catch(Exception ex){
			System.out.println("ERROR: '"+ex.getMessage()+"'.");
		}
		return 0;
	}

}
