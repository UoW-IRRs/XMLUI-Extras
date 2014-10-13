package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import org.apache.cocoon.ProcessingException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Stefan Mutter (stefanm@waikato.ac.nz) for University of Waikato, ITS
 */
public class StandardReportsTransformer extends AbstractDSpaceTransformer{

    private static final Message T_standard_report = message("uow.aspects.Reports.StandardReportsTransformer.home_head");
    private static final Message T_title = message("uow.aspects.Reports.StandardReportsTransformer.title");
    private static final Message T_dspace_home = message("uow.aspects.Reports.StandardReportsTransformer.dspace_home");
    private static final Message T_trail = message("uow.aspects.Reports.StandardReportsTransformer.trail");
    private static final Message T_email_address_label = message("uow.aspects.Reports.StandardReportsTransformer.email_address_label");
    private static final Message T_email_address_help = message("uow.aspects.Reports.StandardReportsTransformer.email_address_help");
    private static final Message T_submit = message("uow.aspects.Reports.StandardReportsTransformer.submit");
    private static final Message T_success = message("uow.aspects.Reports.StandardReportsTransformer.success");
    private static final Message T_fail = message("uow.aspects.Reports.StandardReportsTransformer.fail");
    private static final Message T_reportName_label = message("uow.aspects.Reports.StandardReportsTransformer.report_label");
    private static final Message T_reportName_help = message("uow.aspects.Reports.StandardReportsTransformer.report_help");
    private static final Message T_start_date_label = message("uow.aspects.Reports.StandardReportsTransformer.start_date_label");
    private static final Message T_start_date_help = message("uow.aspects.Reports.StandardReportsTransformer.start_date_help");
    private static final Message T_end_date_label = message("uow.aspects.Reports.StandardReportsTransformer.end_date_label");
    private static final Message T_end_date_help = message("uow.aspects.Reports.StandardReportsTransformer.end_date_help");

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {
        Division reportHome = body.addDivision("standard-report-home");
        Division report = reportHome.addDivision("standard-report-div","standard-report");
        report.setHead(T_standard_report);

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

        Division div = report.addInteractiveDivision("standard-report-form", contextPath + "/reports/standard", Division.METHOD_POST);
        List form = div.addList("choose-report",List.TYPE_FORM);

        Select reportName = form.addItem().addSelect("report_name");
        reportName.setMultiple(false);
        reportName.addOption("report1","All outputs by Group and Team");
        reportName.setOptionSelected(0);
        reportName.setLabel(T_reportName_label);
        reportName.setHelp(T_reportName_help);

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
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }
}
