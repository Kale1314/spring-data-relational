package org.springframework.data.domain;

import java.util.ArrayList;
import java.util.List;

public class Groups {
    private final List<String> groups;

    public Groups(String... groupBy) {
        groups = new ArrayList<>(List.of(groupBy));
    }

    public static Groups by(String... groupBy) {
        return new Groups(groupBy);
    }


    public Groups groupBy(String groupBy) {
        groups.add(groupBy);
        return this;
    }


    public List<String> getGroups() {
        return groups;
    }

    public boolean notEmpty() {
        return !groups.isEmpty();
    }
}
