package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import nz.ac.waikato.its.dspace.reporting.ReportGenerator;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Stefan Mutter (stefanm@waikato.ac.nz) for University of Waikato, ITS
 */
public class StandardReportsAction  extends AbstractAction {

    private static final Logger log = Logger.getLogger(StandardReportsAction.class);

    public static final String STATUS = "status";
    public static final String EMAIL = "email";
    public static final String FAILURE = "failure";
    public static final String SUCCESS = "success";

    @Override
    public Map act(Redirector redirector, SourceResolver sourceResolver, Map map, String s, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(map);
        String email = request.getParameter("email");
        String reportName = "report1";
        Map<String,String> returnMap = new HashMap<String,String>();
        if(email == null || email.equals("") || reportName == null || reportName.equals("")){
            returnMap.put(STATUS, FAILURE);
            if(email == null){
                email = "";
            }
            returnMap.put(EMAIL,email);
            log.warn("Failure sending report with name " + reportName + " to email address " +  email);
        } else {
            returnMap.put(EMAIL,email);
            try {
                ReportGenerator.emailReport(null,null,reportName,email);
                returnMap.put(STATUS, SUCCESS);
                log.info("Successfully sent report with name " + reportName + " to email address " +  email);
            } catch (Exception e){
              log.error("Error sending report with name " + reportName + " to email address " +  email,e);
              returnMap.put(STATUS,FAILURE);
            }
        }
        return returnMap;
    }
}
