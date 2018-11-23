+_id:ObjectId
+active:Boolean	+default=true	required
+name:String	+indextype=unique	+minlength=1
+administrator_email:String	lowercase	trim	+minlength=3	+match=^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$
+startdate:Date	+minDate=Date.now
+enddate:Date	+minDate=Date.now
+starthms:String	+minlength=8	+maxlength=8	+match=^([0-1]?\d|2[0-3])(?::([0-5]?\d))?(?::([0-5]?\d))?$
+endhms:String	trim	+minlength=8	+maxlength=8	+match=^([0-1]?\d|2[0-3])(?::([0-5]?\d))?(?::([0-5]?\d))?$
