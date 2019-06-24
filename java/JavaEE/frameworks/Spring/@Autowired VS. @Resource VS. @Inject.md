# @Autowired VS. @Resource VS. @Inject

## 一. Spring注解

#### 1.@Autowired

`@Autowired` 可以用于**构造函数**，例如：

```java
public class MovieRecommender {

    private final CustomerPreferenceDao customerPreferenceDao;

    @Autowired
    public MovieRecommender(CustomerPreferenceDao customerPreferenceDao) {
        this.customerPreferenceDao = customerPreferenceDao;
    }

    // ...
}
```

从Spring Framework 4.3版本开始，如果目标bean只定义了一个构造函数，其上的`@Autowired`  注解可以省略。但是当有多个构造函数的时候，需要使用`@Autowired`至少明确指定其中之一。

`@Autowired` 注解同样可以应用于”传统的“**setter方法**：

```java
public class SimpleMovieLister {

    private MovieFinder movieFinder;

    @Autowired
    public void setMovieFinder(MovieFinder movieFinder) {
        this.movieFinder = movieFinder;
    }

    // ...
}
```

我们甚至可以在**任意方法名，任意参数列表的普通方法**上使用`@Autowired`注解：

```java
public class MovieRecommender {

    private MovieCatalog movieCatalog;

    private CustomerPreferenceDao customerPreferenceDao;

    @Autowired
    public void prepare(MovieCatalog movieCatalog,
            CustomerPreferenceDao customerPreferenceDao) {
        this.movieCatalog = movieCatalog;
        this.customerPreferenceDao = customerPreferenceDao;
    }

    // ...
}
```

我们也可以在**属性**上使用`@Autowired` 注解，甚至和构造方法上的混用：

```java
public class MovieRecommender {

    private final CustomerPreferenceDao customerPreferenceDao;

    @Autowired
    private MovieCatalog movieCatalog;

    @Autowired
    public MovieRecommender(CustomerPreferenceDao customerPreferenceDao) {
        this.customerPreferenceDao = customerPreferenceDao;
    }

    // ...
}
```

`@Autowired`注解也支持自动装配bean的**数组**和通过泛型指定元素类型的**集合**：

```java
public class MovieRecommender {

    @Autowired
    private MovieCatalog[] movieCatalogs;

    // ...
}
```

```java
public class MovieRecommender {

    private Set<MovieCatalog> movieCatalogs;

    @Autowired
    public void setMovieCatalogs(Set<MovieCatalog> movieCatalogs) {
        this.movieCatalogs = movieCatalogs;
    }

    // ...
}
```

如果希望指定数组或列表中的bean的顺序，则目标bean可以实现`org.springframework.core.Ordered`接口或使用`@Order`或`@Priority`注解。否则，它们的顺序将遵循容器中相应目标bean定义的注册顺序。`@Order`注解可以在目标类级别声明，也可以在`@Bean`方法上声明。 `@Order`值可以影响集合或数组中元素注入的优先级，但注意它们不会影响单例bean的初始化顺序。

`@Autowired`甚至支持自动装配为**Map**，Map的key必须为String，存储的是所有bean的name，value需要由泛型类型指定，里面包含所有符合泛型类型的bean：

```java
public class MovieRecommender {

    private Map<String, MovieCatalog> movieCatalogs;

    @Autowired
    public void setMovieCatalogs(Map<String, MovieCatalog> movieCatalogs) {
        this.movieCatalogs = movieCatalogs;
    }

    // ...
}
```

`@Autowired`注解的默认行为是将带有此注释的方法，构造函数和字段视为指示required=true。所以如果没有符合条件的候选bean，那么自动装配就会失败。这一默认行为可以通过以下方式改变：

```java
public class SimpleMovieLister {

    private MovieFinder movieFinder;

    @Autowired(required = false)
    public void setMovieFinder(MovieFinder movieFinder) {
        this.movieFinder = movieFinder;
    }

    // ...
}
```

