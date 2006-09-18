/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.List;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.business.IBOService;
import com.idega.user.data.Group;

/**
 * @author bluebottle
 *
 */
public interface SKVImportFileHandler extends IBOService, ImportFileHandler {
	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SKVImportFileHandlerBean#handleRecords
	 */
	public boolean handleRecords() throws java.rmi.RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SKVImportFileHandlerBean#setImportFile
	 */
	public void setImportFile(ImportFile file) throws java.rmi.RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SKVImportFileHandlerBean#getFailedRecords
	 */
	public List getFailedRecords() throws RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SKVImportFileHandlerBean#setRootGroup
	 */
	public void setRootGroup(Group rootGroup) throws RemoteException;

}
