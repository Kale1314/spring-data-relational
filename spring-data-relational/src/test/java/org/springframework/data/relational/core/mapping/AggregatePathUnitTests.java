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


import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mapping.PersistentPropertyPath;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.springframework.data.relational.core.sql.SqlIdentifier.*;

class AggregatePathUnitTests {
	RelationalMappingContext context = new RelationalMappingContext();

	private RelationalPersistentEntity<?> entity = context.getRequiredPersistentEntity(DummyEntity.class);

	@Test
	void isNotRootForNonRootPath() {

		AggregatePath path = context.getAggregatePath(context.getPersistentPropertyPath("entityId", DummyEntity.class));

		assertThat(path.isRoot()).isFalse();
	}
	@Test
	void isRootForRootPath() {

		AggregatePath path = context.getAggregateRootPath(DummyEntity.class);

		assertThat(path.isRoot()).isTrue();
	}


	@Test // DATAJDBC-359
	void idDefiningPath() {

		assertSoftly(softly -> {

			softly.assertThat(path("second.third2.value").getIdDefiningParentPath()).isEqualTo(path());
			softly.assertThat(path("second.third.value").getIdDefiningParentPath()).isEqualTo(path());
			softly.assertThat(path("secondList.third2.value").getIdDefiningParentPath()).isEqualTo(path());
			softly.assertThat(path("secondList.third.value").getIdDefiningParentPath()).isEqualTo(path());
			softly.assertThat(path("second2.third2.value").getIdDefiningParentPath()).isEqualTo(path());
			softly.assertThat(path("second2.third.value").getIdDefiningParentPath()).isEqualTo(path());
			softly.assertThat(path("withId.second.third2.value").getIdDefiningParentPath()).isEqualTo(path("withId"));
			softly.assertThat(path("withId.second.third.value").getIdDefiningParentPath()).isEqualTo(path("withId"));
		});
	}

	@Test // DATAJDBC-359
	void getRequiredIdProperty() {

		assertSoftly(softly -> {

			softly.assertThat(path().getRequiredIdProperty().getName()).isEqualTo("entityId");
			softly.assertThat(path("withId").getRequiredIdProperty().getName()).isEqualTo("withIdId");
			softly.assertThatThrownBy(() -> path("second").getRequiredIdProperty())
					.isInstanceOf(IllegalStateException.class);
		});
	}

	@Test // DATAJDBC-359
	void reverseColumnName() {
		System.out.println(path("second.third2"));
		assertSoftly(softly -> {

			softly.assertThat(path("second.third2").getReverseColumnName()).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(path("second.third").getReverseColumnName()).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(path("secondList.third2").getReverseColumnName()).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(path("secondList.third").getReverseColumnName()).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(path("second2.third2").getReverseColumnName()).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(path("second2.third").getReverseColumnName()).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(path("withId.second.third2.value").getReverseColumnName()).isEqualTo(quoted("WITH_ID"));
			softly.assertThat(path("withId.second.third").getReverseColumnName()).isEqualTo(quoted("WITH_ID"));
			softly.assertThat(path("withId.second2.third").getReverseColumnName()).isEqualTo(quoted("WITH_ID"));
		});
	}


	@Test
	void extendBy() {

		assertSoftly(softly -> {

			softly.assertThat(path().extendBy(entity.getRequiredPersistentProperty("withId")))
					.isEqualTo(path("withId"));
			softly.assertThat(path("withId").extendBy(path("withId").getRequiredIdProperty()))
					.isEqualTo(path("withId.withIdId"));
		});
	}
	private AggregatePath path() {
		return context.getAggregateRootPath(entity.getType());
	}

	private AggregatePath path(String path) {
		return context.getAggregatePath(createSimplePath(path));
	}
	PersistentPropertyPath<RelationalPersistentProperty> createSimplePath(String path) {
		return PersistentPropertyPathTestUtils.getPath(context,path, DummyEntity.class);
	}

	@SuppressWarnings("unused")
	static class DummyEntity {
		@Id
		Long entityId;
		@ReadOnlyProperty
		Second second;
		@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sec") Second second2;
		@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL) Second second3;
		List<Second> secondList;
		WithId withId;
	}

	@SuppressWarnings("unused")
	static class Second {
		Third third;
		@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "thrd") Third third2;
	}

	@SuppressWarnings("unused")
	static class Third {
		String value;
	}

	@SuppressWarnings("unused")
	static class WithId {
		@Id Long withIdId;
		Second second;
		@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sec") Second second2;
	}

}

