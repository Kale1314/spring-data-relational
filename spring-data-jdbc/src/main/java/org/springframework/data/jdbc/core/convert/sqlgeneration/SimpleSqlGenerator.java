/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.data.jdbc.core.convert.sqlgeneration;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.convert.Identifier;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.QueryMapper;
import org.springframework.data.jdbc.repository.support.SimpleJdbcRepository;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.mapping.PersistentPropertyPathExtension;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.*;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;
import org.springframework.data.util.Lazy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Generates SQL statements to be used by {@link SimpleJdbcRepository}
 *
 * @author Jens Schauder
 * @author Yoichi Imai
 * @author Bastian Wilhelm
 * @author Oleksandr Kucher
 * @author Mark Paluch
 * @author Tom Hombergs
 * @author Tyler Van Gorder
 * @author Milan Milanov
 * @author Myeonghyeon Lee
 * @author Mikhail Polivakha
 * @author Chirag Tailor
 * @author Diego Krupitza
 */
class SimpleSqlGenerator extends WritingSqlGenerator {

	private final Lazy<String> findOneSql = Lazy.of(this::createFindOneSql);
	private final Lazy<String> findAllSql = Lazy.of(this::createFindAllSql);
	private final Lazy<String> findAllInListSql = Lazy.of(this::createFindAllInListSql);

	private final Lazy<String> existsSql = Lazy.of(this::createExistsSql);
	private final Lazy<String> countSql = Lazy.of(this::createCountSql);
	private final QueryMapper queryMapper;
	private final Dialect dialect;

	/**
	 * Create a new {@link SimpleSqlGenerator} given {@link RelationalMappingContext} and
	 * {@link RelationalPersistentEntity}.
	 *
	 * @param mappingContext must not be {@literal null}.
	 * @param converter must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 * @param dialect must not be {@literal null}.
	 */
	SimpleSqlGenerator(RelationalMappingContext mappingContext, JdbcConverter converter,
			RelationalPersistentEntity<?> entity, Dialect dialect) {
		super(entity, mappingContext, converter, dialect);

		this.queryMapper = new QueryMapper(dialect, converter);
		this.dialect = dialect;
	}

	@Override
	public String getFindAllInList() {
		return findAllInListSql.get();
	}

	@Override
	public String getFindAll() {
		return findAllSql.get();
	}

	@Override
	public String getFindAll(Sort sort) {
		return render(selectBuilder(Collections.emptyList(), sort, Pageable.unpaged()).build());
	}

	@Override
	public String getFindAll(Pageable pageable) {
		return render(selectBuilder(Collections.emptyList(), pageable.getSort(), pageable).build());
	}

	@Override
	public String getFindAllByProperty(Identifier parentIdentifier, @Nullable SqlIdentifier keyColumn, boolean ordered) {

		Assert.isTrue(keyColumn != null || !ordered,
				"If the SQL statement should be ordered a keyColumn to order by must be provided");

		Table table = getTable();

		SelectBuilder.SelectWhere builder = selectBuilder( //
				keyColumn == null //
						? Collections.emptyList() //
						: Collections.singleton(keyColumn) //
		);

		Condition condition = buildConditionForBackReference(parentIdentifier, table);
		SelectBuilder.SelectWhereAndOr withWhereClause = builder.where(condition);

		Select select = ordered //
				? withWhereClause.orderBy(table.column(keyColumn).as(keyColumn)).build() //
				: withWhereClause.build();

		return render(select);
	}

	private Condition buildConditionForBackReference(Identifier parentIdentifier, Table table) {

		Condition condition = null;
		for (SqlIdentifier backReferenceColumn : parentIdentifier.toMap().keySet()) {

			Condition newCondition = table.column(backReferenceColumn).isEqualTo(getBindMarker(backReferenceColumn));
			condition = condition == null ? newCondition : condition.and(newCondition);
		}

		Assert.state(condition != null, "We need at least one condition");

		return condition;
	}

	@Override
	public String getExists() {
		return existsSql.get();
	}

	@Override
	public String getFindOne() {
		return findOneSql.get();
	}

	@Override
	public String getAcquireLockById(LockMode lockMode) {
		return this.createAcquireLockById(lockMode);
	}

	@Override
	public String getAcquireLockAll(LockMode lockMode) {
		return this.createAcquireLockAll(lockMode);
	}

	@Override
	public String getCount() {
		return countSql.get();
	}

	private String createFindOneSql() {

		Select select = selectBuilder().where(getIdColumn().isEqualTo(getBindMarker(ID_SQL_PARAMETER))) //
				.build();

		return render(select);
	}

	private String createAcquireLockById(LockMode lockMode) {

		Table table = this.getTable();

		Select select = StatementBuilder //
				.select(getIdColumn()) //
				.from(table) //
				.where(getIdColumn().isEqualTo(getBindMarker(ID_SQL_PARAMETER))) //
				.lock(lockMode) //
				.build();

		return render(select);
	}

