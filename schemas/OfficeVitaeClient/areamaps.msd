+_id:ObjectId
+name:String	required
+area_id:ObjectId	$Id of area this map belongs to	+ref=areas
+image_url:String	$image file
+image_width:Mixed	$Width of image (pixels)
+image_height:Number	$Height of image (pixels)
+image_scale:Number	+default=1
