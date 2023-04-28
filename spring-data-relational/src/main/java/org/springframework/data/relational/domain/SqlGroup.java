package org.springframework.data.relational.domain;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class SqlGroup {
    private static final Predicate<String> predicate = Pattern.compile("^[0-9a-zA-Z_\\.\\(\\)]*$").asPredicate();

    public static void validate(String property) {
        if (!predicate.test(property)) {
            throw new IllegalArgumentException(
                    "group fields that are not marked as unsafe must only consist of digits, letter, '.', '_', and '\'. Note that such expressions become part of SQL statements and therefore need to be sanatized to prevent SQL injection attacks.");
        }
    }
}
