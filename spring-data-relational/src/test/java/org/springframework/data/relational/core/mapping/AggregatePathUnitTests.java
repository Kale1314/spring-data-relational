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
import org.springframework.data.mapping.context.MappingContext;

import static org.assertj.core.api.Assertions.*;

class AggregatePathUnitTests {
	RelationalMappingContext context = new RelationalMappingContext();

	@Test
	void isNotRootForNonRootPath() {

		AggregatePath path = context.getAggregatePath(context.getPersistentPropertyPath("id", DummyEntity.class));

		assertThat(path.isRoot()).isFalse();
	}
	@Test
	void isRootForRootPath() {

		AggregatePath path = context.getAggregateRootPath(DummyEntity.class);

		assertThat(path.isRoot()).isTrue();
	}
	static class DummyEntity{
		Long id;
		String name;
	}
}

