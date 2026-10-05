# 服务端调用

request-response 调用流程将请求解码、方法解析、服务执行和响应编码分开：

```text
RequestDeserializer -> ServiceMethodResolver -> RemoteRequest
                                                   |
                                       ServiceRequestExecutor
                                                   |
                                          ReturnValueHandler
                                                   |
                                           ResponseSerializer
```

`ServiceChannelHandler` 将上述流程接入当前的 Remoting 传输层。它负责消费并释放
入站 payload、委托执行服务调用、编码返回值或空值响应，并通过统一的响应异常入口处理错误。

## 独立调用

其他传输实现可以直接解析方法并构造已解码的请求：

```java
InvocableMethod method = methodResolver.resolve("com.example.UserService", "findById",
    new String[] { "long" });
RemoteRequest request = new RemoteRequest(method, new Object[] { 1L }, method.getServiceObject());
Publisher<Object> result = requestExecutor.execute(request);
```

执行器和返回值处理接口使用 Reactive Streams 的 `Publisher`，不暴露 Reactor 的
`Mono` 或 Remoting 的 `Payload`。结果最多包含一个值；null 和 void 以无值完成表示。
每次订阅都会启动一次新的调用，取消会传播到底层异步任务。
订阅者负责管理需求和取消，并保证相关信号符合 Reactive Streams 的串行化要求。

默认执行器内部仍使用 Reactor，通过 bounded-elastic 调度服务执行，隔离阻塞方法。
手动构造时可以传入自定义 Scheduler；调度器由外部管理生命周期，执行器不会释放它。
替换执行器后可以使用其他执行引擎，无需同时修改响应编码。
当前实现完成了接口边界解耦，模块本身仍依赖 Reactor。

## 扩展点

### 跨进程元数据

`Metadata` 用于显式传递调用元数据，与 `AttributeAccessor` 的本地属性独立。
支持 UTF-8 文本、二进制值和同名多值；`entries()` 按添加顺序访问所有条目，
`get(name)` / `getBinary(name)` 返回首个对应类型的值。

客户端通过 `ClientInterceptor` 写入：

```java
ClientInterceptor propagation = invocation -> {
  invocation.getMetadata().add("traceparent", traceparent);
  invocation.getMetadata().add("tenant-id", tenantId);
  invocation.getMetadata().addBinary("custom-bin", binaryValue);
  return invocation.proceed();
};
```

服务端通过 `ServiceInterceptor` 读取：

```java
ServiceInterceptor propagation = invocation -> {
  String traceparent = invocation.getRequest().getMetadata().get("traceparent");
  // 提取远端上下文并创建服务端 Span；本地 Span 对象放在 attributes 中。
  invocation.setAttribute("serverSpan", serverSpan);
  return invocation.proceed();
};
```

客户端在构造每次发送的 payload 时获取元数据快照；服务端在释放 payload 前完成解码。
`RemoteRequest` 持有不可变且独立于 ByteBuf 的快照，跨线程执行及参数数组复制会保留它。
本地 attributes 不会自动发送，没有元数据时不分配 metadata buffer、不设置 metadata 标志。

默认 V1 格式为：版本字节，随后是若干条目；每项包含 varint 类型标签、可选的名称长度与
UTF-8 名称、varint 值长度和数据。标签最低位区分文本与二进制，其他位为名称编号。
`traceparent`、`tracestate`、`baggage`、`tenant-id`、`authorization` 使用固定编号 1～5，
自定义名称使用编号 0 并发送名称。不发送条目数量，使用 metadata 区域的边界结束解析。
默认限制为 8 KiB、64 项，截断、非法版本和未知编号会被拒绝。
该格式是应用 RPC 元数据格式，不是 RSocket Composite Metadata。

可在客户端与服务端提供 `MetadataCodec` Bean 替换格式或配置限制，双方必须使用
相同的编码契约。手动创建客户端调用器使用 `setMetadataCodec()`；服务端适配器有对应构造器。

框架提供元数据传输，不会自动生成 Span 或安装 OpenTelemetry。trace 传播应由相应拦截器
对接 tracing propagator，包含父 Span 信息、采样标志和可选的 tracestate，而不只是 traceId。
当前客户端拦截器在代理调用时执行，延迟订阅场景需要由 tracing 集成显式处理订阅时上下文；
跨进程元数据传递本身不等于 ThreadLocal、MDC 或 Reactor Context 的自动恢复。

### 服务调用拦截器

注册有序的 `ServiceInterceptor` Bean 即可拦截已解码的服务调用，第一个拦截器位于最外层。
`ProviderInvocation` 提供方法、参数、请求以及调用链内共享的属性。
它直接继承 `AttributeAccessor`，可使用 `invocation.setAttribute(name, value)` 和
`invocation.getAttribute(name)` 访问属性；`getAttributes()` 返回底层属性 Map。
不调用 `proceed()` 而直接返回结果，可以短路后续拦截器和服务方法。
结果是已展开的业务值，而不是 Future 或 Mono 包装对象。
配置拦截器时，每次订阅都会创建新的调用链，并浅拷贝参数数组，因此数组元素替换不会影响
其他订阅；参数引用的对象本身不会被复制。

```java
ServiceInterceptor timing = invocation -> {
  long started = System.nanoTime();
  return Mono.from(invocation.proceed())
      .doFinally(signal -> recordDuration(System.nanoTime() - started, signal));
};
```

接口使用 Reactive Streams，因此拦截器可以使用任意符合其规范的 Publisher 实现。
上面的示例使用 Reactor 的生命周期操作符。异步成功、错误和取消应通过 Publisher 观察；
包围 `proceed()` 的 `finally` 块只能观察 Publisher 的构造过程，不能表示异步调用结束。
每层调用延续只允许一次 `proceed()` 和一次订阅；重复订阅进行隐式重试会被拒绝，避免重复
产生业务副作用。请求解码和响应编码位于拦截链之外。
拦截器必须线程安全，最多返回一个值，并在包装后续调用时保留取消传播。

手动构造执行器时，可传入 `List<ServiceInterceptor>` 和可选的调度器。

### 方法解析与执行策略

- 提供 `ServiceMethodResolver` Bean 可替换方法解析逻辑。默认解析器按服务对象缓存不可变
  方法签名，仅允许调用导出接口元数据中包含的方法。
- 提供 `ServiceRequestExecutor` Bean 可替换调用执行和调度逻辑。
- 提供有序的 `ReturnValueHandler` Bean 可适配应用返回值类型。自定义处理器优先于默认的
  异步处理器和普通值处理器。处理器不依赖响应序列化或传输缓冲区。

`ReturnValueHandlerComposite` 在构造 `InvocableMethod` 时选择处理器。
缓存的方法只持有最终选中的处理器，执行时不再扫描策略列表。
应在解析方法之前完成处理器配置，并保证处理器线程安全。
修改处理器配置后，需要重新构建解析器及其方法缓存。
手动构造解析器时，可以通过构造器传入已配置的组合处理器。
执行器负责调度，处理器配置由方法解析阶段管理。

基于 methodId 的请求编解码器可以跳过名称解析，直接构造相同的 `RemoteRequest`。
当前请求报文格式保持不变。

当前执行契约适用于 request-response。多值和双向流调用需要独立的流式执行契约，
不能通过截取流中的第一个结果来代替。取消调用不会回滚已发生的业务副作用。
