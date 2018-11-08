$client database - questionnaire: defines a set of questions associated with a project
+_id:ObjectId
+name:String	required
+start_date:Date	+default=Date.now
+end_date:Date
+project_id:ObjectId	+ref=project
+question_ids:[ObjectId]	$list of question ids (as defined in OfficeVitae database) in the order that the user should answer them	+validate=return(v.length>0);
