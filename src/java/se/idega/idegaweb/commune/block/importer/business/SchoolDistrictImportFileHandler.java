/*
 * $Id: SchoolDistrictImportFileHandler.java,v 1.1 2005/08/09 16:33:35 laddi Exp $
 * Created on Aug 1, 2005
 *
 * Copyright (C) 2005 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.List;
import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.business.IBOService;
import com.idega.user.data.Group;


/**
 * Last modified: $Date: 2005/08/09 16:33:35 $ by $Author: laddi $
 * 
 * @author <a href="mailto:laddi@idega.com">laddi</a>
 * @version $Revision: 1.1 $
 */
public interface SchoolDistrictImportFileHandler extends IBOService, ImportFileHandler {

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SchoolDistrictImportFileHandlerBean#handleRecords
	 */
	public boolean handleRecords() throws RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SchoolDistrictImportFileHandlerBean#setImportFile
	 */
	public void setImportFile(ImportFile file) throws RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SchoolDistrictImportFileHandlerBean#setRootGroup
	 */
	public void setRootGroup(Group rootGroup) throws RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SchoolDistrictImportFileHandlerBean#getFailedRecords
	 */
	public List getFailedRecords() throws RemoteException;
}
