/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.business;


import com.idega.business.IBOHomeImpl;

/**
 * @author bluebottle
 *
 */
public class SKVImportFileHandlerHomeImpl extends IBOHomeImpl implements
		SKVImportFileHandlerHome {
	protected Class getBeanInterfaceClass() {
		return SKVImportFileHandler.class;
	}

	public SKVImportFileHandler create() throws javax.ejb.CreateException {
		return (SKVImportFileHandler) super.createIBO();
	}

}
