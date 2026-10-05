# Service provider invocation

The request-response invocation pipeline separates wire decoding, method
resolution, service execution, and response encoding:

```text
RequestDeserializer -> ServiceMethodResolver -> RemoteRequest
                                                   |
                                       ServiceRequestExecutor
                                                   |
                                          ReturnValueHandler
                                                   |
                                           ResponseSerializer
```

`ServiceChannelHandler` bridges this pipeline to the current Remoting transport.
It consumes and releases the inbound payload, delegates execution, encodes a value
or null response, and handles errors through one response-error entry point.

## Independent invocation

Another transport can resolve a method and construct a decoded request directly:

```java
InvocableMethod method = methodResolver.resolve("com.example.UserService", "findById",
    new String[] { "long" });
RemoteRequest request = new RemoteRequest(method, new Object[] { 1L }, method.getServiceObject());
Publisher<Object> result = requestExecutor.execute(request);
```

The executor and return-value handler interfaces expose Reactive Streams
`Publisher`, not Reactor `Mono` or Remoting `Payload`. They represent at most one
value; null and void are represented by empty completion. Each subscription starts
a new invocation. Cancellation propagates to the underlying asynchronous work.
The subscriber owns demand and cancellation and must serialize its signals.

The default executor still uses Reactor internally. It schedules service execution
on bounded-elastic to isolate blocking methods. Its scheduler constructor allows
manual scheduling customization; the scheduler is externally owned. A replacement
executor can use another execution engine without changing response encoding.
This is an interface boundary, not removal of Reactor dependencies from the module.

## Extension points

- Provide a `ServiceMethodResolver` bean to replace method resolution. The default
  resolver caches immutable signatures scoped to the service object and only exposes
  methods present in exported interface metadata.
- Provide a `ServiceRequestExecutor` bean to replace invocation and scheduling.
- Provide ordered `ReturnValueHandler` beans to adapt application return types.
  Custom handlers precede reactive and ordinary-value defaults. Handlers do not
  depend on response serialization or transport buffers.

`ReturnValueHandlerComposite` selects the handler when an `InvocableMethod` is
constructed. The cached method retains only the selected handler; execution does
not scan the strategy list. Configure handlers before resolving methods and keep
them thread-safe. Changing handler configuration requires rebuilding the resolver
and its method cache. Manual resolvers accept a configured composite through their
constructor. The executor owns scheduling, not handler configuration.

Alternative method-ID request codecs can bypass name-based resolution and construct
the same `RemoteRequest`. The existing request wire format is unchanged.

This execution contract is for request-response. Multi-value and duplex invocation
need separate streaming execution contracts rather than truncating a stream into
a single response. Cancellation does not roll back service side effects.
