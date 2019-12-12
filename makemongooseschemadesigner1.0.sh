#!/bin/bash
cwd=$(pwd)
# assume this to be the root folder of the IntelliJ project
cd "./out/production/MongooseSchemaDesigner"
jar cfm "${cwd}/MongooseSchemaDesigner.jar" "${cwd}/mongooseschemadesignermanifest.txt" *
cd "$cwd"
rm -f MongooseSchemaDesigner.log
# how about packing the library jars with the generated jar (which should be in this folder)
# include all design files as well
zip -r MongooseSchemaDesigner1.0.zip MongooseSchemaDesigner.jar libs/rhino-1.7.9.jar schemas
echo "To distribute Mongoose Schema Designer 1.0 send the MongooseSchemaDesigner1.0.zip file."
echo "A version of the Java JDK or JRE should be installed!"
echo "Double-click MongooseSchemaDesigner.jar to run the Mongoose Schema Designer application,"
echo "or execute `java -jar MongooseSchemaDesigner.jar` from the command line."
