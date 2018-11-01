$client database - area: defines a site with a single GPS coordinate containing maps
+_id:ObjectId	required	+indextype=unique
+name:String	required	+indextype=unique
+label:String	required
+areatype_id:ObjectId	+ref=areatypes
+timezone:String
+latitude:Number
+longitude:Number
+altitude:Number
