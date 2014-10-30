package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import nz.ac.waikato.its.dspace.reporting.configuration.ConfigurationException;
import nz.ac.waikato.its.dspace.reporting.configuration.Field;
import nz.ac.waikato.its.dspace.reporting.configuration.Report;
import org.dspace.app.xmlui.wing.AbstractWingTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;

import java.util.List;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 */
public class ReportUtils {
	private static final Message T_field_names = AbstractWingTransformer.message("uow.aspects.Reports.field-names");

	static void addReportEntry(String contextPath, org.dspace.app.xmlui.wing.element.List parent, Report report) throws WingException, ConfigurationException {
		parent.addItem("report-description-" + report.getId(), "report-description").addContent(AbstractWingTransformer.message("uow.aspects.Reports.description." + report.getId()));
		List<Field> fields = report.getFields();
		String fieldNames = "";
		boolean first = true;
		for(Field field : fields){
			if (first) {
				first = false;
			} else {
				fieldNames += " | ";
			}
		   fieldNames += field.getHeader().replace("_"," ");
		}
		if(!fieldNames.equals("|")){
		    parent.addItem("report-fields-" + report.getId(), "report-fields").addContent(T_field_names.parameterize(fieldNames));
		}
	}
}
