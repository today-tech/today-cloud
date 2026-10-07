# 仓库工作约定

## 格式与文档（必须遵守）

- 保留已有文件完整的版权头及 Apache 2.0 声明；重写文件不能丢失。新 Java 文件使用相邻文件的完整声明格式。
- 保留已有 `@author` 和 `@since` 的原始内容，重构、改名不修改作者或创建时间。新增类型补齐作者和创建日期，遵循相邻文件格式。
- 所有 Javadoc 使用多行 `/** ... */`，包括只有一句描述、一个 `@return` 或 `{@inheritDoc}` 的注释。禁止 `/** @return ... */`、`/** ... */` 这类单行 Javadoc。
- 保持原有多行代码块：方法、构造器、`if`、`try`、`catch`、`finally`、多语句 lambda 不压成一行；方法和字段之间保留空行。
- `.editorconfig`：UTF-8，Java/Groovy 使用 2 空格缩进和 LF，续行缩进为 8。保持原有 import 分组、排序和链式调用换行；不要顺便重排无关代码。
- Javadoc 和源码注释使用英文；模块使用说明、设计文章使用中文，技术名称与代码保留英文。
- Javadoc 说明真实生命周期：构造结果不等于执行完成，观察结果不应启动第二次调用；注明取消、线程与 buffer 所有权，不用过时的签名示例。

## 构建与验证

- 使用根目录 `./gradlew`。构建 toolchain 是 JDK 25，默认字节码 `--release 17`；README 的 JDK 17 标识不能当作构建 JDK 要求。配置见 `buildSrc/src/main/java/infra/cloud/JavaConventions.java`。
- 模块测试：`./gradlew :today-service-provider:test --console=plain`；单类测试：`./gradlew :today-service-provider:test --tests infra.cloud.provider.ServiceInterceptorTests --console=plain`。
- 跨结果/API/两端调用链的修改：`./gradlew :today-service-api:test :today-service-client:test :today-service-provider:test --console=plain`。
- 文档检查：`./gradlew :today-service-provider:javadoc --console=plain`，另运行 `git diff --check`。Javadoc 配置了 `failOnError = false`，任务成功不代表没有文档问题；外部链接可能让生成耗时较长。
- 测试只包含 `*Tests` / `*Test` 类，使用 JUnit Platform；默认 Netty leak detection 为 `paranoid`。沿用 Gradle 测试配置，不手动另配 JVM opens。
- 仓库优先解析 `mavenLocal()`，snapshot/dynamic 依赖缓存时间为零。依赖已缓存时可加 `--offline`；它不会阻止 Javadoc 工具访问外部文档链接。
- `infra.test.app.context.InfraTest` 的应用上下文测试需要模块测试 classpath 包含 `cn.taketoday:infra-app`；`infra-test` 单独不足。

## 代码边界

- 已有 `.codegraph/` 索引，定位符号及调用链优先使用 CodeGraph；配置和文档用直接读取。
- `today-service-api` 放共享请求元数据和结果契约；client/provider 各有自己的拦截链与上下文。两端同名 `InterceptorChain` / `DefaultInterceptorChain` 位于不同包，避免误导入。
- provider 调用接线在 `ServiceProviderAutoConfiguration`；`ServiceMethodResolver` 构建并缓存 `InvocableMethod`，方法预绑定 `ReturnValueHandler`。调用级参数、attributes、metadata 属于 `RemoteRequest`，不能放进共享方法或不可变链节点。
- 跨进程 `Metadata` 与本地 `AttributeAccessor` 分开。服务端在释放入站 Payload 前解码元数据，保存独立快照，不留已释放 ByteBuf 的 slice；本地 attributes 不自动发送。
- provider 边界与扩展方式见 `today-service-provider/README.md`。公开接口不暴露 Mono 并不意味着默认实现已移除 Reactor；不要把未实现的流式接入或依赖移除写成完成状态。
