+_id:ObjectId
+id:Int32	required	+indextype=unique
+areamapid:Int32
+name:String	+indextype=unique	trim	+minlength=1
+maplocation_x:Number
+maplocation_y:Number
+mapofficeid:Int32
+abbrev:String
+client_id:ObjectId	+ref=Client
