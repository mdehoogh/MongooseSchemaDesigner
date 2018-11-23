+_id:ObjectId
+id:Int32	required	+indextype=unique
+timestamp:Date	required
+areaid:Int32
+longitude:Number	required
+latitude:Number	required
+dt:Int32
+stationid:Int32
+sunset:Int32
+sunrise:Int32
+temp:Number
+humidity:Int32
+pressure:Int32
+temp_min:Number
+temp_max:Number
+wind_speed:Number
+wind_deg:Number
+weatherid:Int32
+weather_main:String	+maxlength=16
+weather_description:String	+maxlength=32
+weather_icon:String	+maxlength=8
+clouds_all:Int32
