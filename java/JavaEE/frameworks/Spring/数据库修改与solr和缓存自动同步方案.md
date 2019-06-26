# 数据库修改与solr和缓存自动同步方案

### 1.场景描述

假设我们有一个商品模型，商品的搜索使用搜索引擎技术solr实现，商品的详情使用了redis缓存，商品的数据操作层使用了MyBatis框架。我们需要自动将对商品模型数据的修改自动并且异步的同步到solr和redis。

### 2.技术方案

1.使用MyBatis提供的Interceptor机制监控数据修改，发布事件。

2.使用Spring的ApplicationContext发布和监听事件。

3.使用@Cacheable注解简化缓存逻辑

### 3.技术细节

#### 1.MyBatis Interceptors



#### 2.ApplicationContext.publishEvent()

#### 3.@Cacheable

### 4.实现代码

1.开发Interceptor，并发布事件

```java
@Intercepts({
    @Signature(
            type = Executor.class, method = "update", 
            args = {MappedStatement.class, Object.class})
})
public class PictureSolrSyncInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object o = invocation.proceed();
        publishEvent(invocation);
        return o;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) { }
    
    private void publishEven(Invocation invocation) {
        Object entity = invocation.getArgs()[1];
        if (entity instanceof Pic) {
            Long id = ((Pic) entity).getId();
            ApplicationContextHolder.getApplicationContext()
                .publishEvent(new PicUpdateEvent(this, id));
        }
    }
}
```

2.配置Interceptor

MyBatis-config.xml

```xml
<plugins>
    <plugin interceptor="com.ipr.bantu.biz.pic.interceptors.PictureSolrSyncInterceptor"/>
</plugins>
```

3.Event定义：

```java
@Getter @Setter
public class PicUpdateEvent extends ApplicationEvent {

    private Long id;

    public PicUpdateEvent(Object source, Long id) {
        super(source);
        this.id = id;
    }
}
```

4.Event监听：

```java
@Component
public class PicUpdateEventListener {

    @Resource
    private PictureSyncBiz pictureSyncBiz;

    @Resource
    private PictureCacheBiz pictureCacheBiz;

    @EventListener
    public void onEvent(PicUpdateEvent event) {
        Long id = event.getId();
        pictureSyncBiz.asyncSyncById(id);
        pictureCacheBiz.clearById(id);
    }
}
```

5.solr同步逻辑 - 略

6.缓存同步逻辑

```java
@Component
public class PictureCacheBiz {

    private static final String CACHE_NAME = "";

    @Resource
    private PictureDataService pictureDataService;

    @CacheEvict(cacheNames = CACHE_NAME, key = "'pic:detail:' + #id")
    public void clearById(Long id) { }

    // condition=false时，不读取缓存，直接执行方法体，并返回结果，同时返回结果也不放入缓存。
    // condition=true时，读取缓存，有缓存则直接返回。无则执行方法体，同时返回结果放入缓存。
    @Cacheable(cacheNames = CACHE_NAME, key = "'pic:detail:' + #id", 
               condition = "#result == null")
    public PictureDO detail(Long id) {
        return pictureDataService.getById(id);
    }
}
```

7.缓存配置文件

applicationContext.xml

```xml
<!-- 开启缓存注解 --> 
<cache:annotation-driven cache-manager="cacheManager"/>
```

applicationContext-cache.xml

```xml
<bean id="cacheManager" class="..."/>
```

