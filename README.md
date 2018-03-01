# Mule Aggregators Module

From your GitHub home's mule-aggregators-module folder, build the module with 

```shell
mvn clean install
```

This will install the module to your local Maven repository, and also generate teh plugin file, tests, etc. 

You can also manually copy the compiled module JAR file from the`mule-aggretators-module/target/mule-aggregators-module-1.1.0-SNAPSHOT-mule-plugin.jar` to your Mule project's `target/repository/com/mulesoft/modules/mule-aggregators-module/1.1.0-SNAPSHOT` folder. Also create a classloader-model.json file in this folder

```json
{
  "version": "1.0.0",
  "artifactCoordinates": {
    "groupId": "org.mule.modules",
    "artifactId": "mule-aggregators-module",
    "version": "1.1.0-SNAPSHOT",
    "type": "jar",
    "classifier": "mule-plugin"
  },
  "dependencies": []
}
```


Add this dependency to your Mule application's pom.xml
```xml
		<dependency>
			<groupId>org.mule.modules</groupId>
			<artifactId>mule-aggregators-module</artifactId>
			<version>1.1.0-SNAPSHOT</version>
		</dependency>
```

NOTE: There is currently no Studio7 support for this module, so if you include the classifier element in the dependency:

```
Add this dependency to your Mule application's pom.xml
```xml
		<dependency>
			<groupId>org.mule.modules</groupId>
			<artifactId>mule-aggregators-module</artifactId>
			<version>1.1.0-SNAPSHOT</version>
			<classifier>mule-plugin</classifier>
		</dependency>
```
the Aggregators module will appear in Studio7 in the Mule application's Mule Palette, but there will be no message processors nor any global elements to add to your Mule XML configuration files. 
