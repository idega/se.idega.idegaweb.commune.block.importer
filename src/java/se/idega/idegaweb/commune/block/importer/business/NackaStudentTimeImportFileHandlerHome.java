package se.idega.idegaweb.commune.block.importer.business;

import com.idega.business.IBOHome;
import java.rmi.RemoteException;
import javax.ejb.CreateException;

/**
 * Last modified: $Date: 2003/03/27 11:20:54 $ by $Author: staffan $
 *
 * @author <a href="http://www.staffannoteberg.com">Staffan Nöteberg</a>
 * @version $Revision: 1.1 $
 */
public interface NackaStudentTimeImportFileHandlerHome extends IBOHome {
    public NackaStudentTimeImportFileHandler create() throws CreateException,
                                                             RemoteException;
}
