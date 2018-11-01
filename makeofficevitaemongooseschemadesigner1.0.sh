#!/bin/bash
cwd=$(pwd)
# assume this to be the root folder of the IntelliJ project
cd "./out/production/MongooseSchemaDesigner"
jar cfm "${cwd}/OfficeVitaeMongooseSchemaDesigner.jar" "${cwd}/officevitaemongooseschemadesignermanifest.txt" *
cd "$cwd"
rm -f OfficeVitaeMongooseSchemaDesigner.log
# how about packing the library jars with the generated jar (which should be in this folder)
# include all design files as well
zip -r OfficeVitaeMongooseSchemaDesigner1.0.zip OfficevitaeMongooseSchemaDesigner.jar libs/rhino-1.7.9.jar schemas
echo "To distribute Office Vitae Mongoose Schema Designer 1.0 send the OfficeVitaeMongooseSchemaDesigner1.0.zip file."
echo "Double-click OfficeVitaMongooseSchemaDesigner.jar to run the Office Vitae Mongoose Schema Designer application,"
echo "or execute java -jar OfficeVitaeMongooseSchemaDesigner.jar on the command line."
