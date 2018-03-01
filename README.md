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
