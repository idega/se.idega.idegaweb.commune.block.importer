/*
 * $Id: SollentunaPlacedChildImportFileHandler.java,v 1.2 2004/12/05 09:04:57 laddi Exp $
 * Created on 3.12.2004
 *
 * Copyright (C) 2004 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package se.idega.idegaweb.commune.block.importer.business;

import java.util.List;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.business.IBOService;
import com.idega.user.data.Group;


/**
 * Last modified: $Date: 2004/12/05 09:04:57 $ by $Author: laddi $
 * 
 * @author <a href="mailto:laddi@idega.com">laddi</a>
 * @version $Revision: 1.2 $
 */
public interface SollentunaPlacedChildImportFileHandler extends IBOService, ImportFileHandler {

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SollentunaPlacedChildImportFileHandlerBean#handleRecords
	 */
	public boolean handleRecords() throws java.rmi.RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SollentunaPlacedChildImportFileHandlerBean#printFailedRecords
	 */
	public void printFailedRecords() throws java.rmi.RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SollentunaPlacedChildImportFileHandlerBean#setImportFile
	 */
	public void setImportFile(ImportFile file) throws java.rmi.RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SollentunaPlacedChildImportFileHandlerBean#setRootGroup
	 */
	public void setRootGroup(Group rootGroup) throws java.rmi.RemoteException;

	/**
	 * @see se.idega.idegaweb.commune.block.importer.business.SollentunaPlacedChildImportFileHandlerBean#getFailedRecords
	 */
	public List getFailedRecords() throws java.rmi.RemoteException;

}
