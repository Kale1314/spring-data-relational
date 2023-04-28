/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.relational.core.query;

import org.springframework.data.domain.*;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Query object representing {@link Criteria}, columns, {@link Sort}, and limit/offset for a SQL query. {@link Query} is
 * created with a fluent API creating immutable objects.
 *
 * @author Mark Paluch
 * @since 2.0
 * @see Criteria
 * @see Sort
 * @see Pageable
 */
public class Query {

	private static final int NO_LIMIT = -1;

	private final @Nullable CriteriaDefinition criteria;

	private final List<SqlIdentifier> columns;

	@Nullable
	private final Joins joins;

	@Nullable
	private final Groups groups;
	private final Sort sort;
	private final int limit;
	private final long offset;

	/**
	 * Static factory method to create a {@link Query} using the provided {@link CriteriaDefinition}.
	 *
	 * @param criteria must not be {@literal null}.
	 * @return a new {@link Query} for the given {@link Criteria}.
	 */
	public static Query query(CriteriaDefinition criteria) {
		return new Query(criteria);
	}

	/**
	 * Creates a new {@link Query} using the given {@link Criteria}.
	 *
	 * @param criteria must not be {@literal null}.
	 */
	private Query(@Nullable CriteriaDefinition criteria) {
		this(criteria, Collections.emptyList(), Sort.unsorted(), NO_LIMIT, NO_LIMIT, null, null);
	}

	private Query(@Nullable CriteriaDefinition criteria, List<SqlIdentifier> columns, Sort sort,
				  int limit, long offset, @Nullable Joins joins, @Nullable Groups groups) {

		this.criteria = criteria;
		this.columns = columns;
		this.sort = sort;
		this.limit = limit;
		this.offset = offset;
		this.joins = joins;
		this.groups = groups;
	}

	/**
	 * Create a new empty {@link Query}.
	 *
	 * @return
	 */
	public static Query empty() {
		return new Query(null);
	}

	/**
	 * Add columns to the query.
	 *
	 * @param columns
	 * @return a new {@link Query} object containing the former settings with {@code columns} applied.
	 */
	public Query columns(String... columns) {

		Assert.notNull(columns, "Columns must not be null");

		return withColumns(Arrays.stream(columns).map(SqlIdentifier::unquoted).collect(Collectors.toList()));
	}

	/**
	 * Add columns to the query.
	 *
	 * @param columns
	 * @return a new {@link Query} object containing the former settings with {@code columns} applied.
	 */
	public Query columns(Collection<String> columns) {

		Assert.notNull(columns, "Columns must not be null");

		return withColumns(columns.stream().map(SqlIdentifier::unquoted).collect(Collectors.toList()));
	}

	/**
	 * Add columns to the query.
	 *
	 * @param columns
	 * @return a new {@link Query} object containing the former settings with {@code columns} applied.
	 * @since 1.1
	 */
	public Query columns(SqlIdentifier... columns) {

		Assert.notNull(columns, "Columns must not be null");

		return withColumns(Arrays.asList(columns));
	}

	/**
	 * Add columns to the query.
	 *
	 * @param columns
	 * @return a new {@link Query} object containing the former settings with {@code columns} applied.
	 */
	private Query withColumns(Collection<SqlIdentifier> columns) {

		Assert.notNull(columns, "Columns must not be null");

		List<SqlIdentifier> newColumns = new ArrayList<>(this.columns);
		newColumns.addAll(columns);
		return new Query(this.criteria, newColumns, this.sort, this.limit, offset,this.joins,this.groups);
	}

	/**
	 * Set number of rows to skip before returning results.
	 *
	 * @param offset
	 * @return a new {@link Query} object containing the former settings with {@code offset} applied.
	 */
	public Query offset(long offset) {
		return new Query(this.criteria, this.columns, this.sort, this.limit, offset,this.joins,this.groups);
	}

	/**
	 * Limit the number of returned documents to {@code limit}.
	 *
	 * @param limit
	 * @return a new {@link Query} object containing the former settings with {@code limit} applied.
	 */
	public Query limit(int limit) {
		return new Query(this.criteria, this.columns, this.sort, limit, this.offset,this.joins,this.groups);
	}

