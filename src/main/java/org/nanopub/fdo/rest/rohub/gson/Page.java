package org.nanopub.fdo.rest.rohub.gson;

/**
 * Representing a page from RO-HUBs index. You get a page by the RoHub API in the following manner:
 * https://api.rohub.org/api/ros/?page=1
 *
 * And the respons looks like:
 *
 * {
 *     "count": 3292,
 *     "next": "https://api.rohub.org/api/ros/?page=2",
 *     "previous": null,
 *     "results": [
 *         {
 *             "identifier": "b3aa0332-2954-486b-b95f-33d84af14232",
 *             ...
 *         }
 *         {
 *             ...
 *         }
 *         ...
 *      ]
 *
 *      The Java type for gson mapping of the results[] is @RoCrateIndex.class
 */
public class Page {
    public RoCrateIndex[] results;
}
