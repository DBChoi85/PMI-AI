package io.github.dbchoi85.pmiai.model;

import java.util.Set;

public record Privilege(Set<String> operations, String resource, long expiresAtEpochSecond) {
    public boolean attenuates(Privilege parent) {
        return parent.operations().containsAll(operations)
                && resource.startsWith(parent.resource().replace("**", ""))
                && expiresAtEpochSecond <= parent.expiresAtEpochSecond();
    }
}
