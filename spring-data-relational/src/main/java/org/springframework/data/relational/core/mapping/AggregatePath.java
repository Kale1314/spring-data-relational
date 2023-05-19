/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.data.relational.core.mapping;

import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class AggregatePath {

	private final RelationalMappingContext context;

	@Nullable private final Class<?> type;

	@Nullable private final PersistentPropertyPath<? extends RelationalPersistentProperty> path;

	public AggregatePath(RelationalMappingContext context, @Nullable Class<?> type,
			@Nullable PersistentPropertyPath<? extends RelationalPersistentProperty> path) {
		this.context = context;

		this.type = type;
		this.path = path;
	}

	public boolean isRoot() {
		return path == null;
	}

	/**
	 * Tests if {@code this} and the argument represent the same path.
	 *
	 * @param path to which this path gets compared. May be {@literal null}.
	 * @return Whence the argument matches the path represented by this instance.
	 */
	public boolean matches(PersistentPropertyPath<RelationalPersistentProperty> path) {
		return this.path == null ? path.isEmpty() : this.path.equals(path);
	}

	/**
	 * The name of the column used to reference the id in the parent table.
	 *
	 * @throws IllegalStateException when called on an empty path.
	 */
	public SqlIdentifier getReverseColumnName() {

		Assert.state(path != null, "Empty paths don't have a reverse column name");

		RelationalPersistentProperty property = path.getLeafProperty();
		System.out.println("leaf prop" + property);
		return property.getReverseColumnName(this);
	}

	/**
	 * Returns the path that has the same beginning but is one segment shorter than this path.
	 *
	 * @return the parent path. Guaranteed to be not {@literal null}.
	 * @throws IllegalStateException when called on an empty path.
	 */
	private AggregatePath getParentPath() {

		if (path == null) {
			throw new IllegalStateException("The parent path of a root path is not defined.");
		}

		if (path.getLength() == 1) {
			return context.getAggregateRootPath(path.getLeafProperty().getOwner().getType());
		}

		return context.getAggregatePath(path.getParentPath());
	}


	/**
	 * The {@link RelationalPersistentEntity} associated with the leaf of this path.
	 *
	 * @return Might return {@literal null} when called on a path that does not represent an entity.
	 */
	@Nullable
	private RelationalPersistentEntity<?> getLeafEntity() {
		return path == null ? context.getRequiredPersistentEntity(type) : context.getPersistentEntity(path.getLeafProperty().getActualType());
	}

	/**
	 * @return {@literal true} if this path represents an entity which has an Id attribute.
	 */
	private boolean hasIdProperty() {

		RelationalPersistentEntity<?> leafEntity = getLeafEntity();
		return leafEntity != null && leafEntity.hasIdProperty();
	}


	/**
	 * Returns the longest ancestor path that has an {@link org.springframework.data.annotation.Id} property.
	 *
	 * @return A path that starts just as this path but is shorter. Guaranteed to be not {@literal null}.
	 */
	public AggregatePath getIdDefiningParentPath() {

		AggregatePath parent = getParentPath();

		if (parent.path == null) {
			return parent;
		}

		if (!parent.hasIdProperty()) {
			return parent.getIdDefiningParentPath();
		}

		return parent;
	}

	/**
	 * The {@link RelationalPersistentEntity} associated with the leaf of this path or throw {@link IllegalStateException}
	 * if the leaf cannot be resolved.
	 *
	 * @return the required {@link RelationalPersistentEntity} associated with the leaf of this path.
	 * @throws IllegalStateException if the persistent entity cannot be resolved.
	 */
	public RelationalPersistentEntity<?> getRequiredLeafEntity() {

		RelationalPersistentEntity<?> entity = getLeafEntity();

		if (entity == null) {

			if (this.path == null) {
				throw new IllegalStateException("Couldn't resolve leaf PersistentEntity absent path");
			}
			throw new IllegalStateException(
					String.format("Couldn't resolve leaf PersistentEntity for type %s", path.getLeafProperty().getActualType()));
		}

		return entity;
	}

	public RelationalPersistentProperty getRequiredIdProperty() {
		return this.path == null ? context.getRequiredPersistentEntity(type).getRequiredIdProperty() : getRequiredLeafEntity().getRequiredIdProperty();

	}

	public int getLength() {
		return path == null ? 0 : path.getLength();
	}


	/**
	 * The column name used for the list index or map key of the leaf property of this path.
	 *
	 * @return May be {@literal null}.
	 */
	@Nullable
	public SqlIdentifier getQualifierColumn() {
		return path == null ? SqlIdentifier.EMPTY : path.getLeafProperty().getKeyColumn();
	}

	/**
	 * The type of the qualifier column of the leaf property of this path or {@literal null} if this is not applicable.
	 *
	 * @return may be {@literal null}.
	 */
	@Nullable
	public Class<?> getQualifierColumnType() {
		return path == null ? null : path.getLeafProperty().getQualifierColumnType();
	}

	@Override
	public String toString() {
		return "AggregatePath[" + (type == null ? path.getBaseProperty().getOwner().getType().getName() : type.getName()) +  "]" + ((path == null) ? "/" : path.toDotPath());
	}
}
