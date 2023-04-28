package org.springframework.data.r2dbc.core;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Groups;
import org.springframework.data.domain.Joins;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.Join;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.binding.BindTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class StatementMapperSupportUnitTests {
    private ReactiveDataAccessStrategy strategy = new DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE);
    private StatementMapper mapper = strategy.getStatementMapper();

    private BindTarget bindTarget = mock(BindTarget.class);


    @Test
    void shouldMapGroupByWithPage() {

        StatementMapper.SelectSpec selectSpec = StatementMapper.SelectSpec.create("table").withProjection("*")
                .withGroupBy(Groups.by("name", "date"))
                .withPage(PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "id")));

        PreparedOperation<?> preparedOperation = mapper.getMappedObject(selectSpec);

        assertThat(preparedOperation.toQuery())
                .isEqualTo("SELECT table.* FROM table GROUP BY table.name, table.date ORDER BY table.id DESC LIMIT 2 OFFSET 2");
    }

    @Test
    void shouldMapJoinWithPage() {
        Join join = Joins.by("join_table", Join.JoinType.LEFT_OUTER_JOIN,
                        Conditions.isEqual(Expressions.just("join_table.id"), Expressions.just("table.join_id")))
                .withCriteria(Criteria.where("id").is("ID"));
        Joins joins = Joins.join(join);

        StatementMapper.SelectSpec selectSpec = StatementMapper.SelectSpec.create("table").withProjection("*")
                .withCriteria(Criteria.where("name").is("NAME"))
                .withGroupBy(Groups.by("name", "date"))
                .withJoin(joins)
                .withPage(PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "id")));

        PreparedOperation<?> preparedOperation = mapper.getMappedObject(selectSpec);

        assertThat(preparedOperation.toQuery())
                .isEqualTo("SELECT table.* FROM table LEFT OUTER JOIN join_table ON join_table.id = table.join_id WHERE table.name = $1 AND join_table.id = $2 GROUP BY table.name, table.date ORDER BY table.id DESC LIMIT 2 OFFSET 2");
    }
}
