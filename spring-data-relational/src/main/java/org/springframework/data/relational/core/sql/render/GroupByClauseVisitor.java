package org.springframework.data.relational.core.sql.render;

import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.GroupByField;
import org.springframework.data.relational.core.sql.Visitable;
import org.springframework.lang.Nullable;

public class GroupByClauseVisitor extends TypedSubtreeVisitor<GroupByField> implements PartRenderer {
    private final RenderContext context;

    private final StringBuilder builder = new StringBuilder();

    @Nullable
    private PartRenderer delegate;

    private boolean first = true;

    GroupByClauseVisitor(RenderContext context) {
        this.context = context;
    }

    @Override
    Delegation enterMatched(GroupByField segment) {

        if (!first) {
            builder.append(", ");
        }
        first = false;

        return super.enterMatched(segment);
    }


    @Override
    Delegation leaveNested(Visitable segment) {

        if (delegate instanceof SimpleFunctionVisitor || delegate instanceof ExpressionVisitor) {
            builder.append(delegate.getRenderedPart());
            delegate = null;
        }

        if (segment instanceof Column) {
            builder.append(NameRenderer.fullyQualifiedReference(context, (Column) segment));
        }

        return super.leaveNested(segment);
    }

    @Override
    public CharSequence getRenderedPart() {
        return builder;
    }
}
