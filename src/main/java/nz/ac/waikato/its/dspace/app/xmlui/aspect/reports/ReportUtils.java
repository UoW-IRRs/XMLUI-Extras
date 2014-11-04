package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import nz.ac.waikato.its.dspace.reporting.ReportGenerator;
import nz.ac.waikato.its.dspace.reporting.configuration.ConfigurationException;
import nz.ac.waikato.its.dspace.reporting.configuration.Field;
import nz.ac.waikato.its.dspace.reporting.configuration.Report;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.wing.AbstractWingTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import javax.servlet.ServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for Waikato University ITS
 */
public class ReportUtils {
	private static final Logger log = Logger.getLogger(ReportUtils.class);

	public static final String STATUS = "status";
	public static final String EMAIL = "email";
	public static final String FAILURE = "failure";
	public static final String START_DATE = "startDate";
	public static final String END_DATE = "endDate";
	public static final String REPORT_NAME = "reportName";

	private static final String REQUEST_REPORT = "request_report";

	private static final Message T_field_names = AbstractWingTransformer.message("uow.aspects.Reports.field-names");
	private static final Message T_success_header = new Message("", "uow.aspects.Reports.success_header");
	private static final Message T_fail_header = new Message("", "uow.aspects.Reports.fail_header");
	private static final Message T_success_message = new Message("", "uow.aspects.Reports.success");
	private static final Message T_fail_message = new Message("", "uow.aspects.Reports.fail");

	static void addReportEntry(org.dspace.app.xmlui.wing.element.List parent, Report report) throws WingException, ConfigurationException {
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

	public static FlowResult processSendReport(Context context, Request request) {
		FlowResult result = new FlowResult();

		String reportName = Objects.toString(request.get(REPORT_NAME), "");
		result.setParameter(REPORT_NAME, reportName);

		Date startDate = null;
		Date endDate = null;

		String startDateString = Objects.toString(request.get("fromDate"), "");
		if(StringUtils.isNotBlank(startDateString)){
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				startDate = sdf.parse(startDateString);
			} catch (ParseException e) {
				// ignore for now, handled later
			}
			result.setParameter(START_DATE, Objects.toString(startDateString, ""));
		}

		String endDateString = Objects.toString(request.get("toDate"), "");
		if(StringUtils.isNotBlank(endDateString)){
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				endDate = sdf.parse(endDateString);
			} catch (ParseException e) {
				// ignore for now, handled later
			}
			result.setParameter(END_DATE, Objects.toString(endDateString, ""));
		}

		Map<String, List<String>> pickedValues = findPickedValues(request);
		String pickFields = Objects.toString(request.get("pickFields"), "");
		List<String> missingFields = checkForMissingPickValues(pickFields, pickedValues);

		String email = Objects.toString(request.get(EMAIL), "");
		if(StringUtils.isBlank(email) || StringUtils.isBlank(reportName) || startDate == null || endDate == null || endDate.before(startDate) || !missingFields.isEmpty()){
			result.setParameter(STATUS, FAILURE);
			result.setContinue(false);

			result.setParameter(EMAIL, Objects.toString(email, ""));

			StringBuilder problem = new StringBuilder("");
			if(StringUtils.isBlank(reportName)){
				result.addError("reportName");
				if (problem.length() > 0) {
					problem.append(", ");
				}
				problem.append("missing report name");
			}
			if (email == null || email.equals("")){
				result.addError("email");
				if (problem.length() > 0) {
					problem.append(", ");
				}
				problem.append("missing email");
			}
			if (startDate == null){
				result.addError("startDate");
				if (problem.length() > 0) {
					problem.append(", ");
				}
				problem.append("missing start date");
			}
			if (endDate == null){
				result.addError("endDate");
				if (problem.length() > 0) {
					problem.append(", ");
				}
				problem.append("missing end date");
			}
			if (startDate != null && endDate != null && endDate.before(startDate)){
				result.addError("period");
				if (problem.length() > 0) {
					problem.append(", ");
				}
				problem.append("end date before start date");
			}
			if (!missingFields.isEmpty()) {
				for (String field : missingFields) {
					result.addError("values-" + field);
					if (problem.length() > 0) {
						problem.append(", ");
					}
					problem.append("missing pick values for ").append(field);
				}
			}
			result.setOutcome(false);
			result.setContinue(false);
			result.setHeader(T_fail_header);
			result.setMessage(T_fail_message);
			log.info(LogManager.getHeader(context, REQUEST_REPORT, reportName + " with " + problem + " not sent to " + email));
		} else{
			try {
				result.setParameter(EMAIL, email);
				ReportGenerator.emailReport(startDate, endDate, reportName, email, pickedValues);
				result.setHeader(T_success_header);
				result.setMessage(T_success_message);
				result.setOutcome(true);
				result.setContinue(true);
				log.info(LogManager.getHeader(context,REQUEST_REPORT,reportName + " sent to " +  email));
			} catch (Exception e){
				result.setHeader(T_fail_header);
				result.setMessage(T_fail_message);
				result.setOutcome(false);
				result.setContinue(false);
				log.error(LogManager.getHeader(context,REQUEST_REPORT,reportName + " failed to be sent to " +  email));
			}
		}
		return result;
	}

	private static List<String> checkForMissingPickValues(String pickFields, Map<String, List<String>> pickedValues) {
		String[] fields = StringUtils.isNotBlank(pickFields) ? pickFields.split(",") : new String[0];
		List<String> missingFields = new ArrayList<>();
		for (String field : fields) {
			if (!pickedValues.containsKey(field)) {
				missingFields.add(field);
			}
		}
		return missingFields;
	}

	static Map<String, List<String>> findPickedValues(ServletRequest request) {
		Map<String, List<String>> result = new HashMap<>();
		Enumeration params = request.getParameterNames();
		while (params.hasMoreElements()) {
			Object name = params.nextElement();
			if (!name.toString().startsWith("values-")) {
				continue; // not a pick value parameter, ignore
			}
			String field = name.toString().substring("values-".length());
			String[] values = request.getParameterValues(name.toString());
			result.put(field, Arrays.asList(values));
		}
		return result;
	}
}
