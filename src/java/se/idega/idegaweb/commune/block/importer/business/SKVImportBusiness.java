/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.business;


import com.idega.block.importer.business.ImportBusiness;
import com.idega.user.data.User;

/**
 * @author bluebottle
 *
 */
public interface SKVImportBusiness extends ImportBusiness {
	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SKVImportBusinessBean#handleNames
	 */
	public User handleNames(User user, String firstName, String middleName,
			String lastName, String preferredNameIndex, boolean store)
			throws java.rmi.RemoteException;

}
