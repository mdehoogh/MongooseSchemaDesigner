$the S/N of the product instance
+_id:ObjectId
+manfacturer_id:ObjectId	required	+ref=manufacturer
+supplier_id:ObjectId	+ref=supplier
+supplier_date:Date
+model:String
+serialnumber:Mixed
