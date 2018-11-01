$client database - areamap: defines a (floor) map (with image) containing offices and locations
+_id:ObjectId	required	+indextype=unique
+name:String	required
+area_id:ObjectId	+ref=area
+image_url:String
+image_width:Mixed
+image_height:Number
+image_scale:Number	+default=1
