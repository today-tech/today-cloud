/*
 * Copyright 2021 - 2026 the TODAY authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.demo;

import cn.taketoday.demo.model.User;
import cn.taketoday.demo.service.UserService;
import infra.http.ProblemDetail;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.GET;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.annotation.RestControllerAdvice;
import infra.web.handler.ResponseEntityExceptionHandler;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2023/9/5 10:06
 */
@RestController
@RestControllerAdvice
@RequestMapping("/api/users")
public class UserController extends ResponseEntityExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

//  public UserController(ServiceProvider serviceProvider) {
//    this.userService = serviceProvider.lookupService(UserService.class);
//  }

  @GET("/{id}")
  public User get(int id) {
    return userService.getById(id);
  }

  @GET("/throwEx")
  public void throwEx() {
    userService.throwEx();
  }

  @ExceptionHandler(Throwable.class)
  public ProblemDetail errorHandling(Throwable throwable) {
    logger.error("errorHandling", throwable);
    return ProblemDetail.forRawStatusCode(500)
            .withDetail(throwable.getMessage());
  }

}
