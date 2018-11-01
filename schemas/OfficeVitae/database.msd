$Office Vitae database - database: defines a (client) database by name
+_id:ObjectId	required	+indextype=unique
+name:String	trim	+minlength=1
+dbconnection_id:ObjectId	required	+ref=dbconnection
