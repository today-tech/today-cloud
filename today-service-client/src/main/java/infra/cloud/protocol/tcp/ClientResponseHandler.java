/*
 * Copyright 2021 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package infra.cloud.protocol.tcp;

import java.util.Set;

import infra.cloud.RpcResponse;
import infra.cloud.core.serialize.ProtostuffUtils;
import infra.cloud.protocol.Connection;
import infra.cloud.protocol.EventHandler;
import infra.cloud.protocol.ProtocolPayload;
import infra.cloud.protocol.RemoteEventType;
import infra.cloud.protocol.ResponsePromise;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2024/9/11 22:10
 */
public class ClientResponseHandler implements EventHandler {

  private static final Logger logger = LoggerFactory.getLogger(ClientResponseHandler.class);

  @Override
  public Set<RemoteEventType> getSupportedEvents() {
    return Set.of(RemoteEventType.RPC_INVOCATION);
  }

  @Override
  public void channelActive(Connection connection) {
    logger.debug("connected");
  }

  @Override
  public void handleEvent(Connection connection, ProtocolPayload payload) {
    ResponsePromise responsePromise = connection.getAndRemovePromise(payload.getRequestId());
    if (responsePromise != null) {
      try {
        if (payload.body != null) {
          var response = ProtostuffUtils.deserialize(payload.body.nioBuffer(), RpcResponse.class);
//            RpcResponse response = serialization.deserialize(new ByteArrayInputStream(payload.body));
          Throwable exception = response.getException();
          if (exception != null) {
            responsePromise.tryFailure(exception);
          }
          else {
            responsePromise.trySuccess(response.getResult());
          }
        }
        else {
          responsePromise.trySuccess(null);
        }
      }
      catch (Exception e) {
        responsePromise.tryFailure(e);
      }
    }
    else {
      logger.debug("RequestId not found: {}", payload.getRequestId());
    }
  }

}
