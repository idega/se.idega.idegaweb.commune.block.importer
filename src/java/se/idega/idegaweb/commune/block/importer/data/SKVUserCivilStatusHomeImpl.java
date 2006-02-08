/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.data;


import com.idega.data.IDOFactory;

/**
 * @author bluebottle
 *
 */
public class SKVUserCivilStatusHomeImpl extends IDOFactory implements
		SKVUserCivilStatusHome {
	protected Class getEntityInterfaceClass() {
		return SKVUserCivilStatus.class;
	}

	public SKVUserCivilStatus create() throws javax.ejb.CreateException {
		return (SKVUserCivilStatus) super.createIDO();
	}

	public SKVUserCivilStatus findByPrimaryKey(Object pk)
			throws javax.ejb.FinderException {
		return (SKVUserCivilStatus) super.findByPrimaryKeyIDO(pk);
	}

}