	/**
	 * Set the given pagination information on the {@link Query} instance. Will transparently set {@code offset} and
	 * {@code limit} as well as applying the {@link Sort} instance defined with the {@link Pageable}.
	 *
	 * @param pageable
	 * @return a new {@link Query} object containing the former settings with {@link Pageable} applied.
	 */
	public Query with(Pageable pageable) {
		if (pageable.isUnpaged()) {
			return this;
		}

		assertNoCaseSort(pageable.getSort());

		if (pageable instanceof PageExtender pageExtender && pageExtender.getBoundary().notEmpty()) {
			CriteriaDefinition boundaryCriteria = pageExtender.getBoundary().toCriteria();
			return new Query(CriteriaDefinition.from(this.criteria, boundaryCriteria), this.columns, this.sort.and(pageable.getSort()), pageable.getPageSize(),
					pageable.getOffset(), this.joins, this.groups);
		}

		return new Query(this.criteria, this.columns, this.sort.and(pageable.getSort()), pageable.getPageSize(),
				pageable.getOffset(), this.joins, this.groups);
	}

	public Query withBoundary(Boundary boundary) {
		if (!boundary.notEmpty()) {
			return this;
		}
		CriteriaDefinition boundaryCriteria = boundary.toCriteria();
		return new Query(CriteriaDefinition.from(this.criteria, boundaryCriteria), this.columns, this.sort, limit, offset, this.joins, this.groups);
	}

	public Query groupBy(Groups groups) {
		return new Query(this.criteria, this.columns, this.sort.and(sort), this.limit, this.offset, this.joins, groups);
	}

	public Query join(Joins joins) {
		return new Query(this.criteria, this.columns, this.sort.and(sort), this.limit, this.offset, joins, this.groups);
	}

	/**
	 * Add a {@link Sort} to the {@link Query} instance.
	 *
	 * @param sort
	 * @return a new {@link Query} object containing the former settings with {@link Sort} applied.
	 */
	public Query sort(Sort sort) {

		Assert.notNull(sort, "Sort must not be null");

		if (sort.isUnsorted()) {
			return this;
		}

		assertNoCaseSort(sort);

		return new Query(this.criteria, this.columns, this.sort.and(sort), this.limit, this.offset,this.joins,this.groups);
	}

	/**
	 * Return the {@link Criteria} to be applied.
	 *
	 * @return
	 */
	public Optional<CriteriaDefinition> getCriteria() {
		return Optional.ofNullable(this.criteria);
	}

	/**
	 * Return the columns that this query should project.
	 *
	 * @return
	 */
	public List<SqlIdentifier> getColumns() {
		return columns;
	}

	/**
	 * Return {@literal true} if the {@link Query} has a sort parameter.
	 *
	 * @return {@literal true} if sorted.
	 * @see Sort#isSorted()
	 */
	public boolean isSorted() {
		return sort.isSorted();
	}

	public Sort getSort() {
		return sort;
	}

	/**
	 * Return the number of rows to skip.
	 *
	 * @return
	 */
	public long getOffset() {
		return this.offset;
	}

	/**
	 * Return the maximum number of rows to be return.
	 *
	 * @return
	 */
	public int getLimit() {
		return this.limit;
	}

	/**
	 * Return whether the query has a limit.
	 *
	 * @return {@code true} if a limit is set.
	 * @see #getLimit()
	 * @since 3.0
	 */
	public boolean isLimited() {
		return getLimit() != NO_LIMIT;
	}


	public boolean hasGroups() {
		return this.groups != null && this.groups.notEmpty();
	}

	public Groups getGroups() {
		return this.groups;
	}

	public boolean hasJoin() {
		return this.joins != null && this.joins.notEmpty();
	}

	public Joins getJoins() {
		return this.joins;
	}

	private static void assertNoCaseSort(Sort sort) {

		for (Sort.Order order : sort) {
			if (order.isIgnoreCase()) {
				throw new IllegalArgumentException(String.format(
						"Given sort contained an Order for %s with ignore case;" + "Ignore case sorting is not supported",
						order.getProperty()));
			}
		}
	}
}
