package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import nz.ac.waikato.its.dspace.reporting.ReportGenerator;
import nz.ac.waikato.its.dspace.reporting.configuration.Field;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import javax.servlet.ServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

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
    public static final String REPORT_NAME = "reportName";

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
	    Context context = ContextUtil.obtainContext(request);

        Map<String,String> returnMap = new HashMap<String,String>();
        String email = request.getParameter("email");
        String reportName = request.getParameter("reportName");
        returnMap.put(REPORT_NAME,reportName);

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

            String problem = "";
            if(reportName == null || reportName.equals("")){
                returnMap.put(MESSAGE,NO_REPORT);
                problem = "missing report name";
                if(reportName == null){
                    reportName = "";
                }
            } else if (email == null || email.equals("")){
                returnMap.put(MESSAGE,NO_EMAIL);
                problem = "missing email";
            } else if (startDate == null){
                returnMap.put(MESSAGE,NO_START);
                problem = "missing start date";
            } else if (endDate == null){
                returnMap.put(MESSAGE,NO_END);
                problem = "missing end date";
            } else if (endDate.before(startDate)){
                returnMap.put(MESSAGE,END_BEFORE_START);
                problem = "end date before start date";
            }
            log.info(LogManager.getHeader(context,REQUEST_REPORT,reportName + " with "+ problem +" not sent to " +  email));
        } else{
            try {
                returnMap.put(EMAIL,email);
	            Map<String, List<String>> pickedValues = findPickedValues(request);
                ReportGenerator.emailReport(startDate, endDate, reportName, email, pickedValues);
                returnMap.put(STATUS, SUCCESS);
                log.info(LogManager.getHeader(context,REQUEST_REPORT,reportName + " sent to " +  email));
            } catch (Exception e){
              returnMap.put(STATUS,FAILURE);
              returnMap.put(MESSAGE,ERROR);
              log.error(LogManager.getHeader(context,REQUEST_REPORT,reportName + " failed to be sent to " +  email));
            }
        }
        return returnMap;
    }

	private Map<String, List<String>> findPickedValues(ServletRequest request) {
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
