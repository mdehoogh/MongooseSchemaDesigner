$Name of area
+_id:ObjectId
+name:String	required	+indextype=unique
+label:String	required
+areatype_id:ObjectId	+ref=areatypes
+timezone:String
+latitude:Number
+longitude:Number
+altitude:Number
