$client database - device: links a (rented) device to a project and map location
+_id:ObjectId	required	+indextype=unique
+project_id:String	+ref=projects
+maplocation_id:ObjectId	+ref=maplocations
-ovdevicerental_id:ObjectId	+indextype=unique	+ref=ovdevicerentals
+ovdevice_id:ObjectId	+indextype=unique	+ref=ovdevices