每个类只有一个带`@Autowired`注解的构造函数可以标记为required=ture。如果有多个构造函数，Spring会选用“最贪婪”的那一个构造函数，即可以满足最大数量参数注入的构造函数。相比`@Required`注解，建议使用`@Autowired`注解的required属性。required属性为false表示该属性不是自动装配所必需的，如果无法自动装配，则会忽略该属性。另一方面，`@Required`更强大，因为它强制执行任何由容器支持的方式设置的属性。 如果未能注入任何值，则会引发相应的异常。

另外，我们可以通过Java 8的`java.util.Optional`来表达required=false相同的语义：

```java
public class SimpleMovieLister {

    @Autowired
    public void setMovieFinder(Optional<MovieFinder> movieFinder) {
        ...
    }
}
```

从Spring Framework 5.0开始，我们也可以使用任何包下名为`@Nullable`的注解来表达上述语义：

```java
public class SimpleMovieLister {

    @Autowired
    public void setMovieFinder(@Nullable MovieFinder movieFinder) {
        ...
    }
}
```

使用`@Autowired` 也可以注入一些在Spring中常用的bean，例如：`BeanFactory`, `ApplicationContext`, `Environment`, `ResourceLoader`, `ApplicationEventPublisher`, 和`MessageSource`。这些接口和其子接口，例如： `ConfigurableApplicationContext` 或者`ResourcePatternResolver`，可以直接装配使用，无需其他配置：

```java
public class MovieRecommender {

    @Autowired
    private ApplicationContext context;

    public MovieRecommender() {
    }

    // ...
}
```

`@Autowired`，`@Inject`，`@Resource`和`@Value`注解都是由Spring的`BeanPostProcessor`实现类处理的，反过来这意味着在我们自己的`BeanPostProcessor`或`BeanFactoryPostProcessor`中这些注解是不支持的。这些依赖必须通过XML或使用Spring的` @Bean`方法显式装配。

#### 2.@Primary

`@Autowired`默认按照类型装配。按类型自动装配时可能会有多个符合类型的候选bean，因此通常需要对选择过程进行更细粒度的控制。实现这一目标的一种方法是使用Spring的`@Primary`注解。`@Primary`表示当多个bean满足自动装配到单值依赖项的条件时，应该优先选择特定的bean。

如下的例子中，我们假设定义`firstMovieCatalog` 作为优先的`MovieCatalog`：

```
@Configuration
public class MovieConfiguration {

    @Bean
    @Primary
    public MovieCatalog firstMovieCatalog() { ... }

    @Bean
    public MovieCatalog secondMovieCatalog() { ... }

    // ...
}
```

基于以上配置，在下面代码中，`firstMovieCatalog`会被装配到`MovieRecommender` ：

```java
public class MovieRecommender {

    @Autowired
    private MovieCatalog movieCatalog;

    // ...
}
```

相应的xml配置如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">

    <context:annotation-config/>

    <bean class="example.SimpleMovieCatalog" primary="true">
        <!-- inject any dependencies required by this bean -->
    </bean>

    <bean class="example.SimpleMovieCatalog">
        <!-- inject any dependencies required by this bean -->
    </bean>

    <bean id="movieRecommender" class="example.MovieRecommender"/>

</beans>
```

#### 3.@Qualifier

当我们可以从多个候选的bean中指定某一个时使用`@Primary` 注解时一个非常有效的方式。但当我们需要更细粒度的控制bean的选择过程时，需要使用Spring的`@Qualifier` 注解。我们可以为qualifier指定特定的参数，缩小符合条件的bean的范围。最简单的使用方式是指定一个纯文本的描述值：

```java
public class MovieRecommender {

    @Autowired
    @Qualifier("main")
    private MovieCatalog movieCatalog;

    // ...
}
```

`@Qualifier`注解同样可以用于单个构造函数的入参或者方法的入参：

```java
public class MovieRecommender {

    private MovieCatalog movieCatalog;

    private CustomerPreferenceDao customerPreferenceDao;

