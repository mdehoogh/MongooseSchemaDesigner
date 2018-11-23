+_id:ObjectId
+id:Int32	required	+indextype=unique
+name:String
+timezone:String
+userid:Int32
+includeuserhostdeviceid:Boolean
+client_id:ObjectId	+ref=Client
