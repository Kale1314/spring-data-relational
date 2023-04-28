package org.springframework.data.domain;

import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Join;
import org.springframework.data.relational.core.sql.Table;

import java.util.ArrayList;
import java.util.List;

public class Joins {

    private final List<Join> joins;

    public Joins(Join... join) {
        joins = new ArrayList<>(List.of(join));
    }

    public static Joins join(Join... join) {
        return new Joins(join);
    }

    public static Join by(String table, Join.JoinType type, Condition on) {
        return Join.by(type, Table.create(table), on);
    }

    public Joins and(Join join) {
        joins.add(join);
        return this;
    }


    public List<Join> getJoins() {
        return joins;
    }

    public boolean notEmpty() {
        return !joins.isEmpty();
    }

}
