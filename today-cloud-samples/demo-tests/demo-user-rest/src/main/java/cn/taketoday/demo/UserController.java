/*
 * Copyright 2021 - 2023 the original author or authors.
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

package cn.taketoday.demo;

import cn.taketoday.demo.model.User;
import cn.taketoday.demo.service.UserService;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.handler.ResponseEntityExceptionHandler;

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

  @ExceptionHandler(Throwable.class)
  public ProblemDetail errorHandling(Throwable throwable) {
    logger.error("errorHandling", throwable);
    return ProblemDetail.forRawStatusCode(500)
            .withDetail(throwable.getMessage());
  }

}
