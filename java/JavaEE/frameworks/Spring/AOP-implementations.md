### 提纲
AOP的概念与原理我们已经很熟悉了，现在我们来总结一下AOP的具体实现方式并在下面写一写代码来验证：   
**静态代理**
    编译期将增强功能代码注入被代理方法，例如：AspectJ   
**动态代理** 
    运行期为目标生成代理对象，例如：JDK动态代理，使用反射机制，要求被代理的目标类必须实现一个接口；CGLIB为被代理对象生成子类增强其功能。
    
#### AspectJ简介与编译期注入
##### AspectJ简介
官方的定义：AspectJ is a seamless aspect-oriented extension to Java.

#### Special Thanks
http://www.importnew.com/24305.html   
https://www.ibm.com/developerworks/cn/java/j-lo-springaopcglib/   
https://blog.csdn.net/dreamrealised/article/details/12885739   
https://blog.csdn.net/innost/article/details/49387395