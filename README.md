# Jaxb, Jakarta, and the Challenge of Transitive Dependencies

Author: Kevin Franklin

> [!NOTE]
> **TL;DR** When faced with the complexities of transative dependencies on older Java libraries such javax.xml.* failing when calling from Java 11+ runtimes, the likely optimal solution is to ***update the dependency itself***.

## Overview and Introduction

One of the often difficult to appreciate challenges in upgrading Java is the challenge the various libraries or dependencies your application may use. While you may have done the work of migrating the app itself from Java 8 to 21+, that doesn't resolve the fact the dependencies your application is using may not be ready for Java 21+. This can further lead to a situation in which one of the depedencies of your application is reliant upon a library no longer included in the Java ecosystem. A prime example of this, is the removeal of  the [JAXB (Java Architecture for XML Binding)](https://jcp.org/en/jsr/detail?id=222) and [JAX-WS (Java API for XML-Based Web Services)](https://jcp.org/en/jsr/detail?id=224) in Java 11+ as outlined in [JEP 320: Remove the Java EE and CORBA Modules](https://openjdk.org/jeps/320).

This repository will use a toy JAXB sample application written in Java 8 to go through the various migration and mitigation paths available in upgrading your dependency from Java 8 to 21.

## Prerequisites

For following along with these steps, you will need to have at least the following installed:

> [!TIP]
> Since I often have to switch between Java dististributions/versions, I've found (SDKMAN!)[https://sdkman.io/] a superb tool for managing this toil for me.

* Java 1.8
* Java 21+
* Apache Maven

## The Application as "Origionally" Written (`simple-xsd-app-java8`)

![The Application as "Origionally" Written](./images/simple-xsd-app-java8.png)

This is a toy JAXB application composed of a single file (`simple-xsd-app-java8/src/main/java/com/example/App.java`).  It relies on a XML Schema Definitions (XSD) avilable in the `com.northpolesouthern:example-endpoint-definition:1.0-SNAPSHOT` dependency. The application creates a toy "train" object from this dependency, marshalls out the content, and then unmarshalls the content. The diagram lists the `com.northpolesouthern:example-endpoint-definition:1.0-SNAPSHOT` dependency as coming from a Maven repository, in our case, the local one.

So let's test out the process of building and running the application. 

1.  Having pulled this repository down, switch to the `example-endpoint-definition`.
    ```bash
    cd example-endpoint-definition/
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration$ cd example-endpoint-definition/
    ```
2.  Double check you're using Java 1.8
    ```bash
    java -version
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration/example-endpoint-definition$ java -version
    openjdk version "1.8.0_492"
    OpenJDK Runtime Environment (build 1.8.0_492-b09)
    OpenJDK 64-Bit Server VM (build 25.492-b09, mixed mode)
    ```
