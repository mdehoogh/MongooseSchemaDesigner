$client database - userinput: stores the answers given by users to questions in questionnaires
+_id:ObjectId	required	+indextype=unique
+maplocation_id:ObjectId	+ref=maplocations
+user_id:ObjectId	+ref=users
+inputdata:Mixed
+project_id:ObjectId	+ref=projects
