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

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {
        Division reportHome = body.addDivision("standard-report-home");
        Division report = reportHome.addDivision("standard-report-div");
        report.setHead(T_standard_report);

        String success = parameters.getParameter(StandardReportsAction.STATUS,"");
        if(success.equals(StandardReportsAction.SUCCESS)){
            report.addDivision("general-message","notice success alert alert-success").addPara(T_success.parameterize(parameters.getParameter(StandardReportsAction.EMAIL, "")));
        } else if(success.equals(StandardReportsAction.FAILURE)){
            report.addDivision("general-message","notice danger alert alert-danger").addPara(T_fail);
        }

        Division div = report.addInteractiveDivision("standard-report-form", contextPath + "/reports/standard", Division.METHOD_POST);
        List form = div.addList("choose-report",List.TYPE_FORM);
        Select reportName = form.addItem().addSelect("report_name");
        reportName.setMultiple(false);
        reportName.addOption(0,"Report 1: AgResearch Group and Team | Output Type and Subtype | Title | Date Submitted | Citation | AgScite Handle");
        reportName.setOptionSelected(0);
        reportName.setLabel(T_reportName_label);
        reportName.setHelp(T_reportName_help);
        Text emailAddressField = form.addItem().addText("email");
        emailAddressField.setLabel(T_email_address_label);
        emailAddressField.setHelp(T_email_address_help);
        div.addPara().addButton("submit_report").setValue(T_submit);
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }
}