    @Autowired
    public void prepare(@Qualifier("main")MovieCatalog movieCatalog,
            CustomerPreferenceDao customerPreferenceDao) {
        this.movieCatalog = movieCatalog;
        this.customerPreferenceDao = customerPreferenceDao;
    }

    // ...
}
```

对应的xml配置如下。带有qulifier值为"main"的bean会被装配到构造函数入参中带有相同qulifier值的参数值中去：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">

    <context:annotation-config/>

    <bean class="example.SimpleMovieCatalog">
        <qualifier value="main"/>
        <!-- inject any dependencies required by this bean -->
    </bean>

    <bean class="example.SimpleMovieCatalog">
        <qualifier value="action"/>
        <!-- inject any dependencies required by this bean -->
    </bean>

    <bean id="movieRecommender" class="example.MovieRecommender"/>

</beans>
```

bean的name会被作为默认的qualifier值，因此我们可以定义一个id="main"的bean而无需单独指定bean的qualifier值，对于Spring的选择结果是一样的。然而，虽然我们使用这样的默认行为实现了类似按照bean的名称装配的功能，但是Spring的基石是按照类型装配，qualifier只是其可选的语义之一。这意味着即使有这样的默认行为，qualifier也只是Spring按类型匹配语义的一个子集；并不意味着使用qualifier可以直接指向一个bean的ID。推荐的qualifier值类似于"main"或者"EMEA"或者"persistent"，表达了某个组件与其bean的ID毫不相干的业务特征。

限定符也适用于泛型集合，如上所述，例如，`Set <MovieCatalog>`。 在这种情况下，与声明的限定符匹配的所有bean都会被注入集合。 这意味着qualifier值不必是唯一的；它们只是简单地构成过滤标准。 例如，您可以使用相同的限定符值“action”定义多个`MovieCatalog` bean，所有这些bean都将注入到使用`@Qualifier（“action”)`注解的`Set <MovieCatalog>`中。

在类型匹配候选项中，允许根据目标bean根据名称选择限定符值，甚至不需要在注入点处使用`@Qualifier`注解。如果没有其他限制性注解（例如`@Qualifier`或`@Primary`），对于非唯一依赖性情况，Spring将使用注入点名称（即字段名称或参数名称）与目标bean名称匹配，并选择相同的。但是如果你打算用bean的name来表达注解驱动的注入，那么即使能够在类型匹配的候选者中根据bean的name做出选择，也不要主要使用`@Autowired`。 相反，使用JSR-250` @Resource`注解，它在语义上定义为通过其唯一名称标识特定目标组件，其类型与匹配过程无关。

`@Autowired`有相当不同的语义：按类型选择候选bean之后，使用指定的String类型qualifier值从中筛选。对于自身定义为集合或数组类型的bean，`@Resource `是一个很好的解决方案，通过唯一名称引用特定的集合或数组类型的bean。也就是说，从4.3开始，只要元素类型信息保存在`@Bean`返回类型签名或集合继承层次结构中，集合和数组类型也可以通过Spring的`@Autowired`类型匹配算法进行匹配。在这种情况下，qualifier值可用于在相同类型的集合中进行选择，如前一段所述。

我们也可以创建自己的自定义限定注解。 只需定义注解并在定义中提供`@Qualifier`注解：

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface Genre {
    String value();
}
```

然后我们就可以在`@Autowire`注解的属性或者方法上使用我们的自定义限定注解：

```java
public class MovieRecommender {

    @Autowired
    @Genre("Action")
    private MovieCatalog actionCatalog;

    private MovieCatalog comedyCatalog;

    @Autowired
    public void setComedyCatalog(@Genre("Comedy") MovieCatalog comedyCatalog) {
        this.comedyCatalog = comedyCatalog;
    }

    // ...
}
```

在某些情况下，使用没有value属性的注解可能就足够了。 当注解用于更通用的目的并且可以跨多种不同类型的依赖项应用时，这种方式会很有用。 例如，我们提供offline目录，当没有可用的Internet连接时将搜索该目录。首先定义一个简单注解：

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface Offline {

}
```

