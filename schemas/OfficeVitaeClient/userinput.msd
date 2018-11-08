$client database - userinput: stores the answers given by users to questions in questionnaires
+_id:ObjectId
+maplocation_id:ObjectId	+ref=maplocation
+user_id:ObjectId	+ref=user
+inputdata:Mixed
+project_id:ObjectId	+ref=projects
