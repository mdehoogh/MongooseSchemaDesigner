# MongooseSchemaDesigner
Mongoose Schema Designer 1.0 - Copyright 2018-2019 Marc P. de Hoogh

In order to learn Mongoose I decided to create a (Java) application that would make it easy to design schemas, and can
create the schema, model, router and controller text files that can be used directly in a NodeJS project.

Note that I decided to pass the files dependent on both the Express web server (router) and the Mongoose instance,
so both need to be passed to the initial require('...controllers.js') as in require('./app/controllers/recipes.js')(app,mongoose).
The latter (mongoose) can be omitted as it is used as default.

Prerequisites
-------------
This Java (desktop) app requires a Java JDK or JRE to be pre-installed. 
I suppose Java v10.0.2 and above should work fine.

12 December 2019

P.S. An example app.js will be included shortly.
