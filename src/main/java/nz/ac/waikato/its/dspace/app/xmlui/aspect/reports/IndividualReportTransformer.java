package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import nz.ac.waikato.its.dspace.reporting.ReportConfigurationService;
import nz.ac.waikato.its.dspace.reporting.ReportGenerator;
import nz.ac.waikato.its.dspace.reporting.ReportingException;
import nz.ac.waikato.its.dspace.reporting.configuration.*;
import nz.ac.waikato.its.dspace.reporting.configuration.Field;
import org.apache.cocoon.ProcessingException;
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
    private static final Message T_success = message("uow.aspects.Reports.success");
    private static final Message T_fail = message("uow.aspects.Reports.fail");
    private static final Message T_start_date_label = message("uow.aspects.Reports.IndividualReportTransformer.start_date_label");
    private static final Message T_start_date_help = message("uow.aspects.Reports.IndividualReportTransformer.start_date_help");
    private static final Message T_end_date_label = message("uow.aspects.Reports.IndividualReportTransformer.end_date_label");
    private static final Message T_end_date_help = message("uow.aspects.Reports.IndividualReportTransformer.end_date_help");
	private static final Message T_pick_values_label = message("uow.aspects.Reports.IndividualReportTransformer.pick_values_label");
	private static final Message T_pick_values_help = message("uow.aspects.Reports.IndividualReportTransformer.pick_values_help");
	private static final Message T_cancel = message("xmlui.general.cancel");

	@Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {
        Division reportHome = body.addDivision("standard-report-home");
        Division report = reportHome.addDivision("standard-report-div","standard-report");

        String configDir = ConfigurationManager.getProperty("dspace.dir") + "/config/modules/reporting";
        ReportConfigurationService configurationService = new ReportConfigurationService(configDir);
        String reportName = parameters.getParameter("reportName", "");

	    Report requestedReport = null;
        if(!reportName.equals("")) {
	        try {
		        requestedReport = configurationService.getCannedReportConfiguration(reportName);
	        } catch (ConfigurationException e) {
		        report.setHead(T_report_standard_name);
		        log.error("Unable to load report from configuration");
	        }
        }
        if (requestedReport == null) {
	        log.error("Cannot find requested report, name " + reportName);
	        throw new ProcessingException("Unable to load report from configuration");
        }

	    report.setHead(requestedReport.getTitle());

	    List reportInfo = report.addList("report-info", List.TYPE_FORM, "report-info");
	    try {
		    ReportUtils.addReportEntry(contextPath, reportInfo, requestedReport);
	    } catch (ConfigurationException e) {
		    log.warn("Cannot add report information, encountered exception " + e.getMessage(), e);
	    }

	    String success = parameters.getParameter(StandardReportsAction.STATUS,"");
        if(success.equals(StandardReportsAction.SUCCESS)){
            report.addDivision("general-message","notice success alert alert-success").addPara(T_success.parameterize(parameters.getParameter(StandardReportsAction.EMAIL, "")));
        } else if(success.equals(StandardReportsAction.FAILURE)){
            Division noticeDiv = report.addDivision("general-message", "notice danger alert alert-danger");
            noticeDiv.addPara(T_fail);
            String message = parameters.getParameter(StandardReportsAction.MESSAGE,"");
            if(!message.equals("")){
                noticeDiv.addPara(message(message));
            }

        }

        Division div = report.addInteractiveDivision("standard-report-form", contextPath + "/reports/standard/" + reportName, Division.METHOD_POST);
        List form = div.addList("set-report-params",List.TYPE_FORM);

        Hidden reportNameField = form.addItem().addHidden("reportName");
        reportNameField.setValue(reportName);

        Text startDateText = form.addItem().addText("fromDate","from");
        startDateText.setLabel(T_start_date_label);
        startDateText.setHelp(T_start_date_help);

        Text endDateText = form.addItem().addText("toDate","to");
        endDateText.setLabel(T_end_date_label);
        endDateText.setHelp(T_end_date_help);

	    java.util.List<Field> fields = requestedReport.getFields();
	    for (Field field : fields) {
		    addFieldOptions(form, requestedReport, field);
	    }

	    Text emailAddressField = form.addItem().addText("email");
        emailAddressField.setLabel(T_email_address_label);
        emailAddressField.setHelp(T_email_address_help);

        if(success.equals(StandardReportsAction.FAILURE)){
            prepopulateValue(StandardReportsAction.EMAIL,emailAddressField);
            prepopulateValue(StandardReportsAction.START_DATE,startDateText);
            prepopulateValue(StandardReportsAction.END_DATE,endDateText);
	        // TODO prepopulate pick values?
        }
		Para actions = div.addPara();
		actions.addButton("submit_report", "btn-primary").setValue(T_submit);
		actions.addButton("submit_cancel").setValue(T_cancel);
    }

	private void addFieldOptions(List form, Report report, Field field) throws WingException, UIException {
		if (field.getValuesMode() == Field.ValuesMode.ALL || field.getValuesMode() == Field.ValuesMode.SEARCH) {
			return; // do nothing for these types -- TODO later implement search mode
		}
		try {
			java.util.List<String> pickableValues = ReportGenerator.getPickableValues(report, field);
			if (pickableValues.isEmpty()) {
				return; // do nothing if there are no values -- TODO revisit this decision, doesn't this mean there will be no data in the report?
			}
			Select pickSelect = form.addItem().addSelect("values-" + field.getName());
			String header = field.getHeader().replaceAll("_", " ");
			pickSelect.setLabel(T_pick_values_label.parameterize(header));
			pickSelect.setHelp(T_pick_values_help.parameterize(header));
			pickSelect.setMultiple(true);
			pickSelect.setRequired(false);
			for (String value : pickableValues) {
				pickSelect.addOption(value, value);
			}
		} catch (ReportingException e) {
			log.error("Cannot find pickable values for field");
			throw new UIException(e);
		}
	}

	private void prepopulateValue(String reportsActionName,Text field) throws WingException {
        String preSetValues;
        preSetValues = parameters.getParameter(reportsActionName,"");
        if(!preSetValues.equals("")){
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
