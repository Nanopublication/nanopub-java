package org.nanopub.templates;

import java.util.List;

/**
 * Thrown when a template cannot be filled as requested. It lists everything that was found
 * wrong, not only the first problem: a value that is missing, a value that breaks a
 * constraint of its placeholder, a name no placeholder answers to, a template that cannot be
 * retrieved.
 */
public class TemplateFillException extends Exception {

    private final List<String> problems;

    /**
     * Creates the exception for the given problems.
     *
     * @param problems the problems, each a sentence for a person
     */
    public TemplateFillException(List<String> problems) {
        super(String.join("\n", problems));
        this.problems = List.copyOf(problems);
    }

    /**
     * Creates the exception for one problem.
     *
     * @param problem the problem, a sentence for a person
     */
    public TemplateFillException(String problem) {
        this(List.of(problem));
    }

    /**
     * @return the problems, in the order they were found; unmodifiable
     */
    public List<String> getProblems() {
        return problems;
    }

}
