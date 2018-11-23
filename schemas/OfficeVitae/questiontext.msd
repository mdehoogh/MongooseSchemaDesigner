$if a multiple choice question the possible answers to the question for the user to select from
+_id:ObjectId
+question_id:ObjectId	+ref=question
+language:String
+text:String
+answers:Array	+validate=return(v.length!=1);
