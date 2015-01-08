package nz.ac.waikato.its.dspace.exportcitation;

import org.dspace.content.DCValue;
import org.dspace.content.Item;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 */
public interface Converter {
	String appendOutput(StringBuilder builder, String risFieldName, String mdFieldName, DCValue[] value, Item item);
}
