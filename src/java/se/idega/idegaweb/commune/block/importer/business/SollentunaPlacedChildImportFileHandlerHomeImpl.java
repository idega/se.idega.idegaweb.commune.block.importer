/*
 * $Id: SollentunaPlacedChildImportFileHandlerHomeImpl.java,v 1.2 2004/12/05 09:04:57 laddi Exp $
 * Created on 3.12.2004
 *
 * Copyright (C) 2004 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package se.idega.idegaweb.commune.block.importer.business;

import com.idega.business.IBOHomeImpl;


/**
 * Last modified: $Date: 2004/12/05 09:04:57 $ by $Author: laddi $
 * 
 * @author <a href="mailto:laddi@idega.com">laddi</a>
 * @version $Revision: 1.2 $
 */
public class SollentunaPlacedChildImportFileHandlerHomeImpl extends IBOHomeImpl implements SollentunaPlacedChildImportFileHandlerHome {

	protected Class getBeanInterfaceClass() {
		return SollentunaPlacedChildImportFileHandler.class;
	}

	public SollentunaPlacedChildImportFileHandler create() throws javax.ejb.CreateException {
		return (SollentunaPlacedChildImportFileHandler) super.createIBO();
	}

}
