package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz
 *         for the University of Waikato's Institutional Research Repositories
 */
public class ReportingUserSelector extends AbstractLogEnabled implements Selector {
    @Override
    public boolean select(String expression, Map objectModel, Parameters parameters) {
        String reportingGroupName = ConfigurationManager.getProperty("reporting", "reporting.group");
        if (StringUtils.isBlank(reportingGroupName)) {
            return expression.equals("allowed");
        }
        try {
            Context context = ContextUtil.obtainContext(objectModel);
            Group reportingGroup = Group.findByName(context, reportingGroupName);
            if (reportingGroup != null) {
                if (reportingGroup.getID() == Group.ANONYMOUS_ID) {
                    return expression.equals("allowed");
                }
                EPerson eperson = context.getCurrentUser();
                return reportingGroup.isMember(eperson) && expression.equals("allowed");
            } else {
                getLogger().error("Could not find reporting group with name '" + reportingGroupName + "'");
            }
        } catch (SQLException e) {
            getLogger().error("Could not determine eligibility to access reporting functionality: " + e.getMessage(), e);
        }
        return !expression.equals("allowed");
    }
}
