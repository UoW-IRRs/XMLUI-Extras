package nz.ac.waikato.its.dspace.exportcitation;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;

import java.sql.SQLException;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for Waikato University ITS
 */
public class ItemListCitationExporter extends AbstractCitationExporter {
	public static final String HANDLES_PARAM_NAME = "handle";

	@Override
	protected void initItemsAndFilename(Parameters par) throws ParameterException, SQLException, ProcessingException {
		if (par.isParameter(HANDLES_PARAM_NAME)) {
            String handlesParam = par.getParameter(HANDLES_PARAM_NAME);
            DSpaceObject dso = HandleManager.resolveToObject(context, handlesParam);
            if (dso == null || dso.getType() != Constants.ITEM) {
                throw new ProcessingException("No matching item(s) found");
            }
            items.add((Item) dso);
            filename = "cite-" + handlesParam.replaceAll("/", "-");
			log.info(LogManager.getHeader(context, "citationexport_single_item", "Exporting to " + crosswalk.getMIMEType()));
        } else {
            String[] handles = request.getParameterValues(HANDLES_PARAM_NAME);
            for (String handle : handles) {
                DSpaceObject dso = HandleManager.resolveToObject(context, handle);
                if (dso == null || dso.getType() != Constants.ITEM) {
                    log.warn("No item found for handle " + handle);
                } else {
                    items.add((Item) dso);
                }
            }
            filename = "cite-items";
			log.info(LogManager.getHeader(context, "citationexport_list_items", "Exporting to " + crosswalk.getMIMEType()));
        }
	}

}
