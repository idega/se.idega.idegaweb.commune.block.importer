/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.business;


import com.idega.business.IBOHomeImpl;

/**
 * @author bluebottle
 *
 */
public class SKVImportBusinessHomeImpl extends IBOHomeImpl implements
		SKVImportBusinessHome {
	protected Class getBeanInterfaceClass() {
		return SKVImportBusiness.class;
	}

	public SKVImportBusiness create() throws javax.ejb.CreateException {
		return (SKVImportBusiness) super.createIBO();
	}

}
