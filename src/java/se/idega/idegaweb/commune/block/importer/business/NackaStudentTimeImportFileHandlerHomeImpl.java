package se.idega.idegaweb.commune.block.importer.business;

import com.idega.business.IBOHomeImpl;
import javax.ejb.CreateException;

/**
 * Last modified: $Date: 2003/03/27 11:20:54 $ by $Author: staffan $
 *
 * @author <a href="http://www.staffannoteberg.com">Staffan Nöteberg</a>
 * @version $Revision: 1.1 $
 */
public class NackaStudentTimeImportFileHandlerHomeImpl extends IBOHomeImpl
    implements NackaStudentTimeImportFileHandlerHome {
    
    protected Class getBeanInterfaceClass(){
        return NackaStudentTimeImportFileHandler.class;
    }
    
    public NackaStudentTimeImportFileHandler create() throws CreateException {
        return (NackaStudentTimeImportFileHandler) createIBO ();
    }
}
