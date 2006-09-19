package se.idega.idegaweb.commune.block.importer.data;


import javax.ejb.CreateException;
import javax.ejb.FinderException;
import com.idega.user.data.User;
import com.idega.data.IDOEntity;
import com.idega.data.IDOFactory;

public class SKVUserExtraInfoHomeImpl extends IDOFactory implements SKVUserExtraInfoHome {
	public Class getEntityInterfaceClass() {
		return SKVUserExtraInfo.class;
	}

	public SKVUserExtraInfo create() throws CreateException {
		return (SKVUserExtraInfo) super.createIDO();
	}

	public SKVUserExtraInfo findByPrimaryKey(Object pk) throws FinderException {
		return (SKVUserExtraInfo) super.findByPrimaryKeyIDO(pk);
	}

	public SKVUserExtraInfo findByUser(User user) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Object pk = ((SKVUserExtraInfoBMPBean) entity).ejbFindByUser(user);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKey(pk);
	}

	public SKVUserExtraInfo findByUserID(int userID) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Object pk = ((SKVUserExtraInfoBMPBean) entity).ejbFindByUserID(userID);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKey(pk);
	}
}