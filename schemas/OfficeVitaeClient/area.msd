$client database - area: defines a site with a single GPS coordinate containing maps
gpscoordinate
	+_id:ObjectId	required	+indextype=unique
	+longitude:Number	+minNumber=-180	+maxNumber=180
	+latitude:Number	+minNumber=-180	+maxNumber=180
	+altitude:Number
+_id:ObjectId	required	+indextype=unique
+name:String	trim	+minlength=1
+timezone:String
+gpslocation:area_gpscoordinateSchema
