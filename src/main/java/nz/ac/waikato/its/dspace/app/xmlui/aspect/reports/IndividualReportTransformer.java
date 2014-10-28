package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import nz.ac.waikato.its.dspace.reporting.ReportConfigurationService;
import nz.ac.waikato.its.dspace.reporting.configuration.ConfigurationException;
import nz.ac.waikato.its.dspace.reporting.configuration.Report;
import org.apache.cocoon.ProcessingException;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
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
    private static final Message T_dspace_home = message("uow.aspects.Reports.dspace_home");
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

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {
        Division reportHome = body.addDivision("standard-report-home");
        Division report = reportHome.addDivision("standard-report-div","standard-report");

        String configDir = ConfigurationManager.getProperty("dspace.dir") + "/config/modules/reporting";
        ReportConfigurationService configurationService = new ReportConfigurationService(configDir);
        String reportName = parameters.getParameter("reportName", "");

        if(!reportName.equals("")){
        try {
            Report requestedReport = configurationService.getCannedReportConfiguration(reportName);
            report.setHead(requestedReport.getTitle());
        } catch (ConfigurationException e) {
            report.setHead(T_report_standard_name);
            log.error("Unable to load report from configuration");
        }
        } else{
            log.error("Report name parameter is missing");
            throw new ProcessingException("Unable to load report from configuration");
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

        Text emailAddressField = form.addItem().addText("email");
        emailAddressField.setLabel(T_email_address_label);
        emailAddressField.setHelp(T_email_address_help);

        if(success.equals(StandardReportsAction.FAILURE)){
            prepopulateValue(StandardReportsAction.EMAIL,emailAddressField);
            prepopulateValue(StandardReportsAction.START_DATE,startDateText);
            prepopulateValue(StandardReportsAction.END_DATE,endDateText);
        }

        div.addPara().addButton("submit_report").setValue(T_submit);
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
