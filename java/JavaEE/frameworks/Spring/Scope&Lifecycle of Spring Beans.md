# Scope&Lifecycle of Spring Beans

## Bean scopes

根据我们工程中指定的配置文件或/和注解，Spring的容器ApplicationContext为我们提供了Bean的全生命周期管理功能，包括Bean的创建和多种维度的初始化/Bean之间的依赖关系装配/Bean的作用域/Bean的生命周期等。

在官方文档中，spring列出了下表来说明其支持的Bean scope

| Scope                                                        | Description                                                  | Environment |
| ------------------------------------------------------------ | :----------------------------------------------------------- | ----------- |
| [singleton](https://docs.spring.io/spring/docs/5.0.8.RELEASE/spring-framework-reference/core.html#beans-factory-scopes-singleton) | **(Default)** Scopes a single bean definition to a single object instance per Spring IoC container. | all         |
| [prototype](https://docs.spring.io/spring/docs/5.0.8.RELEASE/spring-framework-reference/core.html#beans-factory-scopes-prototype) | Scopes a single bean definition to any number of object instances. | all         |
| [request](https://docs.spring.io/spring/docs/5.0.8.RELEASE/spring-framework-reference/core.html#beans-factory-scopes-request) | Scopes a single bean definition to the lifecycle of a single HTTP request; that is, each HTTP request has its own instance of a bean created off the back of a single bean definition. | web         |
| [session](https://docs.spring.io/spring/docs/5.0.8.RELEASE/spring-framework-reference/core.html#beans-factory-scopes-session) | Scopes a single bean definition to the lifecycle of an HTTP `Session`. | web         |
| [application](https://docs.spring.io/spring/docs/5.0.8.RELEASE/spring-framework-reference/core.html#beans-factory-scopes-application) | Scopes a single bean definition to the lifecycle of a `ServletContext`. | web         |
| [websocket](https://docs.spring.io/spring/docs/5.0.8.RELEASE/spring-framework-reference/web.html#websocket-stomp-websocket-scope) | Scopes a single bean definition to the lifecycle of a `WebSocket`. | web         |

### singleton scope

Spring Bean默认的作用域，所谓单例的范围是基于IOC容器的，即每个IOC容器中只有一个。这些Bean会一直存活在IOC容器中，其寿命可以理解为与IOC容器相同。

### prototype scope

即每一次请求一个Bean的时候Spring都会为我们新建一个，完全可以理解为new操作符的替代。如果singleton bean依赖了prototype bean，他们的依赖关系是在singleton bean被创建的时候一次性组装完成的，所以需要使用spring提供的method injection技术在每次请求这个singleton bean的时候刷新其依赖的prototype bean。可以通过在xml配置中指定look-up method或者replaced-method两种方法来实现。

### request, session, application, and WebSocket scopes

这四种scope只在WebApplicationContext环境下支持，在常规的非web环境下使用会抛出`IllegalStateException` ，分别对应一次http request/http session/ServletContext/WebSocket。

## Spring Bean不同Scope的线程安全问题

singleton bean是会被多线程同时使用的，所以需要考虑线程安全问题，但是一般来讲，我们交给Spring管理的Bean都是无状态对象(Stateless bean)，像是Controller/Service/Dao都是可以认为是无状态对象，不存在可变的局部变量，也不保存其他状态信息，在多线程环境下运行不会产生线程安全问题，配合Spring默认的singleton scope可以节省系统开销，是一种理想的设计。prototype bean天然避免了线程安全问题，但是单纯的通过把Controller配置成prototype scope会严重影响系统性能。在Struts2中，由于其request mapping是基于Action类的，并且在Action中需要声明入参类型作为类变量，提供getter/setter方法注入值，所以是有状态的，需要配置成prototype。

但是在实际工程中，尤其是是Service层，很有可能保存一些状态信息，这个时候就需要我们在业务代码中自己处理线程安全问题。在一些对性能要求不是很严格的场景下，ThreadLocal是一个比较合适的选择。

## Lifecycles

通过实现`InitializingBean` / `DisposableBean` 接口，或者在Bean的方法上添加`@PostConstruct`/`@PreDestroy`注解，或者在applicationContext.xml的<bean></bean>标签中配置`init-method`/`destroy-method`属性，都可以为特定的Bean指定生命周期行为。前者的执行时机是

> after all necessary properties on the bean have been set by the container

后者的执行时机是

> when the container containing it is destroyed

当同时存在上述多种机制时，其执行顺序是

- Methods annotated with `@PostConstruct`
- `afterPropertiesSet()` as defined by the `InitializingBean` callback interface
- A custom configured `init()` method

BeanNameAware接口提供了一个setBeanName()方法让我们获取Bean的名字。这个方法的执行时机是

> after population of normal bean properties but before an initialization callback such as InitializingBean afterPropertiesSet or a custom init-method

相同的BeanFactoryAware接口为我们提供了获取运行时BeanFactory实例的机会。

BeanPostProcessor接口提供了postProcessBeforeInitialization()和postProcessAfterInitialization()两个方法用于在所有bean初始化之前或之后执行我们所需要插入的逻辑。并且Spring支持我们配置多个BeanPostProcessor并指定他们的顺序。

BeanFactoryPostProcessor接口与BeanPostProcessor相似，但是它的设计目的是为了操作 bean configuration metadata，也就是说IOC容器使用BeanFactoryPostProcessor读取配置文件并且在IOC容器初始化任何bean之前改变这些配置。同样支持多个，并且指定顺序。

我们可以通过如下的代码验证这些配置生效的的顺序：

```java
package com.jr.controller.components;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class LifeCycleBeanPostProcessor implements BeanPostProcessor {

    @Nullable
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) 
        throws BeansException {
        System.out.println("postProcessAfterInitialization()" + beanName);
        return bean;
    }

    @Nullable
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) 
        throws BeansException {
        System.out.println("postProcessBeforeInitialization()" + beanName);
        return bean;
    }
}
```

```java
package com.jr.controller.components;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class LifeCycleBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(
        ConfigurableListableBeanFactoryconfigurableListableBeanFactory) 
        throws BeansException {
        System.out.println("postProcessBeanFactory()");
    }
}
```

```java
package com.jr.controller.components;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class LifeCycleBean implements 
    InitializingBean, DisposableBean, BeanFactoryAware, BeanNameAware {

    LifeCycleBean() {
        System.out.println("constructor()");
    }

    @PostConstruct
    public void postConstructMethod() throws Exception {
        System.out.println("@PostConstruct");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet()");
    }

    @PreDestroy
    public void preDestroyMethod() throws Exception {
        System.out.println("@PreDestroy");
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("destroyed()");
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        System.out.println("setBeanFactory() with param : "
                + beanFactory.getClass().getSimpleName());
    }

    @Override
    public void setBeanName(String name) {
        System.out.println("setBeanName() with param :" + name);
    }
}

```

通过单元测试我们可以得到这样的输出：

```java
postProcessBeanFactory()
constructor()
setBeanName() with param :lifeCycleBean
setBeanFactory() with param : DefaultListableBeanFactory
postProcessBeforeInitialization()lifeCycleBean
@PostConstruct
afterPropertiesSet()
postProcessAfterInitialization()lifeCycleBean
...
@PreDestroy
destroyed()
```

总结起来我们可以做这样一个流程图(github不支持md的流程图语法)：

```flow
st=>start: Spring IOC 容器启动
op1=>operation: 启动BeanFactoryPostProcessor实现类
op2=>operation: 执行BeanFactoryPostProcessor.postProcessBeanFactory()
op3=>operation: 启动BeanPostProcessor实现类
op4=>operation: 执行Bean的构造器
op5=>operation: 执行BeanPostProcessor.postProcessBeforeInitialization()
op6=>operation: 为Bean注入属性
op7=>operation: 执行BeanNameAware的setBeanName()方法
op8=>operation: 执行BeanFactoryAware的setBeanFactory()方法
op9=>operation: 执行@PostConstruct注解的方法
op10=>operation: 执行InitializingBean的afterPropertiesSet()方法
op11=>operation: 执行<bean>标签中配置的init-method方法
op12=>operation: 执行BeanPostProcessor.postProcessAfterInitialization()
op13=>operation: 容器初始化成功
e=>end

st->op1->op2->op3->op4->op5->op6->op7->op8->op9->op10->op11->op12->op13->e
```



























