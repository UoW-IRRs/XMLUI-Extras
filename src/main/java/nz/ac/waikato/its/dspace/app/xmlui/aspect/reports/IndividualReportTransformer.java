package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import edu.emory.mathcs.backport.java.util.Collections;
import nz.ac.waikato.its.dspace.reporting.ReportConfigurationService;
import nz.ac.waikato.its.dspace.reporting.ReportGenerator;
import nz.ac.waikato.its.dspace.reporting.ReportingException;
import nz.ac.waikato.its.dspace.reporting.configuration.*;
import nz.ac.waikato.its.dspace.reporting.configuration.Field;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Stefan Mutter (stefanm@waikato.ac.nz) for University of Waikato, ITS
 */
public class IndividualReportTransformer extends AbstractDSpaceTransformer{
    private static final Logger log = Logger.getLogger(IndividualReportTransformer.class);

    private static final Message T_report_standard_name = message("uow.aspects.Reports.IndividualReportTransformer.home_head");
    private static final Message T_title = message("uow.aspects.Reports.IndividualReportTransformer.title");
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_trail = message("uow.aspects.Reports.IndividualReportTransformer.trail");
    private static final Message T_email_address_label = message("uow.aspects.Reports.IndividualReportTransformer.email_address_label");
    private static final Message T_email_address_help = message("uow.aspects.Reports.IndividualReportTransformer.email_address_help");
    private static final Message T_submit = message("uow.aspects.Reports.IndividualReportTransformer.submit");
    private static final Message T_start_date_label = message("uow.aspects.Reports.IndividualReportTransformer.start_date_label");
    private static final Message T_start_date_help = message("uow.aspects.Reports.IndividualReportTransformer.start_date_help");
    private static final Message T_end_date_label = message("uow.aspects.Reports.IndividualReportTransformer.end_date_label");
    private static final Message T_end_date_help = message("uow.aspects.Reports.IndividualReportTransformer.end_date_help");
	private static final Message T_pick_values_label = message("uow.aspects.Reports.IndividualReportTransformer.pick_values_label");
	private static final Message T_pick_values_help = message("uow.aspects.Reports.IndividualReportTransformer.pick_values_help");
	private static final Message T_cancel = message("xmlui.general.cancel");

	private static final Message T_no_pick_values = message("uow.aspects.Reports.IndividualReportTransformer.no_pick_values");
	private static final Message T_no_email = message("uow.aspects.Reports.IndividualReportTransformer.no_email");
	private static final Message T_no_start = message("uow.aspects.Reports.IndividualReportTransformer.no_start_date");
	private static final Message T_no_end = message("uow.aspects.Reports.IndividualReportTransformer.no_end_date");
	private static final Message T_end_before_start = message("uow.aspects.Reports.IndividualReportTransformer.end_before_start");
	private static final Message T_missing_pick_values = message("uow.aspects.Reports.IndividualReportTransformer.missing_pick_values");

