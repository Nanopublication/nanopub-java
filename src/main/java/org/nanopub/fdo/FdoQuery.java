package org.nanopub.fdo;

import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryAccess;

import java.util.Map;

public class FdoQuery {
    private static String textSearch = "RAkYh4UPJryajbtIDbLG-Bfd6A4JD2SbU9bmZdvaEdFRY/fdo-text-search";
    private static String findByRef = "RAQiQjx3OiO9ra9ImWl9kpuDpT8d3EiBSrftckOAAwGKc/find-fdos-by-ref";
    private static String getFeed = "RAP1G35VvTs3gfMaucv_xZUMZuvjB9lxM8tWUGttr5mmo/get-fdo-feed";
    private static String getFavoriteThings = "RAsyc6zFFnE8mblnDfdCCNRsrcN1CSCBDW9I4Ppidgk9g/get-favorite-things";

    private FdoQuery () {
        // no instances
    }

    /**
     * This query performs a full-text search on the FDO nanopublications.
     */
    public static ApiResponse textSearch(String query) throws FailedApiCallException {
        return QueryAccess.get(textSearch, Map.of("query", query));
    }

    /**
     * This query returns the FDOs whose records refer to the given PID / handle.
     */
    public static ApiResponse findByRef(String ref) throws FailedApiCallException {
        return QueryAccess.get(findByRef, Map.of("refid", ref));
    }

    /**
     * This query returns the latest FDOs from the specified creator.
     * @param creator The orcid url, i.e. https://orcid.org/0009-0008-3635-347X
     */
    public static ApiResponse getFeed(String creator) throws FailedApiCallException {
        return QueryAccess.get(getFeed, Map.of("creator", creator));
    }

    /**
     * This query returns the things the given user has declared to be their favorites (using cito:likes).
     * @param creator The orcid url, i.e. https://orcid.org/0009-0008-3635-347X
     */
    public static ApiResponse getFavoriteThings(String creator) throws FailedApiCallException {
        return QueryAccess.get(getFavoriteThings, Map.of("creator", creator));
    }

}
