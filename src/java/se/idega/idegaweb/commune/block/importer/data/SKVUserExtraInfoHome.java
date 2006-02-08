/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.data;


import javax.ejb.FinderException;

import com.idega.data.IDOHome;
import com.idega.user.data.User;

/**
 * @author bluebottle
 *
 */
public interface SKVUserExtraInfoHome extends IDOHome {
	public SKVUserExtraInfo create() throws javax.ejb.CreateException;

	public SKVUserExtraInfo findByPrimaryKey(Object pk)
			throws javax.ejb.FinderException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#ejbFindByUser
	 */
	public SKVUserExtraInfo findByUser(User user) throws FinderException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#ejbFindByUserID
	 */
	public SKVUserExtraInfo findByUserID(int userID) throws FinderException;

}
