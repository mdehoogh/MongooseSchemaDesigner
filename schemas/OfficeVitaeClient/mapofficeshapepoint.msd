$client database - mapofficeshapepoints: defines the polygon of an office on its map
+_id:ObjectId	required	+indextype=unique
+mapoffice_id:ObjectId	required	+ref=mapoffices
+point_index:Long
+areamap_x:Number	required
+areamap_y:Number	required
