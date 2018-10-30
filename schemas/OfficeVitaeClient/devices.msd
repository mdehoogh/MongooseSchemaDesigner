$where the devices is located
+_id:ObjectId
+project_id:String	+ref=projects
+maplocation_id:ObjectId	+ref=maplocations
-ovdevicerental_id:ObjectId	$The id of the rental information	+indextype=unique	+ref=ovdevicerentals
+ovdevice_id:ObjectId	$The id of the device in the OfficeVitae database	+indextype=unique	+ref=ovdevices