	private String createAcquireLockAll(LockMode lockMode) {

		Table table = this.getTable();

		Select select = StatementBuilder //
				.select(getIdColumn()) //
				.from(table) //
				.lock(lockMode) //
				.build();

		return render(select);
	}

	private String createFindAllSql() {
		return render(selectBuilder().build());
	}

	private SelectBuilder.SelectWhere selectBuilder() {
		return selectBuilder(Collections.emptyList());
	}

	private SelectBuilder.SelectWhere selectBuilder(Collection<SqlIdentifier> keyColumns) {

		Table table = getTable();

		List<Expression> columnExpressions = new ArrayList<>();

		List<Join> joinTables = new ArrayList<>();
		for (PersistentPropertyPath<RelationalPersistentProperty> path : mappingContext
				.findPersistentPropertyPaths(entity.getType(), p -> true)) {

			PersistentPropertyPathExtension extPath = new PersistentPropertyPathExtension(mappingContext, path);

			// add a join if necessary
			Join join = getJoin(extPath);
			if (join != null) {
				joinTables.add(join);
			}

			Column column = getColumn(extPath);
			if (column != null) {
				columnExpressions.add(column);
			}
		}

		for (SqlIdentifier keyColumn : keyColumns) {
			columnExpressions.add(table.column(keyColumn).as(keyColumn));
		}

		SelectBuilder.SelectAndFrom selectBuilder = StatementBuilder.select(columnExpressions);
		SelectBuilder.SelectJoin baseSelect = selectBuilder.from(table);

		for (Join join : joinTables) {
			baseSelect = baseSelect.leftOuterJoin(join.joinTable).on(join.joinColumn).equals(join.parentId);
		}

		return (SelectBuilder.SelectWhere) baseSelect;
	}

	private SelectBuilder.SelectOrdered selectBuilder(Collection<SqlIdentifier> keyColumns, Sort sort,
			Pageable pageable) {

		SelectBuilder.SelectOrdered sortable = this.selectBuilder(keyColumns);
		sortable = applyPagination(pageable, sortable);
		return sortable.orderBy(extractOrderByFields(sort));

	}

	private SelectBuilder.SelectOrdered applyPagination(Pageable pageable, SelectBuilder.SelectOrdered select) {

		if (!pageable.isPaged()) {
			return select;
		}

		Assert.isTrue(select instanceof SelectBuilder.SelectLimitOffset,
				() -> String.format("Can't apply limit clause to statement of type %s", select.getClass()));

		SelectBuilder.SelectLimitOffset limitable = (SelectBuilder.SelectLimitOffset) select;
		SelectBuilder.SelectLimitOffset limitResult = limitable.limitOffset(pageable.getPageSize(), pageable.getOffset());

		Assert.state(limitResult instanceof SelectBuilder.SelectOrdered, String.format(
				"The result of applying the limit-clause must be of type SelectOrdered in order to apply the order-by-clause but is of type %s.",
				select.getClass()));

		return (SelectBuilder.SelectOrdered) limitResult;
	}

	/**
	 * Create a {@link Column} for {@link PersistentPropertyPathExtension}.
	 *
	 * @param path the path to the column in question.
	 * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
	 */
	@Nullable
	Column getColumn(PersistentPropertyPathExtension path) {

		// an embedded itself doesn't give an column, its members will though.
		// if there is a collection or map on the path it won't get selected at all, but it will get loaded with a separate
		// select
		// only the parent path is considered in order to handle arrays that get stored as BINARY properly
		if (path.isEmbedded() || path.getParentPath().isMultiValued()) {
			return null;
		}

		if (path.isEntity()) {

			// Simple entities without id include there backreference as an synthetic id in order to distinguish null entities
			// from entities with only null values.

			if (path.isQualified() //
					|| path.isCollectionLike() //
					|| path.hasIdProperty() //
			) {
				return null;
			}

			return super.getReverseColumn(path);
		}

		return super.doGetColumn(path);
	}

	@Nullable
	Join getJoin(PersistentPropertyPathExtension path) {

		if (!path.isEntity() || path.isEmbedded() || path.isMultiValued()) {
			return null;
		}

		Table currentTable = super.getTable(path);

		PersistentPropertyPathExtension idDefiningParentPath = path.getIdDefiningParentPath();
		Table parentTable = super.getTable(idDefiningParentPath);

		return new Join( //
				currentTable, //
				currentTable.column(path.getReverseColumnName()), //
				parentTable.column(idDefiningParentPath.getIdColumnName()) //
		);
	}

	private String createFindAllInListSql() {

		Select select = selectBuilder().where(getIdColumn().in(getBindMarker(IDS_SQL_PARAMETER))).build();

		return render(select);
	}

