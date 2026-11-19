/**
 * This package contains classes for working with nanopublication templates: nanopublications
 * that describe, with placeholders, statements and constraints, how nanopublications of a
 * certain kind are structured. {@link org.nanopub.templates.Template} reads a template,
 * {@link org.nanopub.templates.PlaceholderMatcher} says what a value means for a position of
 * one of its statements, and {@link org.nanopub.templates.BindingContext} holds the values
 * bound for one nanopublication. {@link org.nanopub.templates.TemplateComplianceChecker} checks
 * a nanopublication against the templates it names, and
 * {@link org.nanopub.templates.TemplateFiller} makes a nanopublication from templates and
 * values.
 */
package org.nanopub.templates;
