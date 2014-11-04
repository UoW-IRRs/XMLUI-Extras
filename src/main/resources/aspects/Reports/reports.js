importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);
importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);
importClass(Packages.nz.ac.waikato.its.dspace.app.xmlui.aspect.reports.ReportUtils)


/**
 * Simple access method to access the current cocoon object model.
 */
function getObjectModel()
{
    return FlowscriptUtils.getObjectModel(cocoon);
}
/**
 * Return the DSpace context for this request since each HTTP request generates
 * a new context this object should never be stored and instead allways accessed
 * through this method so you are ensured that it is the correct one.
 */
function getDSContext()
{
    return ContextUtil.obtainContext(getObjectModel());
}
/**
 * Send the current page and wait for the flow to be continued. This method will
 * preform two usefull actions: set the flow parameter & add result information.
 *
 * The flow parameter is used by the sitemap to seperate requests comming from a
 * flow script from just normal urls.
 *
 * The result object could potentialy contain a notice message and a list of
 * errors. If either of these are present then they are added to the sitemap's
 * parameters.
 */
function sendPageAndWait(uri,bizData,result)
{
    if (bizData == null)
        bizData = {};
    if (result != null)
    {
        var outcome = result.getOutcome();
        var header = result.getHeader();
        var message = result.getMessage();
        var characters = result.getCharacters();
        if (message != null || characters != null)
        {
            bizData["notice"] = "true";
            bizData["outcome"] = outcome;
            bizData["header"] = header;
            bizData["message"] = message;
            bizData["characters"] = characters;
        }
        var errors = result.getErrorString();
        if (errors != null)
        {
            bizData["errors"] = errors;
        }
    }
// just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPageAndWait(uri,bizData);
}
/**
 * Send the given page and DO NOT wait for the flow to be continued. Execution will
 * proceed as normal. This method will perform two useful actions: set the flow
 * parameter & add result information.
 *
 * The flow parameter is used by the sitemap to separate requests coming from a
 * flow script from just normal urls.
 *
 * The result object could potentially contain a notice message and a list of
 * errors. If either of these are present then they are added to the sitemap's
 * parameters.
 */
function sendPage(uri,bizData,result)
{
    if (bizData == null)
        bizData = {};
    if (result != null)
    {
        var outcome = result.getOutcome();
        var header = result.getHeader();
        var message = result.getMessage();
        var characters = result.getCharacters();
        if (message != null || characters != null)
        {
            bizData["notice"] = "true";
            bizData["outcome"] = outcome;
            bizData["header"] = header;
            bizData["message"] = message;
            bizData["characters"] = characters;
        }
        var errors = result.getErrorString();
        if (errors != null)
        {
            bizData["errors"] = errors;
        }
    }
// just to remember where we came from.
    bizData["flow"] = "true";
    cocoon.sendPage(uri,bizData);
}

/*********************
 * Entry Point flow
 *********************/

/**
 * Start requesting/sending report
 */
function startSendStandardReport()
{
    var reportName = cocoon.parameters.reportName;
    if (reportName != "")
    {
        var result = doSendStandardReport(reportName);
    }
    cocoon.redirectTo(cocoon.request.getContextPath() + "/reports/standard", true);
    getDSContext().complete();
    cocoon.exit();
}


/*********************
 * Reporting flows
 *********************/

function doSendStandardReport(reportName)
{
    var email;
    var startDate;
    var endDate;
    var result;
    do {
        sendPageAndWait("reports/standard/" + reportName + "/settings", {"reportName": reportName, "email": email, "startDate": startDate, "endDate": endDate}, result);

        // reset result
        result = null;
        // respond to button clicks
        if (cocoon.request.get("submit_report")) {
            result = ReportUtils.processSendReport(getDSContext(),cocoon.request);
            // keep looping so we can show success / error message

            // get variables
            email =  cocoon.request.get("email");
            startDate = cocoon.request.get("fromDate");
            endDate = cocoon.request.get("toDate");
        } else if (cocoon.request.get("submit_cancel")) {
            // go back to where we came from
            return null;
        }
    } while (true)
}
