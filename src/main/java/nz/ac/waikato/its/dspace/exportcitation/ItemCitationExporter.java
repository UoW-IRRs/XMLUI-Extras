package nz.ac.waikato.its.dspace.exportcitation;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.Handle;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for Waikato University ITS
 */
public class ItemCitationExporter extends AbstractReader implements Recyclable {
	private static final String SEPARATOR = "  - ";
	private Request request;
	private Response response;

	private String filename;
	private StringBuilder citation;

	@Override
	public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
		super.setup(resolver, objectModel, src, par);

		try {
			this.request = ObjectModelHelper.getRequest(objectModel);
			this.response = ObjectModelHelper.getResponse(objectModel);
			Context context = ContextUtil.obtainContext(objectModel);

			String handle = par.getParameter("handle");
			DSpaceObject dso = HandleManager.resolveToObject(context, handle);
			Item item = (Item) dso;

			filename = "cite-" + handle.replaceAll("/", "-") + ".ris";
			citation = new StringBuilder();
			citation.append("Provider: DSpace RIS Export").append("\n");
			citation.append("Database: ").append(ConfigurationManager.getProperty("dspace.name")).append("\n");
			citation.append("Content: text/plain; charset=\"UTF-8\"").append("\n");
			citation.append("\n\n"); // two line breaks to separate document header from reference data
			String field = "TY";
			String content = getRISType(item);
			appendLine(field, content);
			String title = getTitle(item);
			if (StringUtils.isNotBlank(title)) {
				appendLine("TI", title);
			}
			// TODO implement properly
			appendValueLines(item, "dc.contributor.author", "AU");
			// TODO implement properly
			appendValueLines(item, "dc.subject", "KW");
			appendLine("UR", HandleManager.getCanonicalForm(handle));
			appendValueLines(item, "dc.identifier.doi", "DO");
			String year = getPublicationYear(item);
			if (StringUtils.isNotBlank(year)) {
				appendLine("PY", year);
			}
			appendLine("ER", "");
		} catch (SQLException | ParameterException e) {
			throw new ProcessingException("Unable to export citation.", e);
		}
	}

	private String getPublicationYear(Item item) {
		List<String> issueDates = getValues(item, "dc.date.issued");
		if (!issueDates.isEmpty()) {
			String firstDateString = issueDates.get(0);
			DCDate asDate = new DCDate(firstDateString);
			return String.valueOf(asDate.getYearUTC());
		}
		return "";
	}

	private void appendValueLines(Item item, String field1, String valueField) {
		List<String> values = getValues(item, field1);
		for (String value : values) {
			appendLine(valueField, value);
		}
	}

	private List<String> getValues(Item item, String field) {
		List<String> result = new ArrayList<>();
		DCValue[] dcValues = item.getMetadata(field);
		for (DCValue dcValue : dcValues) {
			if (dcValue.value != null) {
				result.add(dcValue.value);
			}
		}
		return result;
	}

	private void appendLine(String field, String content) {
		citation.append(field).append(SEPARATOR).append(content).append("\n");
	}

	private String getTitle(Item item) {
		// TODO implement
		return item.getName();
	}

	private String getRISType(Item item) {
		return "JOUR"; // TODO implement
	}

	@Override
	public void generate() throws IOException, SAXException, ProcessingException {
		response.setContentType("application/x-research-info-systems");
		response.setHeader("Content-Disposition", "attachment; filename=" + filename);

		out.write(citation.toString().getBytes("UTF-8"));
		out.flush();
		out.close();
	}

	@Override
	public void recycle() {
		this.request = null;
		this.response = null;
	}
}
