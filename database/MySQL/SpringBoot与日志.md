# SpringBoot与日志

Spring Boot选用slf4j和logback作为默认的日志实现。

## slf4j(simple logging facade for Java)简介

slf4j为现存的流行日志框架提供了统一的门面，其默认的实现是logback.

> The Simple Logging Facade for Java (SLF4J) serves as a simple facade or abstraction for various logging frameworks (e.g. java.util.logging, logback, log4j) allowing the end user to plug in the desired logging framework at *deployment* time
>
> Logback's [`ch.qos.logback.classic.Logger`](http://logback.qos.ch/apidocs/ch/qos/logback/classic/Logger.html) class is a direct implementation of SLF4J's [`org.slf4j.Logger`](http://www.slf4j.org/apidocs/org/slf4j/Logger.html) interface. Thus, using SLF4J in conjunction with logback involves strictly zero memory and computational overhead.
>
> To switch logging frameworks, just replace slf4j bindings on your class path. For example, to switch from java.util.logging to log4j, just replace slf4j-jdk14-1.8.0-beta2.jar with slf4j-log4j12-1.8.0-beta2.jar.

![slf4j archi](C:\Users\Administrator\Desktop\concrete-bindings.png)

关于slf4j的使用，我们最熟悉的就是这样一行代码：

```java
Logger LOGGER = LoggerFactory.getLogger(SpringBootLoggingApplicationTests.class);
```

## Spring Boot集成slf4j

在使用Spring Boot开发我们的业务系统时，我们可以选用其默认的日志框架，但是在我们的工程中不可避免的要引用其他框架、组件等，这些第三方包势必包含了不同的日志框架，我们采用如下的方法来统一日志框架。

![bridging](C:\Users\Administrator\Desktop\legacy.png)

首先我们需要在pom文件中排除三方包中依赖的日志框架，例如在spring-boot-starter-web中使用的hibernate-validator，使用的是JBoss-logging，我们需要排除jboss-logging依赖：

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.jboss.logging</groupId>
                    <artifactId>jboss-logging</artifactId>
                </exclusion>
            </exclusions>
		</dependency>
```

排除之后三方包中缺少日志API，会引起编译和运行错误，此时我们需要引入slf4j为其他日志框架定制的替代包

```xml
        <dependency>
            <groupId>org.jboss.slf4j</groupId>
            <artifactId>slf4j-jboss-logging</artifactId>
            <version>1.1.0.Final</version>
        </dependency>
```

我们可以看到，这些替代包中的Logger类名和包名都与原日志框架相同，不同的是其实现都被转嫁到了slf4j的实现。通过这种偷梁换柱的操作，我们把三方jar包中使用的各种不同的日志框架都替换成了我们统一选用的日志框架。

通过在一个Spring Boot项目的pom文件我们也可以验证这一点：

![](C:\Users\Administrator\Desktop\捕获.PNG)

如果我们需要将默认的日志框架实现从默认的logback切换到log4j，也很简单，我们只需要排除logback的依赖，并加入slf4j到log4j的适配和log4j的实现包。

```xml
	<dependency>
	    <groupId>org.springframework.boot</groupId>
	    <artifactId>spring-boot-starter-web</artifactId>
        <exclusions>
            <exclusion>
                <artifactId>logback-classic</artifactId>
                <groupId>ch.qos.logback</groupId>
            </exclusion>
            <exclusion>
                <artifactId>log4j-to-slf4j</artifactId>
                <groupId>org.apache.logging.log4j</groupId>
            </exclusion>
       </exclusions>
  </dependency>
```

```xml
 <dependency>
     <groupId>org.slf4j</groupId>
     <artifactId>slf4j-log4j12</artifactId>
 </dependency>
```

spring-boot-starter-web默认依赖spring-boot-starter-logging，此外，官方还提供了spring-boot-starter-log4j2，其默认实现是log4j2，我们可以这样切换这两个logging-starter：

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-logging</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging2</artifactId>
        </dependency>
```

上述依赖使用的默认日志配置位于spring-boot-x.x.x.RELEASE.jar!/org/springframework/boot/logging/log4j2路径下的log4j2.xml和log4j2.xml两个文件中。我们可以在类路径下指定我们自定义的同名文件来实现定制化配置。与原始的log4j2不同的是，spring-boot对log4j.properties添加了额外的支持，可以通过在类路径下指定log4j2-spring.xml来使用spring-boot提供的增强功能。



