/*
 * $Id: NackaParagraphImportFileHandlerBean.java,v 1.4 2004/01/12 09:05:03 laddi Exp $
 *
 * Copyright (C) 2003 Agura IT. All Rights Reserved.
 *
 * This software is the proprietary information of Agura IT AB.
 * Use is subject to license terms.
 *
 */

package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import se.idega.idegaweb.commune.business.CommuneUserBusiness;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.data.SchoolClassMember;
import com.idega.block.school.data.SchoolClassMemberHome;
import com.idega.block.school.data.SchoolType;
import com.idega.business.IBOServiceBean;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;

/** 
 * Import logic for adding paragraphs to Nacka student placements.
 * <br>
 * To add this to the "Import handler" dropdown for the import function, execute the following SQL:<br>
 * insert into im_handler values (9, 'Nacka student paragraph importer', 
 * 'se.idega.idegaweb.commune.block.importer.business.NackaParagraphImportFileHandlerBean',
 * 'Imports paragraphs for Nacka student placements.')
 * <br>
 * Note that the "9" value in the SQL might have to be adjusted in the sql, 
 * depending on the number of records already inserted in the table. </p>
 * <p>
 * Last modified: $Date: 2004/01/12 09:05:03 $ by $Author: laddi $
 *
 * @author Anders Lindman
 * @version $Revision: 1.4 $
 */
public class NackaParagraphImportFileHandlerBean extends IBOServiceBean implements NackaParagraphImportFileHandler, ImportFileHandler {

	private CommuneUserBusiness biz = null;
	private SchoolClassMemberHome sClassMemberHome = null;
    
	private ImportFile file;
	private UserTransaction transaction;
  
	private ArrayList userValues;
	private ArrayList failedRecords = null;
		
	private final int COLUMN_PERSONAL_ID = 0;  
	private final int COLUMN_PARAGRAPH = 3;  
	
  	/**
  	 * Default constructor.
  	 */
	public NackaParagraphImportFileHandlerBean() {}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#handleRecords() 
	 */
	public boolean handleRecords(){
		failedRecords = new ArrayList();
		transaction = this.getSessionContext().getUserTransaction();
        
		Timer clock = new Timer();
		clock.start();

		try {
			//initialize business beans and data homes
			biz = (CommuneUserBusiness) this.getServiceInstance(CommuneUserBusiness.class);
			sClassMemberHome = (SchoolClassMemberHome) this.getIDOHome(SchoolClassMember.class);
            		
			transaction.begin();

			//iterate through the records and process them
			String item;
			int count = 0;
			boolean failed = false;

			while (!(item = (String) file.getNextRecord()).trim().equals("")) {
				count++;
				
				if(!processRecord(item, count)) {
					failedRecords.add(item);
					failed = true;
					break;
				} 

				if ((count % 50) == 0 ) {
					System.out.println("NackaParagraphHandler processing RECORD [" + count + "] time: " + IWTimestamp.getTimestampRightNow().toString());
				}
				
				item = null;
			}
      
			printFailedRecords();

			clock.stop();
			System.out.println("Number of records handled: " + (count - 1));
			System.out.println("Time to handleRecords: " + clock.getTime() + " ms  OR " + ((int)(clock.getTime()/1000)) + " s");

			//success commit changes
			if (!failed) {
				transaction.commit();
			} else {
				transaction.rollback(); 
			}
			
			return !failed;
			
		} catch (Exception e) {
			e.printStackTrace();
			try {
				transaction.rollback();
			} catch (SystemException e2) {
				e2.printStackTrace();
			}

			return false;
		}
	}

	/*
	 * Processes one record 
	 */
	private boolean processRecord(String record, int count) throws RemoteException {
		if (count == 1) {
			// Skip header
			return true;
		}
		userValues = file.getValuesFromRecordString(record);
		boolean success = storeUserInfo();
		userValues = null;
				
		return success;
	}
  
	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#printFailedRecords() 
	 */
	public void printFailedRecords() {
		if (failedRecords.isEmpty()) {
			System.out.println("All records imported successfully.");
		} else {
			System.out.println("Import failed for these records, please fix and import again:");
		}
  
		Iterator iter = failedRecords.iterator();

		while (iter.hasNext()) {
			System.out.println((String) iter.next());
		}
	}

	/**
	 * Stores one placement.
	 */
	protected boolean storeUserInfo() throws RemoteException {

		User user = null;

		String personalId = getUserProperty(this.COLUMN_PERSONAL_ID);
		if (personalId == null) return false;

		String paragraph = getUserProperty(this.COLUMN_PARAGRAPH);
		paragraph = paragraph == null ? "" : paragraph;
			
		// user
		try {
			user = biz.getUserHome().findByPersonalID(personalId);
		} catch (FinderException e) {
			System.out.println("User not found for PIN : " + personalId);
			return false;
		}	
				
		try {
			Collection placements = sClassMemberHome.findByStudent(user);
			if (placements != null) {
				Iterator placementsIter = placements.iterator();
				while (placementsIter.hasNext()) {
					SchoolClassMember placement = (SchoolClassMember) placementsIter.next();
					SchoolType schoolType = placement.getSchoolClass().getSchoolType();					
					String stKey = "";
					
					if (schoolType != null) {
						stKey = schoolType.getLocalizationKey();
					}
					
					if (stKey.equals("sch_type.school_type_grundskola") ||
							stKey.equals("sch_type.school_type_forskoleklass") ||
							stKey.equals("sch_type.school_type_oblig_sarskola")) {
						if (placement.getRemovedDate() == null) {
							placement.setPlacementParagraph(paragraph);
							placement.store();
						}
					}
				}
			}
		} catch (FinderException f) {}

		user = null;
		return true;
	}

	/*
	 * Returns the property for the specified column from the current record. 
	 */
	private String getUserProperty(int columnIndex){
		String value = null;
		
		if (userValues!=null) {
		
			try {
				value = (String) userValues.get(columnIndex);
			} catch (RuntimeException e) {
				return null;
			}
	 		if (file.getEmptyValueString().equals(value)) {
	 			return null;
	 		} else {
	 			return value;
	 		} 
		} else {
			return null;
  		} 
	}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#getFailedRecords()
	 */
	public void setImportFile(ImportFile file){
		this.file = file;
	}
		
	/**
	 * Not used
	 * @param rootGroup The rootGroup to set
	 */
	public void setRootGroup(Group rootGroup) {
	}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#getFailedRecords()
	 */
	public List getFailedRecords(){
		return failedRecords;	
	}
}
