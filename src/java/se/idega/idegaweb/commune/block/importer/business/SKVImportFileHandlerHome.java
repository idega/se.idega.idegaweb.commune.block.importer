/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.business;


import com.idega.business.IBOHome;

/**
 * @author bluebottle
 *
 */
public interface SKVImportFileHandlerHome extends IBOHome {
	public SKVImportFileHandler create() throws javax.ejb.CreateException,
			java.rmi.RemoteException;

}