	private String createExistsSql() {

		Table table = getTable();

		Select select = StatementBuilder //
				.select(Functions.count(getIdColumn())) //
				.from(table) //
				.where(getIdColumn().isEqualTo(getBindMarker(ID_SQL_PARAMETER))) //
				.build();

		return render(select);
	}

	private String createCountSql() {

		Table table = getTable();

		Select select = StatementBuilder //
				.select(Functions.count(Expressions.asterisk())) //
				.from(table) //
				.build();

		return render(select);
	}

	private String render(Select select) {
		return this.sqlRenderer.render(select);
	}

	private List<OrderByField> extractOrderByFields(Sort sort) {

		return sort.stream() //
				.map(this::orderToOrderByField) //
				.collect(Collectors.toList());
	}

	private OrderByField orderToOrderByField(Sort.Order order) {

		SqlIdentifier columnName = this.entity.getRequiredPersistentProperty(order.getProperty()).getColumnName();
		Column column = Column.create(columnName, this.getTable());
		return OrderByField.from(column, order.getDirection()).withNullHandling(order.getNullHandling());
	}

	/**
	 * Constructs a single sql query that performs select based on the provided query. Additional the bindings for the
	 * where clause are stored after execution into the <code>parameterSource</code>
	 *
	 * @param query the query to base the select on. Must not be null
	 * @param parameterSource the source for holding the bindings
	 * @return a non null query string.
	 */
	public String selectByQuery(Query query, MapSqlParameterSource parameterSource) {

		Assert.notNull(parameterSource, "parameterSource must not be null");

		SelectBuilder.SelectWhere selectBuilder = selectBuilder();

		Select select = applyQueryOnSelect(query, parameterSource, selectBuilder) //
				.build();

		return render(select);
	}

	/**
	 * Constructs a single sql query that performs select based on the provided query and pagination information.
	 * Additional the bindings for the where clause are stored after execution into the <code>parameterSource</code>
	 *
	 * @param query the query to base the select on. Must not be null.
	 * @param pageable the pageable to perform on the select.
	 * @param parameterSource the source for holding the bindings.
	 * @return a non null query string.
	 */
	public String selectByQuery(Query query, MapSqlParameterSource parameterSource, Pageable pageable) {

		Assert.notNull(parameterSource, "parameterSource must not be null");

		SelectBuilder.SelectWhere selectBuilder = selectBuilder();

		// first apply query and then pagination. This means possible query sorting and limiting might be overwritten by the
		// pagination. This is desired.
		SelectBuilder.SelectOrdered selectOrdered = applyQueryOnSelect(query, parameterSource, selectBuilder);
		selectOrdered = applyPagination(pageable, selectOrdered);
		selectOrdered = selectOrdered.orderBy(extractOrderByFields(pageable.getSort()));

		Select select = selectOrdered.build();
		return render(select);
	}

	/**
	 * Constructs a single sql query that performs select count based on the provided query for checking existence.
	 * Additional the bindings for the where clause are stored after execution into the <code>parameterSource</code>
	 *
	 * @param query the query to base the select on. Must not be null
	 * @param parameterSource the source for holding the bindings
	 * @return a non null query string.
	 */
	public String existsByQuery(Query query, MapSqlParameterSource parameterSource) {

		SelectBuilder.SelectJoin baseSelect = getExistsSelect();

		Select select = applyQueryOnSelect(query, parameterSource, (SelectBuilder.SelectWhere) baseSelect) //
				.build();

		return render(select);
	}

	/**
	 * Constructs a single sql query that performs select count based on the provided query. Additional the bindings for
	 * the where clause are stored after execution into the <code>parameterSource</code>
	 *
	 * @param query the query to base the select on. Must not be null
	 * @param parameterSource the source for holding the bindings
	 * @return a non null query string.
	 */
	public String countByQuery(Query query, MapSqlParameterSource parameterSource) {

		Expression countExpression = Expressions.just("1");
		SelectBuilder.SelectJoin baseSelect = getSelectCountWithExpression(countExpression);

		Select select = applyQueryOnSelect(query, parameterSource, (SelectBuilder.SelectWhere) baseSelect) //
				.build();

		return render(select);
	}

