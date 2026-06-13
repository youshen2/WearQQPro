### WearQQ Pro

基于QQPro V1.5.1
基于NWear-QQ最终版2

### 特别鸣谢

AI Life ➡️ QQ Max
java30433 ➡️ QQPro
|huanli233|

### 关于ApkMixin工具
```
@Mixin
class ExampleHook : TargetClass() { ... }
```
会将`ExampleHook`类内所有`override`的方法的方法体替换到hook的`TargetClass`内，使用`super.fun(...)`可以调用类内原有的方法
你可以在src内的其他任意地方把任意`TargetClass`对象**强转**为`ExampleHook`并调用`ExampleHook`类内定义的其他方法或是字段。非`override`的方法和字段会被复制进hook后的TargetClass内。但是要注意目前**不支持构造函数Hook**，这意味着**添加字段不能有初始值**

我不建议在Hook类内添加过多的东西，建议仅添加必要的代码。

静态方法Hook使用：
```
@Mixin
object ExampleStatic : TargetClass() {
  @StaticHook
  @JvmStatic
  fun targetMethod_(...) {...}
}
```
要点：定义为`object`类，方法带上`JvmStatic`注解，方法名后添加**一个下划线**（否则重复声明无法通过编译）

### 更新日志
- 自己看commit历史