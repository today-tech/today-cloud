# Remoting Protocol

使用 RSocket 协议

原始代码来自 https://github.com/channel/rsocket-java.git cff5cdbb16da6393efc04d8f0b80793e54f79026
1.1.5 版本

It enables the following symmetric interaction models via async message passing over a single connection:

- request/response (stream of 1)
- request/stream (finite/infinite stream of many)
- fire-and-forget (no response)
- channel (bi-directional streams)

It also supports connection resumption to allow resuming long-lived streams across different transport connections. This is particularly useful for mobile<->server communication when network connections drop, switch, and reconnect frequently.

Artifacts include:

- [Protocol](Protocol.md)
