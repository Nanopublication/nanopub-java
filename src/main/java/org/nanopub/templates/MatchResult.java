package org.nanopub.templates;

import java.io.Serializable;
import java.util.List;

/**
 * What a {@link PlaceholderMatcher} found when it checked a value: the problems the value
 * has, if any. A value can be bound when none of them is blocking; it complies with the
 * template when there is none at all.
 *
 * @param violations the problems of the value, empty if there are none
 */
public record MatchResult(List<Violation> violations) implements Serializable {

    /**
     * Creates a result with the given violations.
     *
     * @param violations the problems of the value
     */
    public MatchResult {
        violations = List.copyOf(violations);
    }

    /**
     * Whether the value can stand in the position at all. A statement matcher takes this to
     * decide which statement of a template a triple belongs to.
     *
     * @return true if no violation is blocking
     */
    public boolean isBindable() {
        return violations.stream().noneMatch(Violation::blocking);
    }

    /**
     * Whether the value is what filling in the template could have produced.
     *
     * @return true if there is no violation
     */
    public boolean isValid() {
        return violations.isEmpty();
    }

}