3.  Now let's build the dependency
    ```bash
    mvn clean install
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration/example-endpoint-definition$ mvn clean install
    [INFO] Scanning for projects...
    [INFO] 
    [INFO] ---------< com.northpolesouthern:example-endpoint-definition >----------
    [INFO] Building example-endpoint-definition 1.0-SNAPSHOT
    [INFO]   from pom.xml
    [INFO] --------------------------------[ jar ]---------------------------------
    [INFO] 
    [INFO] --- clean:3.2.0:clean (default-clean) @ example-endpoint-definition ---
    [INFO] Deleting /home/kfrankli/jaxb-javax-jakarta-migration/example-endpoint-definition/target
    [INFO] 
    [INFO] --- jaxb2:2.5.0:xjc (xjc) @ example-endpoint-definition ---
    [WARNING] Using platform encoding [UTF-8], i.e. build is platform dependent!
    [INFO] Created EpisodePath [/home/kfrankli/jaxb-javax-jakarta-migration/example-endpoint-definition/target/generated-sources/jaxb/META-INF/JAXB]: true
    [INFO] Ignored given or default xjbSources [/home/kfrankli/jaxb-javax-jakarta-migration/example-endpoint-definition/src/main/xjb], since it is not an existent file or directory.
    [INFO] 
    [INFO] --- resources:3.3.1:resources (default-resources) @ example-endpoint-definition ---
    [WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
    [INFO] Copying 1 resource from src/main/resources to target/classes
    [INFO] Copying 1 resource from target/generated-sources/jaxb to target/classes
    [INFO] 
    [INFO] --- compiler:3.13.0:compile (default-compile) @ example-endpoint-definition ---
    [INFO] Recompiling the module because of changed source code.
    [WARNING] File encoding has not been set, using platform encoding UTF-8, i.e. build is platform dependent!
    [INFO] Compiling 3 source files with javac [debug target 1.8] to target/classes
    [INFO] 
    [INFO] --- resources:3.3.1:testResources (default-testResources) @ example-endpoint-definition ---
    [WARNING] Using platform encoding (UTF-8 actually) to copy filtered resources, i.e. build is platform dependent!
    [INFO] skip non existing resourceDirectory /home/kfrankli/jaxb-javax-jakarta-migration/example-endpoint-definition/src/test/resources
    [INFO] 
    [INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ example-endpoint-definition ---
    [INFO] No sources to compile
    [INFO] 
    [INFO] --- surefire:3.2.5:test (default-test) @ example-endpoint-definition ---
    [INFO] No tests to run.
    [INFO] 
    [INFO] --- jar:3.4.1:jar (default-jar) @ example-endpoint-definition ---
    [INFO] Building jar: /home/kfrankli/jaxb-javax-jakarta-migration/example-endpoint-definition/target/example-endpoint-definition-1.0-SNAPSHOT.jar
    [INFO] 
    [INFO] --- install:3.1.2:install (default-install) @ example-endpoint-definition ---
    [INFO] Installing /home/kfrankli/jaxb-javax-jakarta-migration/example-endpoint-definition/pom.xml to /home/kfrankli/.m2/repository/com/northpolesouthern/example-endpoint-definition/1.0-SNAPSHOT/example-endpoint-definition-1.0-SNAPSHOT.pom
    [INFO] Installing /home/kfrankli/jaxb-javax-jakarta-migration/example-endpoint-definition/target/example-endpoint-definition-1.0-SNAPSHOT.jar to /home/kfrankli/.m2/repository/com/northpolesouthern/example-endpoint-definition/1.0-SNAPSHOT/example-endpoint-definition-1.0-SNAPSHOT.jar
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time:  1.332 s
    [INFO] Finished at: 2026-06-29T10:05:31-04:00
    [INFO] ------------------------------------------------------------------------
    ```
4.  Switch to our toy apps directory
    ```bash
    cd ../simple-xsd-app-java8/
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration/example-endpoint-definition$ cd ../simple-xsd-app-java8/
    ```
5.  Now let's build your toy application
    ```bash
    mvn clean install exec:java
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration/simple-xsd-app-java8$ mvn clean install exec:java
    [INFO] Scanning for projects...
    Downloading from central: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-metadata.xml
    Downloading from central: https://repo.maven.apache.org/maven2/org/codehaus/mojo/maven-metadata.xml
    Downloaded from central: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-metadata.xml (14 kB at 61 kB/s)
    Downloaded from central: https://repo.maven.apache.org/maven2/org/codehaus/mojo/maven-metadata.xml (21 kB at 79 kB/s)
    [INFO] 
    [INFO] ---------------------< com.example:simple-xsd-app >---------------------
    [INFO] Building simple-xsd-app 1.0-SNAPSHOT
    [INFO]   from pom.xml
    [INFO] --------------------------------[ jar ]---------------------------------
    [INFO] 
    [INFO] --- clean:3.2.0:clean (default-clean) @ simple-xsd-app ---
    [INFO] Deleting /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-java8/target
    [INFO] 
    [INFO] --- resources:3.3.1:resources (default-resources) @ simple-xsd-app ---
    [INFO] skip non existing resourceDirectory /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-java8/src/main/resources
    [INFO] 
    [INFO] --- compiler:3.13.0:compile (default-compile) @ simple-xsd-app ---
    [INFO] Recompiling the module because of changed source code.
    [INFO] Compiling 1 source file with javac [debug target 1.8] to target/classes
    [INFO] 
    [INFO] --- resources:3.3.1:testResources (default-testResources) @ simple-xsd-app ---
    [INFO] skip non existing resourceDirectory /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-java8/src/test/resources
    [INFO] 
    [INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ simple-xsd-app ---
    [INFO] No sources to compile
    [INFO] 
    [INFO] --- surefire:3.2.5:test (default-test) @ simple-xsd-app ---
    [INFO] No tests to run.
    [INFO] 
    [INFO] --- jar:3.4.1:jar (default-jar) @ simple-xsd-app ---
    [INFO] Building jar: /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-java8/target/simple-xsd-app-1.0-SNAPSHOT.jar
    [INFO] 
    [INFO] --- install:3.1.2:install (default-install) @ simple-xsd-app ---
    [INFO] Installing /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-java8/pom.xml to /home/kfrankli/.m2/repository/com/example/simple-xsd-app/1.0-SNAPSHOT/simple-xsd-app-1.0-SNAPSHOT.pom
    [INFO] Installing /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-java8/target/simple-xsd-app-1.0-SNAPSHOT.jar to /home/kfrankli/.m2/repository/com/example/simple-xsd-app/1.0-SNAPSHOT/simple-xsd-app-1.0-SNAPSHOT.jar
    [INFO] 
    [INFO] --- exec:3.1.0:java (default-cli) @ simple-xsd-app ---
    --- Marshalling (Java to XML) ---
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Train xmlns="http://northpolesouthern.com">
        <id>1045</id>
        <origin>Chicago</origin>
        <destination>Seattle</destination>
        <axles>44</axles>
    </Train>

    --- Unmarshalling (XML to Java) ---
    Successfully parsed XML back into Java:
    Train ID : 1045
    Route    : Chicago -> Seattle
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time:  1.602 s
    [INFO] Finished at: 2026-06-29T10:09:55-04:00
    [INFO] ------------------------------------------------------------------------
    ```
    As we can see in the output above, our application created a `com.northpolesouthern.TrainType` object using a `com.northpolesouthern.ObjectFactory`, added data to the object, marshalled the object to a XML object it then wrote to a string `xmlOutput`, the unmarshalled the data it from `xmlOutput`.
