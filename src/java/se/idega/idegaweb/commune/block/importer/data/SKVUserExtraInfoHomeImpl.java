/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.data;


import javax.ejb.FinderException;

import com.idega.data.IDOFactory;
import com.idega.user.data.User;

/**
 * @author bluebottle
 *
 */
public class SKVUserExtraInfoHomeImpl extends IDOFactory implements
		SKVUserExtraInfoHome {
	protected Class getEntityInterfaceClass() {
		return SKVUserExtraInfo.class;
	}

	public SKVUserExtraInfo create() throws javax.ejb.CreateException {
		return (SKVUserExtraInfo) super.createIDO();
	}

	public SKVUserExtraInfo findByPrimaryKey(Object pk)
			throws javax.ejb.FinderException {
		return (SKVUserExtraInfo) super.findByPrimaryKeyIDO(pk);
	}

	public SKVUserExtraInfo findByUser(User user) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		Object pk = ((SKVUserExtraInfoBMPBean) entity).ejbFindByUser(user);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKey(pk);
	}

	public SKVUserExtraInfo findByUserID(int userID) throws FinderException {
		com.idega.data.IDOEntity entity = this.idoCheckOutPooledEntity();
		Object pk = ((SKVUserExtraInfoBMPBean) entity).ejbFindByUserID(userID);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKey(pk);
	}

}
