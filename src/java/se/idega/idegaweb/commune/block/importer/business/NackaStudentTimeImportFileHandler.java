package se.idega.idegaweb.commune.block.importer.business;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.business.IBOService;

/**
 * Reads records with three ordered fields: ssn (string), school name (string)
 * and number of hours (integer) and stores them as rows in SCH_TIME. Ignores
 * header rows. If a school name is unknown, then 'null' is stored in db. If a
 * ssn is unknown then a 'special citizen' is created with that ssn.
 * <p>
 * Last modified: $Date: 2003/03/28 11:10:05 $ by $Author: staffan $
 *
 * @author <a href="http://www.staffannoteberg.com">Staffan Nöteberg</a>
 * @version $Revision: 1.2 $
 * @see se.idega.idegaweb.commune.block.importer.business.NackaStudentTimeImportFileHandler
 * @see com.idega.block.school.data.SchoolTime
 */
public interface NackaStudentTimeImportFileHandler
    extends IBOService, ImportFileHandler {
}
