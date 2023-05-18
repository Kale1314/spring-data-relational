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

class AggregatePathUnitTests {
	RelationalMappingContext context = new RelationalMappingContext();

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

			softly.assertThat(path("second.third2.value").getIdDefiningParentPath().isRoot()).isTrue();
			softly.assertThat(path("second.third.value").getIdDefiningParentPath().isRoot()).isTrue();
			softly.assertThat(path("secondList.third2.value").getIdDefiningParentPath().isRoot()).isTrue();
			softly.assertThat(path("secondList.third.value").getIdDefiningParentPath().isRoot()).isTrue();
			softly.assertThat(path("second2.third2.value").getIdDefiningParentPath().isRoot()).isTrue();
			softly.assertThat(path("second2.third.value").getIdDefiningParentPath().isRoot()).isTrue();
			softly.assertThat(path("withId.second.third2.value").getIdDefiningParentPath().isRoot()).isFalse();
			softly.assertThat(path("withId.second.third.value").getIdDefiningParentPath().isRoot()).isFalse();
		});
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

