/*
 * $Id: SchoolDistrictImportFileHandlerHome.java,v 1.1 2005/08/09 16:33:35 laddi Exp $
 * Created on Aug 1, 2005
 *
 * Copyright (C) 2005 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package se.idega.idegaweb.commune.block.importer.business;

import com.idega.business.IBOHome;


/**
 * Last modified: $Date: 2005/08/09 16:33:35 $ by $Author: laddi $
 * 
 * @author <a href="mailto:laddi@idega.com">laddi</a>
 * @version $Revision: 1.1 $
 */
public interface SchoolDistrictImportFileHandlerHome extends IBOHome {

	public SchoolDistrictImportFileHandler create() throws javax.ejb.CreateException, java.rmi.RemoteException;
}
