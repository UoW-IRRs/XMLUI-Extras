package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import nz.ac.waikato.its.dspace.reporting.ReportGenerator;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;

/**
 * @author Stefan Mutter (stefanm@waikato.ac.nz) for University of Waikato, ITS
 */
public class StandardReportsAction  extends AbstractAction {

    private static final Logger log = Logger.getLogger(StandardReportsAction.class);

    public static final String STATUS = "status";
    public static final String EMAIL = "email";
    public static final String FAILURE = "failure";
    public static final String SUCCESS = "success";
    public static final String MESSAGE = "message";
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";

    private static final String NO_REPORT = "uow.aspects.Reports.StandardReportsAction.no_report";
    private static final String NO_EMAIL = "uow.aspects.Reports.StandardReportsAction.no_email";
    private static final String NO_START = "uow.aspects.Reports.StandardReportsAction.no_start_date";
    private static final String NO_END = "uow.aspects.Reports.StandardReportsAction.no_end_date";
    private static final String END_BEFORE_START = "uow.aspects.Reports.StandardReportsAction.end_before_start";
    private static final String ERROR = "uow.aspects.Reports.StandardReportsAction.error";
    private static final String REQUEST_REPORT = "request_report";

    @Override
    public Map act(Redirector redirector, SourceResolver sourceResolver, Map map, String s, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(map);
        Map<String,String> returnMap = new HashMap<String,String>();
        String email = request.getParameter("email");
        String reportName = request.getParameter("report_name");
        String requestName = REQUEST_REPORT + "_" + reportName;
        Context context = new Context();

        Date startDate = null;
        Date endDate = null;
        String startDateString = request.getParameter("fromDate");
        String endDateString = request.getParameter("toDate");

        if(startDateString != null &&  !startDateString.equals("")){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            startDate = sdf.parse(startDateString);
            returnMap.put(START_DATE,sdf.format(startDate));
        }
        if(endDateString != null && !endDateString.equals("")){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            endDate = sdf.parse(endDateString);
            returnMap.put(END_DATE,sdf.format(endDate));
        }

        if(email == null || email.equals("") || reportName == null || reportName.equals("") || startDate == null || endDate == null
                || endDate.before(startDate) ){
            returnMap.put(STATUS, FAILURE);
            if(email == null){
                email = "";
            }
            returnMap.put(EMAIL,email);

            if(reportName == null || reportName.equals("")){
                returnMap.put(MESSAGE,NO_REPORT);
            } else if (email == null || email.equals("")){
                returnMap.put(MESSAGE,NO_EMAIL);
            } else if (startDate == null){
                returnMap.put(MESSAGE,NO_START);
            } else if (endDate == null){
                returnMap.put(MESSAGE,NO_END);
            } else if (endDate.before(startDate)){
                returnMap.put(MESSAGE,END_BEFORE_START);
            }
            log.warn(LogManager.getHeader(context,requestName,"Failure sending report " + reportName + " to email address " +  email));
        } else{
            try {
                returnMap.put(EMAIL,email);
                ReportGenerator.emailReport(startDate, endDate, reportName, email);
                returnMap.put(STATUS, SUCCESS);
                log.info(LogManager.getHeader(context,requestName,"Successfully sent " + reportName + " to email address " +  email));
            } catch (Exception e){
              returnMap.put(STATUS,FAILURE);
              returnMap.put(MESSAGE,ERROR);
            }
        }
        try{
            context.complete();
            context = null;

        } catch (Exception e){
            if(context != null && context.isValid()){
                context.abort();
            }
        } finally {
            if(context != null && context.isValid()){
                context.abort();
            }
        }
        return returnMap;
    }
}
