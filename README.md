# Mule Aggregators Module

From your GitHub home's mule-aggregators-module folder, build the module with 

```shell
mvn clean install
```

This will install the module to your local Maven repository, and also generate the plugin file, tests, etc.


Add this dependency to your Mule application's pom.xml
```xml
		<dependency>
			<groupId>org.mule.modules</groupId>
			<artifactId>mule-aggregators-module</artifactId>
			<version>1.1.0-SNAPSHOT</version>
			<classifier>mule-plugin</classifier>
		</dependency>
```
