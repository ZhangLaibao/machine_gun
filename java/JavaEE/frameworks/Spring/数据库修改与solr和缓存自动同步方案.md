# 数据库修改与solr和缓存自动同步方案

### 1.场景描述

假设我们有一个商品模型，商品的搜索使用搜索引擎技术solr实现，商品的详情使用了redis缓存，商品的数据操作层使用了MyBatis框架。我们需要自动将对商品模型数据的修改自动并且异步的同步到solr和redis。

### 2.技术方案

1.使用MyBatis提供的Interceptor机制监控数据修改，发布事件。

2.使用Spring的ApplicationContext发布事件，业务监听到事件做相应处理。

3.使用@Cacheable注解简化缓存逻辑

### 3.技术细节

#### 1.MyBatis Interceptors

关于MyBatis的一些架构和流程大致可以参考如下两张图：

![MyBatis架构图](https://github.com/ZhangLaibao/machine_gun/blob/master/images/MyBatis-archi.jpg) 
![MyBatis流程图](https://github.com/ZhangLaibao/machine_gun/blob/master/images/MyBatis-flow.jpg) 

首先我们来看MyBatis源码

`(代码位置：org.mybatis:mybatis:3.2.2 ---> org.apache.ibatis.session.Configuration:433-477)`

```java
public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
    parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
    return parameterHandler;
  }

  public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler,
      ResultHandler resultHandler, BoundSql boundSql) {
    ResultSetHandler resultSetHandler = mappedStatement.hasNestedResultMaps() ? new NestedResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql,
        rowBounds) : new FastResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
    resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
    return resultSetHandler;
  }

  public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
    statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
    return statementHandler;
  }

  public Executor newExecutor(Transaction transaction, ExecutorType executorType, boolean autoCommit) {
    executorType = executorType == null ? defaultExecutorType : executorType;
    executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
    Executor executor;
    if (ExecutorType.BATCH == executorType) {
      executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
      executor = new ReuseExecutor(this, transaction);
    } else {
      executor = new SimpleExecutor(this, transaction);
    }
    if (cacheEnabled) {
      executor = new CachingExecutor(executor, autoCommit);
    }
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
  }
```

我们看到，在返回MyBatis的四大组件：Executor/StatementHandler/ResultSetHandler/ParameterHandler之前，首先会执行`interceptorChain.pluginAll();`方法，我们自定义的interceptor会被MyBatis接管并加入对应的interceptorChain，在组件执行之前，执行拦截逻辑。

`interceptorChain.pluginAll()`的逻辑十分简单，即依次执行所有拦截器的plugin方法，这个plugin方法是我们自定义interceptor时需要重写的方法，目的是为目标对象生成我们的代理对象：

`(代码位置：org.mybatis:mybatis:3.2.2 ---> org.apache.ibatis.plugin.InterceptorChain:26)`

```java
  public Object pluginAll(Object target) {
    for (Interceptor interceptor : interceptors) {
      target = interceptor.plugin(target);
    }
    return target;
  }
```

然后我们来看这个Interceptor接口(源码里面没注释)：

```java
public interface Interceptor {
    
    /**
     * 实际的拦截逻辑，可以类比Spring自定义切面的@Around开发
     * 我们常用的PageHelper就是用了Interceptor来实现的，可以参考其代码中的interceptor逻辑
     * com.github.pagehelper.PageHelper#intercept
     */
  	Object intercept(Invocation invocation) throws Throwable;
  	
    /**
     * 为目标对象生成代理对象，例如：
     * @Override
     * public Object plugin(Object target) {
     *     return Plugin.wrap(target, this);
     * }
     * 使用Plugin.wrap()静态方法，将当前对象(也就是我们自定义拦截器的实例)包装到目标对象，MyBatis
     * 使用了JDK的动态代理机制生成代理对象，对目标对象的调用自然会被代理到我们的代理对象，自然就可以
     * 控制我们interceptor方法的执行。
     */
  	Object plugin(Object target);
 
    /**
     * 注册一些属性，这些属性可以在配置拦截器时在xml文件里配置
     */
    void setProperties(Properties properties);
}
```

我们在开发自定义Interceptor时需要使用注解对拦截器做一些说明，比如：

```java
@Intercepts({
    @Signature(
            type = Executor.class, method = "update", 
            args = {MappedStatement.class, Object.class})
})
```

关于@Intercepts的使用，此处不再作详细说明。

在具体的interceptor逻辑中，我们只是简单的把事件发布到ApplicationContext，而不是直接做业务处理。这是因为MyBatis在执行可监听的四大组件之前，需要先执行拦截逻辑。以我们的问题场景为例，如果在拦截逻辑中同步做业务处理，会延长数据更新操作的响应时间，影响系统系能，并且如果拦截器逻辑出错，还会影响到数据的更新操作。我们简单的把事件发布出去，通过监听异步的处理业务逻辑，可以提高系统性能和安全性。

#### 2.ApplicationContext.publishEvent()

Spring的事件发布和监听开发比较简单，主要有两个步骤。

1.我们需要使自定义的事件继承ApplicationEvent，重写构造器，在构造器中就可以指定事件创建时的数据传递逻辑。调用ApplicationContext提供的API：publishEvent(ApplicationEvent)即可将事件发布到整个应用。

2.开发事件监听逻辑，只需要在ApplicationContext管理的Bean中定义@EventListener注解的方法，将参数类型设置为我们自定义的事件类型，并开发业务处理逻辑即可。

我们来研究一下Spring在背后为我们做了什么工作。此处先提一下最基础的JDK提供的事件发布监听机制，JDK为此场景提供了最基础的抽象：一个代表事件的EventObject，一个代表监听事件的EventListener:

```java
// FROM JDK8, 源码有删减
/**
 * The root class from which all event state objects shall be derived.
 * All Events are constructed with a reference to the object, the "source",
 * that is logically deemed to be the object upon which the Event in question
 * initially occurred upon.
 */
public class EventObject implements java.io.Serializable {

    /**
     * The object on which the Event initially occurred. - 事件源
     */
    protected transient Object  source;

    /**
     * 通过构造器设置事件源
     */
    public EventObject(Object source) {
        if (source == null)
            throw new IllegalArgumentException("null source");
        this.source = source;
    }

    public Object getSource() {
        return source;
    }
}
```

```java
/**
 * A tagging interface that all event listener interfaces must extend. - 就是个标记接口
 */
public interface EventListener { }
```

基于JDK提供的机制，我们开发一个最简单的时间发布和监听例子：

工程结构：

> event
>
> ​	+----ServiceEvent - 业务事件定义
>
> ​	+----ServiceEventBean - 业务事件发布携带的数据
>
> ​	+----ServiceEnventListener - 业务事件监听器定义
>
> ​	+----ServiceEventListenerImpl - 业务事件监听处理逻辑
>
> ​	+----ServiceEventPublisher - 事件分发
>
> ​	+----TestEvent - 测试入口

具体代码如下(为精简篇幅，将所有代码放在了一个代码块中)：

```java
class ServiceEvent extends EventObject{
    public ServiceEvent(Object source) {
        super(source);
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ServiceEventBean {
    private int id;
    private String desc;
}

interface ServiceEventListener extends EventListener{
    void onEvent(ServiceEvent event);
}

class ServiceEventListenerImpl implements ServiceEventListener {
    @Override
    public void onEvent(ServiceEvent event) {
        System.out.println("Service Handle ServiceEvent");
    }
}

class ServiceEventPublisher {

    private static List<ServiceEventListener> listners =
            new ArrayList<ServiceEventListener>() {{
                add(new ServiceEventListenerImpl());
            }};

    //发布事件
    public static void publishEvent(ServiceEvent event) {
        for (ServiceEventListener listner : listners)
            listner.onEvent(event);
    }
}

public class TestEvent {
    public static void main(String[] args) {
        // 构造业务数据
        ServiceEventBean bean  = new ServiceEventBean(1000,"service");
        // 构造事件对象
        ServiceEvent event = new ServiceEvent(bean);
        // 发布事件
        ServiceEventPublisher.publishEvent(event);
    }
}

```

对应JDK的EventObject和EventListener，Spring提供了代表事件的抽象ApplicationEvent和代表事件监听器的ApplicationListener，源码如下：

```java
/**
 * Class to be extended by all application events. Abstract as it
 * doesn't make sense for generic events to be published directly.
 */
public abstract class ApplicationEvent extends EventObject {

	/** System time when the event happened */
	private final long timestamp;

	/**
	 * Create a new ApplicationEvent.
	 * @param source the object on which the event initially occurred (never null)
	 */
	public ApplicationEvent(Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Return the system time in milliseconds when the event happened.
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}
}
```

```java
/**
 * Interface to be implemented by application event listeners. Based on the standard
 * java.util.EventListener interface for the Observer design pattern.
 *
 * As of Spring 3.0, an ApplicationListener can generically declare the event type
 * that it is interested in. When registered with a Spring ApplicationContext, events
 * will be filtered accordingly, with the listener getting invoked for matching event
 * objects only.
 */
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

	/**
	 * Handle an application event.
	 * @param event the event to respond to
	 */
	void onApplicationEvent(E event);
}
```

根据源码我们你可以看到Spring对JDK提供的API仅仅做了简单封装，并且与我们写的JDK的例子有相同的封装思路。需要注意的一个点在于ApplicationListener被设计成泛型接口，这样在实际使用中我们只需要维护一个时间发布器，简化了使用。Spring做的最重要的事情在于我们无需自己维护事件的发布和监听对应，通过IOC容器，Spring会为我们组织事件的发布器和监听器实例，并在事件被发布出来的时候找到并调用对应的监听器。我们只需要根据Spring支持的方式开发即可，下面我们来看Spring是如何支持这种能力的。

如果研究过Spring启动过程，我们可以指导一个很关键的点，无论是何种ApplicationContext的实现，在启动过程中都要进入AbstractApplicationContext的refresh()方法，这个方法定义了Spring启动过程的骨架流程：

```java
	@Override
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			// Prepare this context for refreshing.
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				postProcessBeanFactory(beanFactory);

				// Invoke factory processors registered as beans in the context.
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				registerBeanPostProcessors(beanFactory);

				// Initialize message source for this context.
				initMessageSource();

                // ************初始化事件广播器************
				// Initialize event multicaster for this context.
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				onRefresh();

                // ************注册事件监听器************
				// Check for listener beans and register them.
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				finishRefresh();
			}
		......
        }
    }
```

由此我们知道在ApplicationContext的启动过程中需要初始化事件广播器并注册事件监听器。

初始化事件广播器的源码：

```java
protected void initApplicationEventMulticaster() {
	ConfigurableListableBeanFactory beanFactory = getBeanFactory();
	// APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";
    /**
     * 如果用户配置了名为applicationEventMulticaster的bean，则使用此bean作为ApplicationEventMulticaster
     */
    if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
		this.applicationEventMulticaster =
				beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
	}
    /**
     * 否则Spring会为我们创建一个SimpleApplicationEventMulticaster，并注册到容器
     * 在SimpleApplicationEventMulticaster中定义了线程池Executor来对事件监听进行异步回调
     * 定义了ErrorHandler处理回调逻辑可能出现的错误
     */
	else {
		this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
		beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
	}
}
```

注册事件监听器的源码：

```java
/**
 * Add beans that implement ApplicationListener as listeners.
 * Doesn't affect other listeners, which can be added without being beans.
 */
protected void registerListeners() {
	// Register statically specified listeners first.
	for (ApplicationListener<?> listener : getApplicationListeners()) {
		getApplicationEventMulticaster().addApplicationListener(listener);
	}
	......
}

```

此处通过调用getApplicationListeners()获取了所有的事件监听器并注册到ApplicationEventMulticaster，逻辑很好理解，但问题在于，ApplicationListeners是何时被识别并加载到IOC容器的，我们来跟进源码：

```java
/**
 * Return the list of statically specified ApplicationListeners.
 * applicationListerners被定义为ApplicationContextd的成员变量，我们需要找到它是何时被设置值的
 */
public Collection<ApplicationListener<?>> getApplicationListeners() {
	return this.applicationListeners;
}
```

在refresh()方法之前的perpareBeanFactory()方法里我们可以找到这样一行代码：

```java
/**
 * Configure the factory's standard context characteristics, - 定义IOC容器的标准特性
 * such as the context's ClassLoader and post-processors.
 */
protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	......
    // Register early post-processor for detecting inner beans as ApplicationListeners.
	beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));
    ......
}
```

我们可以确定，再启动容器的标准初始化过程中，是这个名为ApplicationListenerDetector的BeanPostProcessor来实际将监听器注册到IOC容器，其postProcessAfterInitialization()方法也确实是做了这件事情：

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    // ********找到ApplicationListener并注册到ApplicationContext的成员变量********
	if (this.applicationContext != null && bean instanceof ApplicationListener) {
		// potentially not detected as a listener by getBeanNamesForType retrieval
		Boolean flag = this.singletonNames.get(beanName);
		if (Boolean.TRUE.equals(flag)) {
			// singleton bean (top-level or inner): register on the fly
			this.applicationContext.addApplicationListener((ApplicationListener<?>) bean);
		}
		else if (Boolean.FALSE.equals(flag)) {
			this.singletonNames.remove(beanName);
		}
	}
	return bean;
}
```

他回调了AbstractApplicationContext的如下方法：

```java
@Override
public void addApplicationListener(ApplicationListener<?> listener) {
	Assert.notNull(listener, "ApplicationListener must not be null");
	if (this.applicationEventMulticaster != null) {
		this.applicationEventMulticaster.addApplicationListener(listener);
	}
	else {
		this.applicationListeners.add(listener);
	}
}
```

初始化的过程我们大致搞清楚了，下面我们来看事件发布和监听流程。

```java
/**
 * Publish the given event to all listeners.
 * Note: Listeners get initialized after the MessageSource, to be able to access it 
 * within listener implementations. Thus, MessageSource implementations cannot publish
 * events.
 */
@Override
public void publishEvent(Object event) {
	publishEvent(event, null);
}

/**
 * @param event the event to publish (may be an ApplicationEvent or a payload object 
 * to be turned into a PayloadApplicationEvent)
 * @param eventType the resolved event type, if known
 */
protected void publishEvent(Object event, ResolvableType eventType) {
	...
    // Decorate event as an ApplicationEvent if necessary
	ApplicationEvent applicationEvent;
	if (event instanceof ApplicationEvent) {
		applicationEvent = (ApplicationEvent) event;
	}
	else {
		applicationEvent = new PayloadApplicationEvent<Object>(this, event);
		if (eventType == null) {
			eventType = ((PayloadApplicationEvent) applicationEvent).getResolvableType();
		}
	}

    // Multicast right now if possible - or lazily once the multicaster is initialized
	if (this.earlyApplicationEvents != null) {
		this.earlyApplicationEvents.add(applicationEvent);
	}
	else {
		getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
	}

	// Publish event via parent context as well...
	if (this.parent != null) {
		if (this.parent instanceof AbstractApplicationContext) {
			((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
		}
		else {
			this.parent.publishEvent(event);
		}
	}
}
```

通过这段代码我们可以分析得出最终我们的业务事件，也就是应用启动之后在运行过程中的事件，是通过ApplicationEventMulticaster.multicastEvent()方法发布出来的。

```java
@Override
public void multicastEvent(final ApplicationEvent event, ResolvableType eventType) {
	ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
	for (final ApplicationListener<?> listener : getApplicationListeners(event, type)) {
		Executor executor = getTaskExecutor();
		if (executor != null) {
			executor.execute(() -> invokeListener(listener, event));
		} else {
			invokeListener(listener, event);
		}
	}
}
```

这段代码的逻辑和之前我们分析EventMulticaster时说的一致，在实际调用监听器时确实有使用线程池的逻辑。

```java
// 根据事件类型获取对应的监听器 - 这是真正事件和监听的对应逻辑实现的地方
// 梳理完流程，我们不再对此方法的细节深究
/**
 * Return a Collection of ApplicationListeners matching the given event type. 
 * Non-matching listeners get excluded early.
 * @param event the event to be propagated. Allows for excluding non-matching listeners 
 * early, based on cached matching information.
 * @param eventType the event type
 * @return a Collection of ApplicationListeners
 */
protected Collection<ApplicationListener<?>> getApplicationListeners(
		ApplicationEvent event, ResolvableType eventType) {

    Object source = event.getSource();
	Class<?> sourceType = (source != null ? source.getClass() : null);
	ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);

	// Quick check for existing entry on ConcurrentHashMap...
	ListenerRetriever retriever = this.retrieverCache.get(cacheKey);
	if (retriever != null) {
		return retriever.getApplicationListeners();
	}

	if (this.beanClassLoader == null ||
			(ClassUtils.isCacheSafe(event.getClass(), this.beanClassLoader) &&
					(sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {
		// Fully synchronized building and caching of a ListenerRetriever
		synchronized (this.retrievalMutex) {
			retriever = this.retrieverCache.get(cacheKey);
			if (retriever != null) {
				return retriever.getApplicationListeners();
			}
			retriever = new ListenerRetriever(true);
			Collection<ApplicationListener<?>> listeners =
					retrieveApplicationListeners(eventType, sourceType, retriever);
			this.retrieverCache.put(cacheKey, retriever);
			return listeners;
		}
	}
	else {
		// No ListenerRetriever caching -> no synchronization necessary
		return retrieveApplicationListeners(eventType, sourceType, null);
	}
}
```

#### 3.@Cacheable

Spring cache的实现其实就是简单的注解AOP实现，Spring为注解的方法所在的对象生成代理对象，按照注解逻辑实现缓存和实际业务方法之间的逻辑。通过debug我们可以看到，Spring为使用缓存注解的Bean生成了CacheInterceptor代理：

```java
@Override
public Object invoke(final MethodInvocation invocation) throws Throwable {
	Method method = invocation.getMethod();

	CacheOperationInvoker aopAllianceInvoker = new CacheOperationInvoker() {
		@Override
		public Object invoke() {
			try {
				return invocation.proceed();
			}
			catch (Throwable ex) {
				throw new ThrowableWrapper(ex);
			}
		}
	};

    try {
		return execute(aopAllianceInvoker, invocation.getThis(), method, invocation.getArguments());
	}
	catch (CacheOperationInvoker.ThrowableWrapper th) {
		throw th.getOriginal();
	}
}
```





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
        return target instanceof Executor? Plugin.wrap(target, this) : target;
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

