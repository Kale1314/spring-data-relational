/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.data.r2dbc.repository;

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * R2DBC specific {@link org.springframework.data.repository.Repository} interface with reactive support.
 *
 * @author Mark Paluch
 * @author Stephen Cohen
 * @author Greg Turnquist
 */
@NoRepositoryBean
public interface R2dbcRepository<T, ID> extends ReactiveCrudRepository<T, ID>, ReactiveSortingRepository<T, ID>, ReactiveQueryByExampleExecutor<T> {

    /**
     * 分页查询
     *
     * @param example  参数
     * @param pageable 分页信息
     * @return 分页结果
     */
    <S extends T> Mono<Page<S>> page(Example<S> example, Pageable pageable);


    /**
     * 分页查询
     *
     * @param example         参数
     * @param pageable        分页信息
     * @param queryCustomizer 更多查询参数
     * @return 分页结果
     */
    <S extends T> Mono<Page<S>> page(Example<S> example, Pageable pageable, UnaryOperator<Query> queryCustomizer);


    /**
     * 查询
     *
     * @param example         参数
     * @param queryFunction   参数函数
     * @param queryCustomizer 更多查询参数
     * @return 分页结果
     */
    <S extends T, R, P extends Publisher<R>> P findBy(Example<S> example,
                                                      Function<FluentQuery.ReactiveFluentQuery<S>, P> queryFunction, UnaryOperator<Query> queryCustomizer);


}