	/**
	 * Generates a {@link org.springframework.data.relational.core.sql.SelectBuilder.SelectJoin} with a
	 * <code>COUNT(...)</code> where the <code>countExpressions</code> are the parameters of the count.
	 *
	 * @return a non-null {@link org.springframework.data.relational.core.sql.SelectBuilder.SelectJoin} that joins all the
	 *         columns and has only a count in the projection of the select.
	 */
	private SelectBuilder.SelectJoin getExistsSelect() {

		Table table = getTable();

		SelectBuilder.SelectJoin baseSelect = StatementBuilder //
				.select(dialect.getExistsFunction()) //
				.from(table);

		// add possible joins
		for (PersistentPropertyPath<RelationalPersistentProperty> path : mappingContext
				.findPersistentPropertyPaths(entity.getType(), p -> true)) {

			PersistentPropertyPathExtension extPath = new PersistentPropertyPathExtension(mappingContext, path);

			// add a join if necessary
			Join join = getJoin(extPath);
			if (join != null) {
				baseSelect = baseSelect.leftOuterJoin(join.joinTable).on(join.joinColumn).equals(join.parentId);
			}
		}
		return baseSelect;
	}

	/**
	 * Generates a {@link org.springframework.data.relational.core.sql.SelectBuilder.SelectJoin} with a
	 * <code>COUNT(...)</code> where the <code>countExpressions</code> are the parameters of the count.
	 *
	 * @param countExpressions the expression to use as count parameter.
	 * @return a non-null {@link org.springframework.data.relational.core.sql.SelectBuilder.SelectJoin} that joins all the
	 *         columns and has only a count in the projection of the select.
	 */
	private SelectBuilder.SelectJoin getSelectCountWithExpression(Expression... countExpressions) {

		Assert.notNull(countExpressions, "countExpressions must not be null");
		Assert.state(countExpressions.length >= 1, "countExpressions must contain at least one expression");

		Table table = getTable();

		SelectBuilder.SelectJoin baseSelect = StatementBuilder //
				.select(Functions.count(countExpressions)) //
				.from(table);

		// add possible joins
		for (PersistentPropertyPath<RelationalPersistentProperty> path : mappingContext
				.findPersistentPropertyPaths(entity.getType(), p -> true)) {

			PersistentPropertyPathExtension extPath = new PersistentPropertyPathExtension(mappingContext, path);

			// add a join if necessary
			Join join = getJoin(extPath);
			if (join != null) {
				baseSelect = baseSelect.leftOuterJoin(join.joinTable).on(join.joinColumn).equals(join.parentId);
			}
		}
		return baseSelect;
	}

	private SelectBuilder.SelectOrdered applyQueryOnSelect(Query query, MapSqlParameterSource parameterSource,
														   SelectBuilder.SelectWhere selectBuilder) {

		Table table = Table.create(this.entity.getTableName());

		SelectBuilder.SelectOrdered selectOrdered = query //
				.getCriteria() //
				.map(item -> this.applyCriteria(item, selectBuilder, parameterSource, table)) //
				.orElse(selectBuilder);

		if (query.isSorted()) {
			List<OrderByField> sort = this.queryMapper.getMappedSort(table, query.getSort(), entity);
			selectOrdered = selectBuilder.orderBy(sort);
		}

		SelectBuilder.SelectLimitOffset limitable = (SelectBuilder.SelectLimitOffset) selectOrdered;

		if (query.getLimit() > 0) {
			limitable = limitable.limit(query.getLimit());
		}

		if (query.getOffset() > 0) {
			limitable = limitable.offset(query.getOffset());
		}
		return (SelectBuilder.SelectOrdered) limitable;
	}

	SelectBuilder.SelectOrdered applyCriteria(@Nullable CriteriaDefinition criteria,
											  SelectBuilder.SelectWhere whereBuilder, MapSqlParameterSource parameterSource, Table table) {

		return criteria != null //
				? whereBuilder.where(queryMapper.getMappedObject(parameterSource, criteria, table, entity)) //
				: whereBuilder;
	}

	/**
	 * Value object representing a {@code JOIN} association.
	 */
	static final class Join {

		private final Table joinTable;
		private final Column joinColumn;
		private final Column parentId;

		Join(Table joinTable, Column joinColumn, Column parentId) {

			Assert.notNull(joinTable, "JoinTable must not be null");
			Assert.notNull(joinColumn, "JoinColumn must not be null");
			Assert.notNull(parentId, "ParentId must not be null");

			this.joinTable = joinTable;
			this.joinColumn = joinColumn;
			this.parentId = parentId;
		}

		Table getJoinTable() {
			return this.joinTable;
		}

		Column getJoinColumn() {
			return this.joinColumn;
		}

		Column getParentId() {
			return this.parentId;
		}

		@Override
		public boolean equals(Object o) {

			if (this == o)
				{
				return true;
			}
			if (o == null || getClass() != o.getClass())
				{
				return false;
			}
			Join join = (Join) o;
			return joinTable.equals(join.joinTable) && joinColumn.equals(join.joinColumn) && parentId.equals(join.parentId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(joinTable, joinColumn, parentId);
		}

		@Override
		public String toString() {

			return "Join{" + //
					"joinTable=" + joinTable + //
					", joinColumn=" + joinColumn + //
					", parentId=" + parentId + //
					'}';
		}
	}

}