然后在自动装配的bean上使用：

```java
public class MovieRecommender {

    @Autowired
    @Offline
    private MovieCatalog offlineCatalog;

    // ...
}
```

此时，bean的定义中仅仅需要一个qualifier的属性`type`:

```xml
<bean class="example.SimpleMovieCatalog">
    <qualifier type="Offline"/>
    <!-- inject any dependencies required by this bean -->
</bean>
```

我们还可以定义接收其他属性的自定义qualifer注解，以补充简单的`value`属性或者代替`value`属性。 如果在要自动装配的字段或参数上指定了限定注解的多个属性值，则bean定义必须匹配所有的属性值才能被视为自动装配的候选。 例如如下的注解定义：

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface MovieQualifier {
    String genre();
    Format format();
}
```

其中的`Format` 是一个枚举：

```java
public enum Format {
    VHS, DVD, BLURAY
}
```

需要自动注入的属性使用了我们的自定义注解，并同时制定了`genre` 和`format`属性：

```java
public class MovieRecommender {

    @Autowired
    @MovieQualifier(format=Format.VHS, genre="Action")
    private MovieCatalog actionVhsCatalog;

    @Autowired
    @MovieQualifier(format=Format.VHS, genre="Comedy")
    private MovieCatalog comedyVhsCatalog;

    @Autowired
    @MovieQualifier(format=Format.DVD, genre="Action")
    private MovieCatalog actionDvdCatalog;

    @Autowired
    @MovieQualifier(format=Format.BLURAY, genre="Comedy")
    private MovieCatalog comedyBluRayCatalog;

    // ...
}
```

最后，bean定义应包含相匹配的qualifier属性值。 此示例还演示了可以使用bean的meta属性实现`<qualifier />`子元素的功能。 如果有的话，`<qualifier/>`及其属性优先，但如果没有qualifier，自动装配机制将回退到`<meta/>`标记内提供的值，如在以下示例。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">

    <context:annotation-config/>

    <bean class="example.SimpleMovieCatalog">
        <qualifier type="MovieQualifier">
            <attribute key="format" value="VHS"/>
            <attribute key="genre" value="Action"/>
        </qualifier>
        <!-- inject any dependencies required by this bean -->
    </bean>

    <bean class="example.SimpleMovieCatalog">
        <qualifier type="MovieQualifier">
            <attribute key="format" value="VHS"/>
            <attribute key="genre" value="Comedy"/>
        </qualifier>
        <!-- inject any dependencies required by this bean -->
    </bean>

    <bean class="example.SimpleMovieCatalog">
        <meta key="format" value="DVD"/>
        <meta key="genre" value="Action"/>
        <!-- inject any dependencies required by this bean -->
    </bean>

    <bean class="example.SimpleMovieCatalog">
        <meta key="format" value="BLURAY"/>
        <meta key="genre" value="Comedy"/>
        <!-- inject any dependencies required by this bean -->
    </bean>

</beans>
```

#### 4.Using generics as autowiring qualifiers

除了使用`@Qualifier` 注解，Java泛型也可以被用作一种隐式的限定方式。例如我们有如下配置：

```java
@Configuration
public class MyConfiguration {

    @Bean
    public StringStore stringStore() {
        return new StringStore();
    }

    @Bean
    public IntegerStore integerStore() {
        return new IntegerStore();
    }
}
```

假设上述的bean都实现了一个泛型接口，例如`Store<String>` 和`Store<Integer>`，如下的`@Autowire` 会使用泛型作为一个隐式的限定符来装配`Store` ：

```
@Autowired
private Store<String> s1; // <String> qualifier, injects the stringStore bean

@Autowired
private Store<Integer> s2; // <Integer> qualifier, injects the integerStore bean
```

泛型限定方法也可以应用于集合数组等：

```
// Inject all Store beans as long as they have an <Integer> generic
// Store<String> beans will not appear in this list
@Autowired
private List<Store<Integer>> s;
```

