package nz.ac.waikato.its.dspace.exportcitation;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.aspect.discovery.DiscoveryUIUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.handle.HandleManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz
 *         for the University of Waikato's Institutional Research Repositories
 */
public class SearchResultsCitationExporter extends AbstractCitationExporter {
    private static final int MAX_RPP = 2_000;

    @Override
    protected void initItemsAndFilename(Parameters par) throws ParameterException, SQLException, ProcessingException {
        try {
            DiscoverResult searchResults = doSearch();
            List<DSpaceObject> dsos = searchResults.getDspaceObjects();
            for (DSpaceObject dso : dsos) {
                if (dso != null && dso.getType() == Constants.ITEM) {
                    items.add((Item) dso);
                }
            }
            filename = "cite-items";
            log.info(LogManager.getHeader(context, "citationexport_search_results", "Exporting to " + crosswalk.getMIMEType()));
        } catch (UIException | SearchServiceException e) {
            log.error("Problem exporting items for search result: " + e.getMessage(), e);
            throw new ProcessingException("Could not perform search: " + e.getMessage(), e);
        }
    }

    private DiscoverResult doSearch() throws SQLException, UIException, SearchServiceException {
        // TODO determine whether we want to export page or all -- defaulting to page

        DSpaceObject scope = getScope();
        DiscoverQuery queryArgs = new DiscoverQuery();

        String query = getQuery();

        // Escape any special characters in this user-entered query
        //query = DiscoveryUIUtils.escapeQueryChars(query);
        query = StringUtils.replace(query, ": ", "\\: ");

        int page = getParameterPage();

        List<String> filterQueries = new ArrayList<>();

        String[] fqs = getFilterQueries();

        if (fqs != null)
        {
            filterQueries.addAll(Arrays.asList(fqs));
        }

        //Add the configured default filter queries
        DiscoveryConfiguration discoveryConfiguration = SearchUtils.getDiscoveryConfiguration(scope);
        List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
        queryArgs.addFilterQueries(defaultFilterQueries.toArray(new String[defaultFilterQueries.size()]));

        if (filterQueries.size() > 0) {
            queryArgs.addFilterQueries(filterQueries.toArray(new String[filterQueries.size()]));
        }

        int parameterRpp = getParameterRpp();
        queryArgs.setMaxResults(parameterRpp > MAX_RPP ? MAX_RPP : parameterRpp);

        String sortBy = request.getParameter("sort_by");
        DiscoverySortConfiguration searchSortConfiguration = discoveryConfiguration.getSearchSortConfiguration();
        if(sortBy == null){
            //Attempt to find the default one, if none found we use SCORE
            sortBy = "score";
            if(searchSortConfiguration != null){
                for (DiscoverySortFieldConfiguration sortFieldConfiguration : searchSortConfiguration.getSortFields()) {
                    if(sortFieldConfiguration.equals(searchSortConfiguration.getDefaultSort())){
                        sortBy = SearchUtils.getSearchService().toSortFieldIndex(sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());
                    }
                }
            }
        }
        String sortOrder = request.getParameter("order");
        if(sortOrder == null && searchSortConfiguration != null){
            sortOrder = searchSortConfiguration.getDefaultSortOrder().toString();
        }

        if (sortOrder == null || sortOrder.equalsIgnoreCase("DESC"))
        {
            queryArgs.setSortField(sortBy, DiscoverQuery.SORT_ORDER.desc);
        }
        else
        {
            queryArgs.setSortField(sortBy, DiscoverQuery.SORT_ORDER.asc);
        }

        queryArgs.setQuery(query != null && !query.trim().equals("") ? query : null);

        if (page > 1)
        {
            queryArgs.setStart((page - 1) * queryArgs.getMaxResults());
        }
        else
        {
            queryArgs.setStart(0);
        }
        queryArgs.setSpellCheck(false);

        return SearchUtils.getSearchService().search(context, scope, queryArgs);
    }

    private String getQuery() throws UIException {
        String query = AbstractDSpaceTransformer.decodeFromURL(request.getParameter("query"));
        if (query == null)
        {
            return "";
        }
        return query.trim();
    }

    protected int getParameterPage() {
        try {
            return Integer.parseInt(request.getParameter("page"));
        }
        catch (Exception e) {
            return 1;
        }
    }
    /**
     * Returns all the filter queries for use by solr
     * This method returns more expanded filter queries then the getParameterFilterQueries
     * @return an array containing the filter queries
     */
    protected String[] getFilterQueries() {
        try {
            return DiscoveryUIUtils.getFilterQueries(request, context);
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Determine the current scope. This may be derived from the current url
     * handle if present or the scope parameter is given. If no scope is
     * specified then null is returned.
     *
     * @return The current scope.
     */
    protected DSpaceObject getScope() throws SQLException {
        String scopeString = request.getParameter("current-scope");

        // Are we in a community or collection?
        DSpaceObject dso;
        if (scopeString == null || "".equals(scopeString))
        {
            // get the search scope from the url handle
            dso = HandleUtil.obtainHandle(objectModel);
        }
        else
        {
            // Get the search scope from the location parameter
            dso = HandleManager.resolveToObject(context, scopeString);
        }

        return dso;
    }

    protected int getParameterRpp() {
        try {
            return Integer.parseInt(request.getParameter("rpp"));
        }
        catch (Exception e) {
            return 10;
        }
    }
}
