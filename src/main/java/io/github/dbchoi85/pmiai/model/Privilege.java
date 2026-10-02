package io.github.dbchoi85.pmiai.model;

import java.util.Set;

public record Privilege(Set<String> operations, String resource, long expiresAtEpochSecond) {
    public Privilege {
        operations = Set.copyOf(operations);
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("resource must not be blank");
        }
    }

    public boolean attenuates(Privilege parent) {
        return parent.operations().containsAll(operations)
                && resourceWithin(resource, parent.resource())
                && expiresAtEpochSecond <= parent.expiresAtEpochSecond();
    }

    static boolean resourceWithin(String child, String parent) {
        String childPath = normalize(child);
        String parentPath = normalize(parent);

        if (parent.endsWith("/**")) {
            return childPath.equals(parentPath) || childPath.startsWith(parentPath + "/");
        }
        return childPath.equals(parentPath);
    }

    private static String normalize(String resource) {
        String value = resource;
        if (value.endsWith("/**")) value = value.substring(0, value.length() - 3);
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
