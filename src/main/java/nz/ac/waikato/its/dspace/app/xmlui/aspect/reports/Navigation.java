package nz.ac.waikato.its.dspace.app.xmlui.aspect.reports;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Stefan Mutter (stefanm@waikato.ac.nz) for University of Waikato, ITS
 */

public class Navigation extends AbstractDSpaceTransformer {

    private final static Message T_list_head = message("uow.aspects.Reports.Navigation.options_list_head");
    private final static Message T_standard_item = message("uow.aspects.Reports.Navigation.standard_link_text");

    @Override
    public void addOptions(Options options) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        List reportList = options.addList("uow-reporting");
        reportList.setHead(T_list_head);
        reportList.addItemXref(contextPath + "/reports/standard", T_standard_item);
    }
}
