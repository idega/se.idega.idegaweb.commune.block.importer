/*
 * $Id: SollentunaPlacedChildImportFileHandler.java,v 1.1 2004/12/03 08:59:51 laddi Exp $
 * Created on 3.12.2004
 *
 * Copyright (C) 2004 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import se.idega.idegaweb.commune.business.CommuneUserBusiness;
import se.idega.idegaweb.commune.care.business.AlreadyCreatedException;
import se.idega.idegaweb.commune.childcare.business.ChildCareBusiness;
import se.idega.util.Report;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.business.SchoolBusiness;
import com.idega.block.school.data.School;
import com.idega.block.school.data.SchoolClass;
import com.idega.block.school.data.SchoolClassHome;
import com.idega.block.school.data.SchoolClassMember;
import com.idega.block.school.data.SchoolClassMemberHome;
import com.idega.block.school.data.SchoolHome;
import com.idega.business.IBOService;
import com.idega.business.IBOServiceBean;
import com.idega.idegaweb.UnavailableIWContext;
import com.idega.presentation.IWContext;
import com.idega.user.data.Gender;
import com.idega.user.data.GenderHome;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.DateFormatException;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;


/**
 * Last modified: $Date: 2004/12/03 08:59:51 $ by $Author: laddi $
 * 
 * @author <a href="mailto:laddi@idega.com">laddi</a>
 * @version $Revision: 1.1 $
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