4.  Switch to back to the root of the proejct
    ```bash
    cd ..
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration/simple-xsd-app-java8$ cd ..
    ```

## "Naïve" Migration Attempt

!["Naïve" Migration Attempt](./images/simple-xsd-app-naive-migrate.png)

Having demonstrated how the app origionally worked, we're going to attempt to naïvely migrate the application to Java 21. We're going to ignore our dependency for now and see what happens.

1.  From the root of the project let's switch to `simple-xsd-app-naive-migrate`
    ```bash
    cd simple-xsd-app-naive-migrate
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration$ cd simple-xsd-app-naive-migrate/
    ```
2.  Lets first examine our application itself.
    ```bash
    cat src/main/java/com/example/App.java 
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration/simple-xsd-app-naive-migrate$ cat src/main/java/com/example/App.java
    package com.example;

    // Swap javax to jakarta
    //import javax.xml.bind.JAXBContext;
    //import javax.xml.bind.JAXBElement;
    //import javax.xml.bind.JAXBException;
    //import javax.xml.bind.Marshaller;
    //import javax.xml.bind.Unmarshaller;
    //import javax.xml.transform.stream.StreamSource;

    import jakarta.xml.bind.JAXBContext;
    import jakarta.xml.bind.JAXBElement;
    import jakarta.xml.bind.JAXBException;
    import jakarta.xml.bind.Marshaller;
    import jakarta.xml.bind.Unmarshaller;
    import javax.xml.transform.stream.StreamSource;
    ...
    ```
    We can see that we've made some *trivial* changes. Namely changing our imports from `javax.xml` to `jakarta.xml`. Otherwise it remains the same. The need to shift to Jakarta is due to the renaming of the formerly Java EE ecosystem to Jakarta EE. When Oracle donated the Java EE codebase to the Eclipse Foundation on September 12, 2017, they also [retained ownership of the "Java" trademark](https://www.infoq.com/news/2018/02/from-javaee-to-jakartaee/), necessitating a renaming. 
