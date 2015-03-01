package nz.ac.waikato.its.dspace.exportcitation;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for Waikato University ITS
 */
public class ItemCitationExporter extends AbstractReader implements Recyclable {
	private static final String SEPARATOR = "  - ";

	private Request request;
	private Response response;

	EndnoteExportCrosswalk crosswalk = new EndnoteExportCrosswalk();

	private String filename;
	private Context context;
	private Item item;

	@Override
	public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
		super.setup(resolver, objectModel, src, par);

		try {
			this.request = ObjectModelHelper.getRequest(objectModel);
			this.response = ObjectModelHelper.getResponse(objectModel);
			context = ContextUtil.obtainContext(objectModel);

			String handle = par.getParameter("handle");
			DSpaceObject dso = HandleManager.resolveToObject(context, handle);
			item = (Item) dso;

			filename = "cite-" + handle.replaceAll("/", "-") + ".ris";
		} catch (SQLException | ParameterException e) {
			throw new ProcessingException("Unable to export citation.", e);
		}
	}

	@Override
	public void generate() throws IOException, SAXException, ProcessingException {
		response.setContentType(crosswalk.getMIMEType());
		response.setHeader("Content-Disposition", "attachment; filename=" + filename);

		crosswalk.writeHeader(out);

		if (crosswalk.canDisseminate(context, item)) {
			try {
				crosswalk.disseminate(context, item, out);
				out.write("\n\n".getBytes());
			} catch (CrosswalkException | AuthorizeException | SQLException e) {
				e.printStackTrace();
			}
		}

		out.flush();
		out.close();
	}

	@Override
	public void recycle() {
		this.request = null;
		this.response = null;
		this.context = null;
		this.item = null;
		this.filename = null;
	}
}
