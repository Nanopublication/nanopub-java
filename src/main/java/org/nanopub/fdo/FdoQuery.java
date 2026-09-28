package org.nanopub.fdo;

import org.nanopub.extra.services.*;

/**
 * This class represents an FDO query.
 */
public class FdoQuery {

    private static final String textSearch = "RAkYh4UPJryajbtIDbLG-Bfd6A4JD2SbU9bmZdvaEdFRY/fdo-text-search";
    private static final String findByRef = "RAQiQjx3OiO9ra9ImWl9kpuDpT8d3EiBSrftckOAAwGKc/find-fdos-by-ref";
    private static final String getFeed = "RAP1G35VvTs3gfMaucv_xZUMZuvjB9lxM8tWUGttr5mmo/get-fdo-feed";
    private static final String getFavoriteThings = "RAsyc6zFFnE8mblnDfdCCNRsrcN1CSCBDW9I4Ppidgk9g/get-favorite-things";

    private FdoQuery() {
        // no instances
    }

    /**
     * This query performs a full-text search on the FDO nanopublications.
     *
     * @param query The search query string
     * @return An ApiResponse containing the FDOs that match the search query.
     * @throws org.nanopub.extra.services.FailedApiCallException if the API call fails.
     * @throws APINotReachableException                          if the API is not reachable.
     * @throws NotEnoughAPIInstancesException                    if there are not enough API instances available.
     */
    public static ApiResponse textSearch(String query) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        return QueryAccess.get(new QueryRef(textSearch, "query", query));
    }

    /**
     * This query returns the FDOs whose records refer to the given PID / handle.
     *
     * @param ref The PID or handle to search for
     * @return An ApiResponse containing the FDOs that refer to the given PID / handle.
     * @throws org.nanopub.extra.services.FailedApiCallException if the API call fails.
     * @throws APINotReachableException                          if the API is not reachable.
     * @throws NotEnoughAPIInstancesException                    if there are not enough API instances available.
     */
    public static ApiResponse findByRef(String ref) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        return QueryAccess.get(new QueryRef(findByRef, "refid", ref));
    }

    /**
     * This query returns the latest FDOs from the specified creator.
     *
     * @param creator The orcid url, i.e. <a href="https://orcid.org/0009-0008-3635-347X">...</a>
     * @return An ApiResponse containing the FDOs created by the specified user.
     * @throws FailedApiCallException         if the API call fails.
     * @throws APINotReachableException       if the API is not reachable.
     * @throws NotEnoughAPIInstancesException if there are not enough API instances available.
     */
    public static ApiResponse getFeed(String creator) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        return QueryAccess.get(new QueryRef(getFeed, "creator", creator));
    }

    /**
     * This query returns the things the given user has declared to be their favorites (using cito:likes).
     *
     * @param creator The orcid url, i.e. <a href="https://orcid.org/0009-0008-3635-347X">...</a>
     * @return An ApiResponse containing the favorite things of the specified user.
     * @throws FailedApiCallException         if the API call fails.
     * @throws APINotReachableException       if the API is not reachable.
     * @throws NotEnoughAPIInstancesException if there are not enough API instances available.
     */
    public static ApiResponse getFavoriteThings(String creator) throws FailedApiCallException, APINotReachableException, NotEnoughAPIInstancesException {
        return QueryAccess.get(new QueryRef(getFavoriteThings, "creator", creator));
    }

}
