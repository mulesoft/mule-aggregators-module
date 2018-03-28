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

NOTE: There is currently no Studio7 support for this module, so, when you include the dependency, the Aggregators module will appear in the Mule application's Mule Palette, but there will be no message processors nor any global elements to add to your Mule XML configuration files.
