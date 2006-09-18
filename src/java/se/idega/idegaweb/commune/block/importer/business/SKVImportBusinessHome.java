/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.business;


import com.idega.business.IBOHome;

/**
 * @author bluebottle
 *
 */
public interface SKVImportBusinessHome extends IBOHome {
	public SKVImportBusiness create() throws javax.ejb.CreateException,
			java.rmi.RemoteException;

}
