$client database - questionnaire: defines a set of questions associated with a project
+_id:ObjectId	required	+indextype=unique
+name:String	required
+start_date:Date	+default=Date.now
+end_date:Date
+project_id:ObjectId	+ref=project
