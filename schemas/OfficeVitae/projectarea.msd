+_id:ObjectId
+id:Int32	required	+indextype=unique
+projectid:Int32	required
+areaid:Int32	required
+client_id:ObjectId	+ref=Client
