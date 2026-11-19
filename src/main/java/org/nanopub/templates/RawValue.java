package org.nanopub.templates;

import java.io.Serializable;

/**
 * A value as a person or a program gives it for a placeholder, before it is turned into the
 * RDF value that goes into the nanopublication. What the text means depends on the kind of
 * placeholder, see {@link PlaceholderMatcher#toValue(RawValue, BindingContext)}.
 *
 * @param text        the text of the value; never null, empty for no value
 * @param languageTag the language tag chosen for a literal whose placeholder leaves the
 *                    language open, or null
 */
public record RawValue(String text, String languageTag) implements Serializable {

    /**
     * Creates a raw value. A null text stands for no value, a blank language tag for none.
     *
     * @param text        the text of the value
     * @param languageTag the language tag, or null
     */
    public RawValue {
        if (text == null) text = "";
        if (languageTag != null && languageTag.isBlank()) languageTag = null;
    }

    /**
     * Creates a raw value without a language tag.
     *
     * @param text the text of the value
     */
    public RawValue(String text) {
        this(text, null);
    }

    /**
     * @return true if this stands for no value
     */
    public boolean isEmpty() {
        return text.isEmpty();
    }

}