#### 1.9.6. CustomAutowireConfigurer

[`CustomAutowireConfigurer`](https://docs.spring.io/spring-framework/docs/5.0.2.RELEASE/javadoc-api/org/springframework/beans/factory/annotation/CustomAutowireConfigurer.html) 是一个`BeanFactoryPostProcessor` ，支持我们注册自定义的注解类型，即使没有使用Spring的 `@Qualifier` 注解：

```xml
<bean id="customAutowireConfigurer"
        class="org.springframework.beans.factory.annotation.CustomAutowireConfigurer">
    <property name="customQualifierTypes">
        <set>
            <value>example.CustomQualifier</value>
        </set>
    </property>
</bean>
```

The `AutowireCandidateResolver` determines autowire candidates by:

- the `autowire-candidate` value of each bean definition
- any `default-autowire-candidates` pattern(s) available on the `<beans/>` element
- the presence of `@Qualifier` annotations and any custom annotations registered with the `CustomAutowireConfigurer`

When multiple beans qualify as autowire candidates, the determination of a "primary" is the following: if exactly one bean definition among the candidates has a `primary` attribute set to `true`, it will be selected.

#### 1.9.7. @Resource

Spring also supports injection using the JSR-250 `@Resource` annotation on fields or bean property setter methods.  Spring supports this pattern for Spring-managed objects as well.

Spring还支持在字段或bean属性setter方法上使用JSR-250` @Resource`注解进行注入。`@Resource`采用name属性，它遵循by-name语义，如本例所示：

```java
public class SimpleMovieLister {

    private MovieFinder movieFinder;

    @Resource(name="myMovieFinder")
    public void setMovieFinder(MovieFinder movieFinder) {
        this.movieFinder = movieFinder;
    }
}
```

如果未明确指定名称，则bean的默认名称是从字段名称或setter方法派生出来的。如果是字段，则采用字段名称；在setter方法的情况下，它采用bean属性名称。所以下面的例子将把名为“movieFinder”的bean注入其setter方法：

```java
public class SimpleMovieLister {

    private MovieFinder movieFinder;

    @Resource
    public void setMovieFinder(MovieFinder movieFinder) {
        this.movieFinder = movieFinder;
    }
}
```

在下面的示例中，`customerPreferenceDao`字段首先查找名为customerPreferenceDao的bean，如果找不到，则查找类型为CustomerPreferenceDao的类型进行匹配。 基于已知的可解析依赖类型“ApplicationContext”注入“context”字段。

```java
public class MovieRecommender {

    @Resource
    private CustomerPreferenceDao customerPreferenceDao;

    @Resource
    private ApplicationContext context;

    public MovieRecommender() {
    }

    // ...
}
```

### 二. JSR 330 标准注解

从Spring 3.0开始，Spring提供了对JSR-330标准注解(依赖注入)的支持。Spring像扫描自己提供的注解一样扫描这些注解。我们只需要引入如下的依赖：

```xml
<dependency>
    <groupId>javax.inject</groupId>
    <artifactId>javax.inject</artifactId>
    <version>1</version>
</dependency>
```

#### 1.@Inject @Named ≈≈ @Resource @Qualifier

相较于`@Autowired`，`@javax.inject.Inject` 的使用方式如下：

```java
import javax.inject.Inject;

public class SimpleMovieLister {

    private MovieFinder movieFinder;

    @Inject
    public void setMovieFinder(MovieFinder movieFinder) {
        this.movieFinder = movieFinder;
    }

    public void listMovies() {
        this.movieFinder.findMovies(...);
        ...
    }
}
```

和`@Autowired`一样，`@Inject` 可以用于属性级别，方法级别和构造函数参数级别。并且支持我们将注入点声明为一个Provider<>，允许我们延迟请求或者在更小的生命周期内使用Provider.get()方法请求bean。上述例子的一个变体如下：

```java
import javax.inject.Inject;
import javax.inject.Provider;

public class SimpleMovieLister {

    private Provider<MovieFinder> movieFinder;

    @Inject
    public void setMovieFinder(Provider<MovieFinder> movieFinder) {
        this.movieFinder = movieFinder;
    }

    public void listMovies() {
        this.movieFinder.get().findMovies(...);
        ...
    }
}
```

如果你想要注入指定名称的bean，可以像下面这样使用`@Named` 注解：

```java
import javax.inject.Inject;
import javax.inject.Named;

public class SimpleMovieLister {

    private MovieFinder movieFinder;

    @Inject
    public void setMovieFinder(@Named("main") MovieFinder movieFinder) {
        this.movieFinder = movieFinder;
    }

    // ...
}
```

像`@Autowired`一样，`@Inject` 也可以和`java.util.Optional`或者`@Nullable`一起使用。这样甚至更合适，因为`@Inject`注解没有`required`属性。

```java
public class SimpleMovieLister {

    @Inject
    public void setMovieFinder(Optional<MovieFinder> movieFinder) {
        ...
    }
}
```

```java
public class SimpleMovieLister {

    @Inject
    public void setMovieFinder(@Nullable MovieFinder movieFinder) {
        ...
    }
}
```

#### 2. @Named @ManagedBean ≈≈ @Component

相较于`@Component`，`@javax.inject.Named`或者`javax.annotation.ManagedBean`的使用方式如下：

```java
import javax.inject.Inject;
import javax.inject.Named;

@Named("movieListener")  
// @ManagedBean("movieListener") could be used as well
public class SimpleMovieLister {

    private MovieFinder movieFinder;

    @Inject
    public void setMovieFinder(MovieFinder movieFinder) {
        this.movieFinder = movieFinder;
    }

    // ...
}
```

使用`@Component` 注解时通常我们不会为其指定名称，`@Named` 注解在使用时也可以沿用这一风格：

```java
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SimpleMovieLister {

    private MovieFinder movieFinder;

    @Inject
    public void setMovieFinder(MovieFinder movieFinder) {
        this.movieFinder = movieFinder;
    }

    // ...
}
```

使用`@Named`或者`@ManagedBean`的时候，仍然可以使用与Spring的component scan相同的配置：

```java
@Configuration
@ComponentScan(basePackages = "org.example")
public class AppConfig  {
    ...
}
```

#### 3. JSR-330标准注解的局限

当使用标准注解时，很重要的一点是需要明白Spring的一些很重要的特性是不支持的。详情如下表：

| Spring              | javax.inject.*        | javax.inject restrictions / comments                         |
| ------------------- | --------------------- | ------------------------------------------------------------ |
| @Autowired          | @Inject               | `@Inject` has no 'required' attribute; can be used with Java 8’s `Optional`instead. |
| @Component          | @Named / @ManagedBean | JSR-330 does not provide a composable model, just a way to identify named components. |
| @Scope("singleton") | @Singleton            | The JSR-330 default scope is like Spring’s `prototype`. However, in order to keep it consistent with Spring’s general defaults, a JSR-330 bean declared in the Spring container is a `singleton` by default. In order to use a scope other than `singleton`, you should use Spring’s `@Scope` annotation. `javax.inject` also provides a [@Scope](https://download.oracle.com/javaee/6/api/javax/inject/Scope.html)annotation. Nevertheless, this one is only intended to be used for creating your own annotations. |
| @Qualifier          | @Qualifier / @Named   | `javax.inject.Qualifier` is just a meta-annotation for building custom qualifiers. Concrete String qualifiers (like Spring’s `@Qualifier` with a value) can be associated through `javax.inject.Named`. |
| @Value              | -                     | no equivalent                                                |
| @Required           | -                     | no equivalent                                                |
| @Lazy               | -                     | no equivalent                                                |
| ObjectFactory       | Provider              | `javax.inject.Provider` is a direct alternative to Spring’s `ObjectFactory`, just with a shorter `get()` method name. It can also be used in combination with Spring’s `@Autowired`or with non-annotated constructors and setter methods. |
