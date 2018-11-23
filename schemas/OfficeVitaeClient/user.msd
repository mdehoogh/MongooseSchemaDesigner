+_id:ObjectId
+user_id:ObjectId	$the unique login user id in the OfficeVitae database	required	-ref=user
+name:String	$The name of the user (as displayed in the front-end)
+mapoffice_id:ObjectId	$reference to the office of the user	+ref=mapoffice
