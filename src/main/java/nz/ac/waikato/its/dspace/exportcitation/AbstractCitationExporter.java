package nz.ac.waikato.its.dspace.exportcitation;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz
 *         for the University of Waikato's Institutional Research Repositories
 */
public abstract class AbstractCitationExporter extends AbstractReader implements Recyclable {
	public static final String FORMAT_ENDNOTE = "endnote";
	public static final String FORMAT_WORD_CITATIONONLY = "word-citation";
	public static final String FORMAT_WORD_CITATIONABSTRACT = "word-citationabstract";

	protected static final Logger log = Logger.getLogger(ItemListCitationExporter.class);
	protected static Map<String, CitationDisseminationCrosswalk> formatToCrosswalk = new HashMap<>();
	protected static Map<String, String> mimetypeToExtension = new HashMap<>();
	protected Request request;
	protected Response response;
	protected Context context;
	protected CitationDisseminationCrosswalk crosswalk;

	protected String filename;
	protected List<Item> items = new ArrayList<>();

	static {
		if (formatToCrosswalk.isEmpty()) {
			formatToCrosswalk.put(FORMAT_ENDNOTE, new EndnoteExportCrosswalk());
			formatToCrosswalk.put(FORMAT_WORD_CITATIONONLY, new WordCitationExportCrosswalk(false));
			formatToCrosswalk.put(FORMAT_WORD_CITATIONABSTRACT, new WordCitationExportCrosswalk(true));
		}
	}

	static {
		mimetypeToExtension.put("application/x-research-info-systems", ".ris");
		mimetypeToExtension.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
	}

	@Override
	public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
		super.setup(resolver, objectModel, src, par);

		try {
			this.request = ObjectModelHelper.getRequest(objectModel);
			this.response = ObjectModelHelper.getResponse(objectModel);
			context = ContextUtil.obtainContext(objectModel);

			String format = par.getParameter("format", "endnote");
			if (formatToCrosswalk.containsKey(format)) {
				crosswalk = formatToCrosswalk.get(format);
			} else {
				throw new ProcessingException("Unknown citation export format " + format + ", cannot proceed");
			}

			initItemsAndFilename(par);
		} catch (SQLException | ParameterException e) {
			throw new ProcessingException("Unable to export citation.", e);
		}
	}

	protected abstract void initItemsAndFilename(Parameters par) throws ParameterException, SQLException, ProcessingException;

	@Override
	public void generate() throws IOException, SAXException, ProcessingException {
		if (items == null || items.isEmpty()) {
			log.warn("Generate called but list of items to disseminate is empty, aborting");
			return;
		}

		response.setContentType(crosswalk.getMIMEType());
		response.setHeader("Content-Disposition", "attachment; filename=" + filename + getExtension(crosswalk.getMIMEType()));

		try {
			if (items.size() == 1) {
				crosswalk.disseminate(context, items.get(0), out);
			} else {
				crosswalk.disseminateList(context, items, out);
			}
		} catch (CrosswalkException | AuthorizeException | SQLException e) {
			log.error("Problem disseminating item/s: " + e.getMessage(), e);
		}

		out.flush();
		out.close();
	}

	private String getExtension(String mimeType) {
		if (mimetypeToExtension.containsKey(mimeType)) {
			return mimetypeToExtension.get(mimeType);
		}
		return "";
	}

	@Override
	public void recycle() {
		this.request = null;
		this.response = null;
		this.context = null;
		this.filename = null;
		this.items = new ArrayList<>();
		this.crosswalk = null;
	}
}
