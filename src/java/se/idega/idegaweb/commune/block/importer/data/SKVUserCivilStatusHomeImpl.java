package se.idega.idegaweb.commune.block.importer.data;


import javax.ejb.CreateException;
import javax.ejb.FinderException;
import com.idega.data.IDOEntity;
import com.idega.data.IDOFactory;

public class SKVUserCivilStatusHomeImpl extends IDOFactory implements SKVUserCivilStatusHome {
	public Class getEntityInterfaceClass() {
		return SKVUserCivilStatus.class;
	}

	public SKVUserCivilStatus create() throws CreateException {
		return (SKVUserCivilStatus) super.createIDO();
	}

	public SKVUserCivilStatus findByPrimaryKey(Object pk) throws FinderException {
		return (SKVUserCivilStatus) super.findByPrimaryKeyIDO(pk);
	}

	public SKVUserCivilStatus findByStatusCode(String code) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Object pk = ((SKVUserCivilStatusBMPBean) entity).ejbFindByStatusCode(code);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKey(pk);
	}
}