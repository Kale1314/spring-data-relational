package org.springframework.data.domain;

import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Boundary {

    private static final Predicate<String> predicate = Pattern.compile("^[0-9a-zA-Z_\\.\\(\\)]*$").asPredicate();
    private final List<Scope<?>> scopes;

    public Boundary(Scope<?>... scopes) {
        this(List.of(scopes));
    }

    public Boundary(List<Scope<?>> scopes) {
        this.scopes = new ArrayList<>(scopes);
    }

    public static Boundary scopes(Scope<?>... scopes) {
        return new Boundary(scopes);
    }

    public static Boundary scopes(List<Scope<?>> scopes) {
        return new Boundary(scopes);
    }

    private static void validate(String property) {
        if (!predicate.test(property)) {
            throw new IllegalArgumentException(
                    "boundary fields that are not marked as unsafe must only consist of digits, letter, '.', '_', and '\'. Note that such expressions become part of SQL statements and therefore need to be sanatized to prevent SQL injection attacks.");
        }
    }

    public Boundary and(Scope<?> scope) {
        this.scopes.add(scope);
        return this;
    }

    public boolean notEmpty() {
        return !scopes.isEmpty();
    }

    public List<Scope<?>> getScopes() {
        return scopes;
    }

    public CriteriaDefinition toCriteria() {
        return CriteriaDefinition.from(scopes.stream().map(Scope::toCriteria).toList());
    }

    public static final class Scope<T> {
        private final String property;
        private final Range<T> range;

        public Scope(
                String property,
                Range<T> range
        ) {
            validate(property);
            this.property = property;
            this.range = range;
        }

        public static <T> Scope<T> by(String property, Range<T> range) {
            return new Scope<>(property, range);
        }

        public CriteriaDefinition toCriteria() {
            List<CriteriaDefinition> criteriaDefinitions = new ArrayList<>();
            if (range.getLowerBound().isBounded()) {
                Range.Bound<T> bound = range.getLowerBound();
                criteriaDefinitions.add(bound.isInclusive() ?
                        Criteria.where(property).greaterThanOrEquals(bound.getValue().get())
                        : Criteria.where(property).greaterThan(bound.getValue().get()));
            }

            if (range.getUpperBound().isBounded()) {
                Range.Bound<T> bound = range.getUpperBound();
                criteriaDefinitions.add(bound.isInclusive() ?
                        Criteria.where(property).lessThanOrEquals(bound.getValue().get())
                        : Criteria.where(property).lessThan(bound.getValue().get()));
            }
            return CriteriaDefinition.from(criteriaDefinitions);
        }

        public String getProperty() {
            return property;
        }

        public Range<T> getRange() {
            return range;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Scope) obj;
            return Objects.equals(this.property, that.property) &&
                    Objects.equals(this.range, that.range);
        }

        @Override
        public int hashCode() {
            return Objects.hash(property, range);
        }

        @Override
        public String toString() {
            return "Scope[" +
                    "property=" + property + ", " +
                    "range=" + range + ']';
        }

    }
}
