package org.springframework.data.r2dbc.core;

import io.r2dbc.spi.R2dbcType;
import io.r2dbc.spi.test.MockColumnMetadata;
import io.r2dbc.spi.test.MockResult;
import io.r2dbc.spi.test.MockRow;
import io.r2dbc.spi.test.MockRowMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Boundary;
import org.springframework.data.domain.Groups;
import org.springframework.data.domain.Joins;
import org.springframework.data.domain.Range;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.testing.StatementRecorder;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.Join;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class R2dbcEntityTemplateSupportUnitTests {
    private org.springframework.r2dbc.core.DatabaseClient client;
    private R2dbcEntityTemplate entityTemplate;
    private StatementRecorder recorder;

    @BeforeEach
    void before() {

        recorder = StatementRecorder.newInstance();
        client = DatabaseClient.builder().connectionFactory(recorder)
                .bindMarkers(PostgresDialect.INSTANCE.getBindMarkersFactory()).build();
        entityTemplate = new R2dbcEntityTemplate(client, PostgresDialect.INSTANCE);
    }


    @Test
    void countWithBoundary() {

        MockRowMetadata metadata = MockRowMetadata.builder()
                .columnMetadata(MockColumnMetadata.builder().name("name").type(R2dbcType.VARCHAR).build()).build();
        MockResult result = MockResult.builder().row(MockRow.builder().identified(0, Long.class, 1L).build()).build();

        recorder.addStubbing(s -> s.startsWith("SELECT"), result);


        Boundary boundary = Boundary.scopes(Boundary.Scope.by("name", Range.of(Range.Bound.inclusive(0), Range.Bound.inclusive(2))));

        entityTemplate.count(Query.query(Criteria.where("name").is("Walter")).withBoundary(boundary), R2dbcEntityTemplateUnitTests.Person.class) //
                .as(StepVerifier::create) //
                .expectNext(1L) //
                .verifyComplete();

        StatementRecorder.RecordedStatement statement = recorder.getCreatedStatement(s -> s.startsWith("SELECT"));

        assertThat(statement.getSql()).isEqualTo("SELECT COUNT(person.id) FROM person WHERE (person.THE_NAME = $1) AND ((person.THE_NAME >= $2 AND person.THE_NAME <= $3))");
        assertThat(statement.getBindings()).hasSize(3).containsEntry(0, Parameter.from("Walter"))
                .containsEntry(1, Parameter.from("0"))
                .containsEntry(2, Parameter.from("2"));
    }


    @Test
    void countWithGroupBy() {

        MockRowMetadata metadata = MockRowMetadata.builder()
                .columnMetadata(MockColumnMetadata.builder().name("name").type(R2dbcType.VARCHAR).name("date").type(R2dbcType.DATE).build()).build();
        MockResult result = MockResult.builder().row(MockRow.builder().identified(0, Long.class, 1L).build()).build();

        recorder.addStubbing(s -> s.startsWith("SELECT"), result);


        Boundary boundary = Boundary.scopes(Boundary.Scope.by("name", Range.of(Range.Bound.inclusive(0), Range.Bound.inclusive(2))));

        entityTemplate.count(Query.query(Criteria.where("name").is("Walter")).withBoundary(boundary).groupBy(Groups.by("name", "date")), R2dbcEntityTemplateUnitTests.Person.class) //
                .as(StepVerifier::create) //
                .expectNext(1L) //
                .verifyComplete();

        StatementRecorder.RecordedStatement statement = recorder.getCreatedStatement(s -> s.startsWith("SELECT"));

        assertThat(statement.getSql()).isEqualTo("SELECT COUNT(1) FROM (SELECT COUNT(person.id) FROM person WHERE (person.THE_NAME = $1 AND ((person.THE_NAME >= $2 AND (person.THE_NAME <= $3)))) GROUP BY person.THE_NAME, person.date) person");
        assertThat(statement.getBindings()).hasSize(3).containsEntry(0, Parameter.from("Walter"))
                .containsEntry(1, Parameter.from("0"))
                .containsEntry(2, Parameter.from("2"));
    }


    @Test
    void countWithJoin() {

        MockRowMetadata metadata = MockRowMetadata.builder()
                .columnMetadata(MockColumnMetadata.builder().name("name").type(R2dbcType.VARCHAR).name("date").type(R2dbcType.DATE).build()).build();
        MockResult result = MockResult.builder().row(MockRow.builder().identified(0, Long.class, 1L).build()).build();

        recorder.addStubbing(s -> s.startsWith("SELECT"), result);


        Boundary boundary = Boundary.scopes(Boundary.Scope.by("name", Range.of(Range.Bound.inclusive(0), Range.Bound.inclusive(2))));

        Join join = Joins.by("join_table", Join.JoinType.LEFT_OUTER_JOIN, Conditions.isEqual(Expressions.just("person.id"), Expressions.just("join_table.person_id")))
                .withCriteria(Criteria.where("id").is("ID"));
        Joins joins = Joins.join(join);

        entityTemplate.count(Query.query(Criteria.where("name").is("Walter")).withBoundary(boundary)
                        .groupBy(Groups.by("name", "date"))
                        .join(joins), R2dbcEntityTemplateUnitTests.Person.class) //
                .as(StepVerifier::create) //
                .expectNext(1L) //
                .verifyComplete();

        StatementRecorder.RecordedStatement statement = recorder.getCreatedStatement(s -> s.startsWith("SELECT"));

        assertThat(statement.getSql()).isEqualTo("SELECT COUNT(person.id) FROM person LEFT OUTER JOIN join_table ON person.id = join_table.person_id WHERE (person.THE_NAME = $1 AND ((person.THE_NAME >= $2 AND (person.THE_NAME <= $3)))) AND join_table.id = $4 GROUP BY person.THE_NAME, person.date");
        assertThat(statement.getBindings()).hasSize(4).containsEntry(0, Parameter.from("Walter"))
                .containsEntry(1, Parameter.from("0"))
                .containsEntry(2, Parameter.from("2"))
                .containsEntry(3, Parameter.from("ID"));
    }

}
