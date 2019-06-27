### Spring boot在web环境下的静态资源路径映射规划

Spring boot自动配置web mvc的代码位置：

spring-boot-autoconfigure-x.x.x.jar:

org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter#addResourceHandlers

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
	if (!this.resourceProperties.isAddMappings()) {
		logger.debug("Default resource handling disabled");
		return;
	}
	Duration cachePeriod = this.resourceProperties.getCache().getPeriod();
	CacheControl cacheControl = this.resourceProperties.getCache().getCachecontrol().toHttpCacheControl();
	if (!registry.hasMappingForPattern("/webjars/**")) {
		customizeResourceHandlerRegistration(registry.addResourceHandler("/webjars/**")
				.addResourceLocations("classpath:/META-INF/resources/webjars/")
				.setCachePeriod(getSeconds(cachePeriod)).setCacheControl(cacheControl));
	}
	String staticPathPattern = this.mvcProperties.getStaticPathPattern();
	if (!registry.hasMappingForPattern(staticPathPattern)) {
		customizeResourceHandlerRegistration(registry.addResourceHandler(staticPathPattern)
				.addResourceLocations(getResourceLocations(this.resourceProperties.getStaticLocations()))
				.setCachePeriod(getSeconds(cachePeriod)).setCacheControl(cacheControl));
	}
}
```

这段代码重点配置了两个规则：

1.`/webjars/**`映射：spring boot提供了将js框架或者类库打成jar包，像引入java三方包一样的引入js类库。对应的静态资源路径为`classpath:/META-INF/resources/webjars/`。

例如我们通过webjars的方式导入jquery，需要在pom文件中引入：

```xml
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>jquery</artifactId>
    <version>3.4.0</version>
</dependency>
```

（webjars可以在https://www.webjars.org/查找）

这个jar包的目录结构是这样的：

![jquery-webjars](https://github.com/ZhangLaibao/machine_gun/blob/master/images/jquery-webjars.png)

根据代码和jar包目录结构我们可以知道，在前端页面中我们可以通过http://ip:port/context-path/webjars/jquery/3.4.0/jquery.js请求到这一资源。

2.`/**`映射：会在如下四个路径下寻找对应资源，我们的js/css/images都可以在这些目录下规划路径。

```
"classpath:/META-INF/resources/",
"classpath:/resources/",
"classpath:/static/", 
"classpath:/public/"
```

#### 相关点

在WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter中还有其他一些和web相关的配置：

欢迎页面配置：

```java
@Bean
public WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext) {
   return new WelcomePageHandlerMapping(new TemplateAvailabilityProviders(applicationContext),
         applicationContext, getWelcomePage(), this.mvcProperties.getStaticPathPattern());
}
```

页面小图标配置：

```java
   @Configuration
   @ConditionalOnProperty(value = "spring.mvc.favicon.enabled", matchIfMissing = true)
   public static class FaviconConfiguration implements ResourceLoaderAware {

      private final ResourceProperties resourceProperties;

      private ResourceLoader resourceLoader;

      public FaviconConfiguration(ResourceProperties resourceProperties) {
         this.resourceProperties = resourceProperties;
      }

      @Override
      public void setResourceLoader(ResourceLoader resourceLoader) {
         this.resourceLoader = resourceLoader;
      }

      @Bean
      public SimpleUrlHandlerMapping faviconHandlerMapping() {
         SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
         mapping.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
         mapping.setUrlMap(Collections.singletonMap("**/favicon.ico", faviconRequestHandler()));
         return mapping;
      }

      @Bean
      public ResourceHttpRequestHandler faviconRequestHandler() {
         ResourceHttpRequestHandler requestHandler = new ResourceHttpRequestHandler();
         requestHandler.setLocations(resolveFaviconLocations());
         return requestHandler;
      }

      private List<Resource> resolveFaviconLocations() {
         String[] staticLocations = getResourceLocations(this.resourceProperties.getStaticLocations());
         List<Resource> locations = new ArrayList<>(staticLocations.length + 1);
         Arrays.stream(staticLocations).map(this.resourceLoader::getResource).forEach(locations::add);
         locations.add(new ClassPathResource("/"));
         return Collections.unmodifiableList(locations);
      }
   }
}
```

分析代码我们可以大致得出结论，spring boot会去静态资源根目录，也就是上述第二条规则中`/**`映射到的静态资源路径下找欢迎页和小图标资源文件。所以我们在项目中可以这样组织：
![welcome-and-icons](https://github.com/ZhangLaibao/machine_gun/blob/master/images/welcome-and-icons.png)

此时如果直接访问http://localhost:8080 效果是这样的：
![show-off](https://github.com/ZhangLaibao/machine_gun/blob/master/images/show-off.png)

