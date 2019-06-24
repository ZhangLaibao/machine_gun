## DUBBO集成JSR-303校验

1.接口定义中使用@Valid注解

```java
Result<Void> dubboJSRValidation(@Valid Request<RequestDto> request);
```

入参DTO中指定校验规则

```java
    @NotNull
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer intParam;

	@NotBlank
	private String strParam;

	...
```

2.在dubbo服务的provider/consumer端配置校验开关

dubbo-provider.xml:

```xml
<dubbo:service ref="validatedService" interface="com.x.x.IValidatedService" validation="true"/>
```

dubbo-consumer.xml:

```xml
<dubbo:reference id="validatedService" interface="com.x.x.IValidatedService" validation="true"/>
```

