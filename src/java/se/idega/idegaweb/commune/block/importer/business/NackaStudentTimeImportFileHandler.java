package se.idega.idegaweb.commune.block.importer.business;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.business.IBOService;

/**
 * Reads records with three ordered fields: ssn (string), school name (string)
 * and number of hours (integer). Ignores header rows.
 * <p>
 * Last modified: $Date: 2003/03/27 11:20:54 $ by $Author: staffan $
 *
 * @author <a href="http://www.staffannoteberg.com">Staffan Nöteberg</a>
 * @version $Revision: 1.1 $
 */
public interface NackaStudentTimeImportFileHandler
    extends IBOService, ImportFileHandler {
}