3.  Since JAXB was removed from core Java SE with [JEP 320](https://openjdk.org/jeps/320). We will have to add it as a dependency to our `pom.xml`. We also change the compiler source and target to 21.
    ```bash
    cat pom.xml
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration/simple-xsd-app-naive-migrate$ cat pom.xml
    ...
        <properties>
            <maven.compiler.source>21</maven.compiler.source>
            <maven.compiler.target>21</maven.compiler.target>
            <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        </properties>

        <dependencies>
            <dependency>
                <groupId>com.northpolesouthern</groupId>
                <artifactId>example-endpoint-definition</artifactId>
                <version>1.0-SNAPSHOT</version>
            </dependency>
            <dependency>
                <groupId>jakarta.xml.bind</groupId>
                <artifactId>jakarta.xml.bind-api</artifactId>
                <version>4.0.0</version>
            </dependency>
            <dependency>
                <groupId>org.glassfish.jaxb</groupId>
                <artifactId>jaxb-runtime</artifactId>
                <version>4.0.3</version>
                <scope>runtime</scope>
            </dependency>
        </dependencies>
    ...
    ```
4.  Now change your Java runtime to Java 21 and double check it.
    > [!TIP]
    > This is where (SDKMAN!)[https://sdkman.io/] comes in clutch and I quick switch to Java 21 via `$ sdk use java 21.0.2-open`

    ```bash
    $ java -version
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration/simple-xsd-app-naive-migrate$ java -version
    openjdk version "21.0.2" 2024-01-16
    OpenJDK Runtime Environment (build 21.0.2+13-58)
    OpenJDK 64-Bit Server VM (build 21.0.2+13-58, mixed mode, sharing)
    ```
5.  Now let us attempt to build and run this upgraded application.
    ```bash
     mvn clean install exec:java
    ```
    Example output:
    ```bash
    kfrankli@kfrankli-thinkpadp1gen3:~/jaxb-javax-jakarta-migration/simple-xsd-app-naive-migrate$  mvn clean install exec:java
    [INFO] Scanning for projects...
    [INFO] 
    [INFO] ---------------------< com.example:simple-xsd-app >---------------------
    [INFO] Building simple-xsd-app 1.0-SNAPSHOT
    [INFO]   from pom.xml
    [INFO] --------------------------------[ jar ]---------------------------------
    [INFO] 
    [INFO] --- clean:3.2.0:clean (default-clean) @ simple-xsd-app ---
    [INFO] Deleting /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-naive-migrate/target
    [INFO] 
    [INFO] --- resources:3.3.1:resources (default-resources) @ simple-xsd-app ---
    [INFO] skip non existing resourceDirectory /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-naive-migrate/src/main/resources
    [INFO] 
    [INFO] --- compiler:3.13.0:compile (default-compile) @ simple-xsd-app ---
    [INFO] Recompiling the module because of changed source code.
    [INFO] Compiling 1 source file with javac [debug target 21] to target/classes
    [INFO] -------------------------------------------------------------
    [WARNING] COMPILATION WARNING : 
    [INFO] -------------------------------------------------------------
    [WARNING] /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-naive-migrate/src/main/java/com/example/App.java: unknown enum constant javax.xml.bind.annotation.XmlAccessType.FIELD
    reason: class file for javax.xml.bind.annotation.XmlAccessType not found
    [INFO] 1 warning
    [INFO] -------------------------------------------------------------
    [INFO] -------------------------------------------------------------
    [ERROR] COMPILATION ERROR : 
    [INFO] -------------------------------------------------------------
    [ERROR] /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-naive-migrate/src/main/java/com/example/App.java:[36,92] cannot access javax.xml.bind.JAXBElement
    class file for javax.xml.bind.JAXBElement not found
    [INFO] 1 error
    [INFO] -------------------------------------------------------------
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD FAILURE
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time:  0.760 s
    [INFO] Finished at: 2026-06-29T10:39:34-04:00
    [INFO] ------------------------------------------------------------------------
    [ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project simple-xsd-app: Compilation failure
    [ERROR] /home/kfrankli/jaxb-javax-jakarta-migration/simple-xsd-app-naive-migrate/src/main/java/com/example/App.java:[36,92] cannot access javax.xml.bind.JAXBElement
    [ERROR]   class file for javax.xml.bind.JAXBElement not found
    [ERROR] 
    [ERROR] -> [Help 1]
    [ERROR] 
    [ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
    [ERROR] Re-run Maven using the -X switch to enable full debug logging.
    [ERROR] 
    [ERROR] For more information about the errors and possible solutions, please read the following articles:
    [ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
    ```
    Well that didn't work. So what happend?

    When we updated our application to use Java 21 and Jakarta, we were unawares that our depedency on `com.northpolesouthern:example-endpoint-definition:1.-SNAPSHOT` was in itself depedent on `javax.xml`, introducing a [transative dependency for our application](https://en.wikipedia.org/wiki/Transitive_dependency). So next we will see several ways of how to deal with this.

## Updating the Depedency Library (Strongly Recommended)

![Updating the Depedency Library](./images/simple-xsd-app-update-lib.png)

The ideal, preferable, and arguably correct choice is for us to update the endpoint definition project, `com.northpolesouthern:example-endpoint-definition:1.-SNAPSHOT`, that is our dependency. If we have the circumstance where this dependency needs to support both legacy Java 1.8 apps and Java 21 applications, we will need to publish two variants of this depedency. This can be done in several ways, note this list isn't exhaustive. 

This is recommended because owning the XSDs means you can solve namespace collision at the root rather than patching it downstream, and pushing significant technical debt to the consumers of the library.

### JEP14: Tip & Tail Model

OpenJDP recommends adopting [JEP 14: The Tip & Tail Model of Library Development](https://openjdk.org/jeps/14). It was written specifically to address the type of ecosystem fracture this repository is dealing with. It formally urges the Java ecosystem to abandon the "one-size-fits-all" release model in favor of splitting release trains.

Instead of compiling Java 1.8 code and trying to dynamically rewrite it for Java 21, you maintain two distinct release trains in your Git repository:

* The Tip (e.g., Version 2.0.0+): This branch moves entirely to Java 21 and the `jakarta.xml.*` namespace. All new XSD schemas, feature enhancements, and active development happen strictly here. Your Quarkus applications consume the Tip.
* The Tail (e.g., Version 1.0.x): This branch remains locked to Java 8 and `javax.xml.*`. It is placed in strict maintenance mode. It receives only critical bug fixes and security patches. Your legacy applications consume the Tail.  

### Build Once, Puiblish Twice

Since Java 8 and `javax.xml.*` represent the lowest common denominator, you keep the library's source code exactly as it is. Instead of duplicating the repository or managing parallel branches, you modify the DevSecOps pipeline to generate a second, modernized JAR during the Maven build phase. This work would best resemble the work outline in the `simple-xsd-app-eclipse-transformer` but applied to the `example-endpoint-defition`.

### A Note of Parasing Between Differing Versions of Java

The raison d'être for the cration of JAXB and JAX-WS was to allow the ability to specify a implimentation independent means of exporting objects through marshalling to XML and then consuming the XML through unmarshalling. E.g. our `com.northpolesouthern.TrainType` is marshalled into the following XML object:

```xml
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Train xmlns="http://northpolesouthern.com">
        <id>1045</id>
        <origin>Chicago</origin>
        <destination>Seattle</destination>
        <axles>44</axles>
    </Train>
```

In fact part of the reason for this is to allow non-java producers and consumters to communicate with Java applications through REST or SOAP by using XML as defined by the XSD.

There are some caveats to be noted, if the XSD is poorly formed you may run in to behavioral differences due to flaws in the XSD specfication. The `javax.xml.validation` package is simply an API wrapper. Under the hood, the JDK uses an internal fork of the [Apache Xerces parser](https://xerces.apache.org/).

Between Java 8 and Java 21, that "internal" parser received years of bug fixes, performance tweaks, and specification compliance corrections. If the legacy Java 8 clients have inadvertently relied on a parsing bug, a lenient edge-case, or an unspecified behavior in the older Xerces engine, that behavior might be "fixed" (and thus broken for the specific use case) in Java 21.

## Using the Eclipse Transformer Plugin

![simple-xsd-app-eclipse-transformer](./images/simple-xsd-app-eclipse-transformer.png)


## Generating the XSD Objects

![simple-xsd-app-regenerate-xsd](./images/simple-xsd-app-regenerate-xsd.png)

## Running Both Javax.xml and Jakarta.xml in Parallel ("Bridge" Architecture)

![simple-xsd-app-bridge-arch](./images/simple-xsd-app-bridge-arch.png)

Because Java 21 completely removed the native JAXB implementation, you can manually force both the legacy javax and modern jakarta standalone runtimes to coexist in your classpath. The application code will use Jakarta, while the legacy code continues using Javax.




## Conclusions

Dealing with transitive dependencies due to the changes from `javax` to `jakarta` that crop up in mirating from Java 8 to 21 can be thorny. But for all the aofrementioned reasons, ideally [updating the libary depdency itself](#updating-the-depedency-library-strongly-recommended).


# TODO below



1.  Lorem Ipsum
    ```bash
    
    ```
    Example output:
    ```bash
    
    ```
2.  Lorem Ipsum
    ```bash
    
    ```
    Example output:
    ```bash
    
    ```
3.  Lorem Ipsum
    ```bash
    
    ```
    Example output:
    ```bash
    
    ```
4.  Lorem Ipsum
    ```bash
    
    ```
    Example output:
    ```bash
    
    ```
5.  Lorem Ipsum
    ```bash
    
    ```
    Example output:
    ```bash
    
    ```
6.  Lorem Ipsum
    ```bash
    
    ```
    Example output:
    ```bash
    
    ```
7.  Lorem Ipsum
    ```bash
    
    ```
    Example output:
    ```bash
    
    ```












# simple-xsd-app-naive-migrate

Update pom.xml to use Java 21 `maven.compiler.source` and `maven.compiler.target`

Add a dependency for jakarta.xml.bind since as of java 11, it's no longer included. Also add a runtime implementation (this case glassfish) since nolonger stock from jvm

Update App.java to use javarta.xml rather than javax.xml


Because the upstream xsd is


# For simple-xsd-app-eclise-transformer

We need to *remove* the direct dependency

Add maven-dependency-plugin to unpack and then transform the classes to Jakarta