	@Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {
        Division reportHome = body.addDivision("standard-report-home");
        Division report = reportHome.addDivision("standard-report-div","standard-report");

        String configDir = ConfigurationManager.getProperty("dspace.dir") + "/config/modules/reporting";
        ReportConfigurationService configurationService = new ReportConfigurationService(configDir);
        String reportName = parameters.getParameter("reportName", "");

	    Report requestedReport = null;
		try {
			requestedReport = configurationService.getCannedReportConfiguration(reportName);
		} catch (ConfigurationException e) {
			report.setHead(T_report_standard_name);
			log.error("Unable to load report from configuration");
		}

		if (requestedReport == null) {
			log.error("Cannot find requested report, name " + reportName);
			throw new ProcessingException("Unable to load report from configuration");
		}

		report.setHead(requestedReport.getTitle());


		Division reportEntry = report.addDivision("report-info-" + requestedReport.getId(), "report-info panel panel-default");
	    try {
		    ReportUtils.addReportEntry(reportEntry, requestedReport);
	    } catch (ConfigurationException e) {
		    log.warn("Cannot add report information, encountered exception " + e.getMessage(), e);
	    }

		String errorString = parameters.getParameter("errors", "");
		java.util.List<String> errors = StringUtils.isNotBlank(errorString) ? Arrays.asList(errorString.split(",")) : Collections.emptyList();

        Division div = report.addInteractiveDivision("standard-report-form", contextPath + "/reports/standard/" + reportName, Division.METHOD_POST);
        List form = div.addList("set-report-params",List.TYPE_FORM);

        Hidden reportNameField = form.addItem().addHidden("reportName");
        reportNameField.setValue(reportName);

        Text startDateText = form.addItem().addText("fromDate","from");
		if (errors.contains("startDate")) {
			startDateText.addError(T_no_start);
		}
        startDateText.setLabel(T_start_date_label);
        startDateText.setHelp(T_start_date_help);
		startDateText.setRequired(true);

        Text endDateText = form.addItem().addText("toDate","to");
		if (errors.contains("endDate")) {
			endDateText.addError(T_no_end);
		}
		if (errors.contains("period")) {
			endDateText.addError(T_end_before_start);
		}
		endDateText.setLabel(T_end_date_label);
        endDateText.setHelp(T_end_date_help);
		endDateText.setRequired(true);

	    java.util.List<Field> fields = requestedReport.getFields();
	    for (Field field : fields) {
		    addFieldOptions(form, requestedReport, field, errors);
	    }

	    Text emailAddressField = form.addItem().addText("email");
		if (errors.contains("email")) {
			emailAddressField.addError(T_no_email);
		}
        emailAddressField.setLabel(T_email_address_label);
        emailAddressField.setHelp(T_email_address_help);
		emailAddressField.setRequired(true);

        if(!errors.isEmpty()){
            prepopulateValue("email", emailAddressField);
            prepopulateValue("fromDate", startDateText);
            prepopulateValue("toDate", endDateText);
	        // TODO prepopulate pick values?
        }
		Para actions = div.addPara();
		actions.addButton("submit_report").setValue(T_submit);
		actions.addButton("submit_cancel").setValue(T_cancel);

		div.addHidden("reports-continue").setValue(knot.getId());
    }

	private void addFieldOptions(List form, Report report, Field field, java.util.List<String> errors) throws WingException, UIException {
		if (field.getValuesMode() == Field.ValuesMode.ALL || field.getValuesMode() == Field.ValuesMode.SEARCH) {
			return; // do nothing for these types -- TODO later implement search mode
		}
		try {
			String header = field.getHeader().replaceAll("_", " ");
			java.util.List<String> pickableValues = ReportGenerator.getPickableValues(report, field);
			if (pickableValues.isEmpty()) {
				form.addItem("values-" + field.getName(), "notice danger alert alert-danger").addContent(T_no_pick_values.parameterize(header));
			} else {
				Select pickSelect = form.addItem().addSelect("values-" + field.getName());
				pickSelect.setLabel(T_pick_values_label.parameterize(header));
				pickSelect.setHelp(T_pick_values_help.parameterize(header));
				pickSelect.setMultiple(true);
				pickSelect.setRequired(true);
				if (errors.contains("values-" + field.getName())) {
					pickSelect.addError(T_missing_pick_values);
				}
				for (String value : pickableValues) {
					pickSelect.addOption(value, value);
				}
			}
			form.addItem().addHidden("pickFields").setValue(field.getName());
		} catch (ReportingException e) {
			log.error("Cannot find pickable values for field");
			throw new UIException(e);
		}
	}

	private void prepopulateValue(String reportsActionName,Text field) throws WingException {
		Request request = ObjectModelHelper.getRequest(objectModel);
        String preSetValues;
        preSetValues = request.getParameter(reportsActionName);
        if(!StringUtils.isBlank(preSetValues)){
            field.setValue(preSetValues);
        }
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        String configDir = ConfigurationManager.getProperty("dspace.dir") + "/config/modules/reporting";
        ReportConfigurationService configurationService = new ReportConfigurationService(configDir);
        try {
            String title = configurationService.getCannedReportConfiguration(parameters.getParameter("reportName", "")).getTitle();
            pageMeta.addMetadata("title").addContent(title);
            pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
            pageMeta.addTrailLink(contextPath + "/reports/standard", T_trail);
            pageMeta.addTrail().addContent(title);
        } catch (ConfigurationException e) {
            log.warn("Unable to build page trail",e);
            pageMeta.addMetadata("title").addContent(T_title);
            pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
            pageMeta.addTrailLink(contextPath + "/reports/standard", T_trail);
            pageMeta.addTrail().addContent(T_title);
        }
    }
}
