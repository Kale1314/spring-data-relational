package org.springframework.data.relational.core.sql;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.UnaryOperator;

public class ExpressionSqlIdentifier implements SqlIdentifier {
    private final Expression expression;

    public ExpressionSqlIdentifier(Expression expression) {
        this.expression = expression;
    }

    public static SqlIdentifier create(Expression expression) {
        return new ExpressionSqlIdentifier(expression);
    }

    @Override
    @Deprecated(since = "3.1", forRemoval = true)
    public String getReference(IdentifierProcessing processing) {
        return expression.toString();
    }

    @Override
    public String toSql(IdentifierProcessing processing) {
        return expression.toString();
    }

    @Override
    public SqlIdentifier transform(UnaryOperator<String> transformationFunction) {
        return new ExpressionSqlIdentifier(expression);
    }

    @Override
    public Iterator<SqlIdentifier> iterator() {
        return Collections.<SqlIdentifier>singleton(this).iterator();
    }

    public Expression getExpression() {
        return expression;
    }
}
