/*
 * $Id: NackaPlacementImportFileHandlerBean.java,v 1.1 2003/10/15 15:30:52 anders Exp $
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import se.idega.idegaweb.commune.business.CommuneUserBusiness;

import com.idega.block.importer.data.ImportFile;
import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.school.business.SchoolBusiness;
import com.idega.block.school.data.School;
import com.idega.block.school.data.SchoolClass;
import com.idega.block.school.data.SchoolClassHome;
import com.idega.block.school.data.SchoolClassMember;
import com.idega.block.school.data.SchoolClassMemberHome;
import com.idega.block.school.data.SchoolHome;
import com.idega.block.school.data.SchoolSeason;
import com.idega.block.school.data.SchoolSeasonHome;
import com.idega.block.school.data.SchoolYear;
import com.idega.block.school.data.SchoolYearHome;
import com.idega.business.IBOServiceBean;
import com.idega.data.IDOAddRelationshipException;
import com.idega.user.data.Gender;
import com.idega.user.data.GenderHome;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;

/** 
 * Import logic for placing Nacka students.
 * <p>
 * Last modified: $Date: 2003/10/15 15:30:52 $ by $Author: anders $
 *
 * @author Anders Lindman
 * @version $Revision: 1.1 $
 */
public class NackaPlacementImportFileHandlerBean extends IBOServiceBean implements NackaPlacementImportFileHandler, ImportFileHandler {

	private CommuneUserBusiness biz;
	private SchoolBusiness schoolBiz;
  
	private SchoolYearHome sYearHome;
	private SchoolHome sHome;
	private SchoolClassHome sClassHome;
	private SchoolClassMemberHome sClassMemberHome;

	private SchoolSeason season = null;
    
	private ImportFile file;
	private UserTransaction transaction;
  
	private ArrayList userValues;
	private Map failedSchools;
	private ArrayList failedRecords = new ArrayList();

	private final int COLUMN_SCHOOL_NAME = 0;  
	private final int COLUMN_PERSONAL_ID = 1;
	private final int COLUMN_SCHOOL_YEAR = 2; 
	private final int COLUMN_CLASS = 3; 
	private Gender female;
	private Gender male;
  
  	/**
  	 * Default constructor.
  	 */
	public NackaPlacementImportFileHandlerBean() {}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#handleRecords() 
	 */
	public boolean handleRecords() throws RemoteException{
		failedSchools = new HashMap();
		transaction = this.getSessionContext().getUserTransaction();
    
		try {
			season = ((SchoolSeasonHome)this.getIDOHome(SchoolSeason.class)).findByPrimaryKey(new Integer(2));    	
			//((SchoolChoiceBusiness)this.getServiceInstance(SchoolChoiceBusiness.Class)).getCurrentSeason();
    	} catch(FinderException ex) {
			ex.printStackTrace();
			System.err.println("NackaStudentHandler:School season is not defined");
			return false;
		}
    
		Timer clock = new Timer();
		clock.start();

		try {
			//initialize business beans and data homes
			biz = (CommuneUserBusiness) this.getServiceInstance(CommuneUserBusiness.class);
			//home = biz.getUserHome();
      
			schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);

			sHome = schoolBiz.getSchoolHome();           
			sYearHome = schoolBiz.getSchoolYearHome();
			sClassHome = (SchoolClassHome)this.getIDOHome(SchoolClass.class);
			sClassMemberHome = (SchoolClassMemberHome)this.getIDOHome(SchoolClassMember.class);
      
			//if the transaction failes all the users and their relations are removed
			transaction.begin();

			//iterate through the records and process them
			String item;

			int count = 0;
			while (!(item = (String) file.getNextRecord()).equals("")) {
				count++;
				
				if(!processRecord(item)) {
					failedRecords.add(item);
				} 

				if ((count % 500) == 0 ) {
					System.out.println("NackaStudentHandler processing RECORD [" + count + "] time: " + IWTimestamp.getTimestampRightNow().toString());
				}
				
				item = null;
			}
      
			printFailedRecords();

			clock.stop();
			System.out.println("Time to handleRecords: " + clock.getTime() + " ms  OR " + ((int)(clock.getTime()/1000)) + " s");

			//success commit changes
			transaction.commit();
			
			return true;
			
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
	 * Processes a single record 
	 */
	private boolean processRecord(String record) throws RemoteException{
		userValues = file.getValuesFromRecordString(record);
		//System.out.println("THE RECORD = "+record);
		
		boolean success = storeUserInfo();
		userValues = null;
		
		return success;
	}
  
	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#printFailedRecords() 
	 */
	public void printFailedRecords() {
		System.out.println("Import failed for these records, please fix and import again:");
  
		Iterator iter = failedRecords.iterator();

		while (iter.hasNext()) {
			System.out.println((String) iter.next());
		}
		
		System.out.println("Schools missing from database or have different names:");
		Collection cols = failedSchools.values();
		Iterator schools = cols.iterator();
		
		while (schools.hasNext()) {
			String name = (String) schools.next();
			System.out.println(name);
		}	
	}

	/**
	 * Stores the placement.
	 */
	protected boolean storeUserInfo() throws RemoteException{

		User user = null;

		//variables
    
		String PIN = getUserProperty(this.COLUMN_PERSONAL_ID);  
		if(PIN == null ) return false;
        
		String schoolName = getUserProperty(this.COLUMN_SCHOOL_NAME);
		if(schoolName == null ) return false;

		String schoolYear = getUserProperty(this.COLUMN_SCHOOL_YEAR);
		if(schoolYear == null ) return false;

		String schoolClass = getUserProperty(this.COLUMN_CLASS);
		if(schoolClass == null ) return false;

		//database stuff
		School school;
		SchoolYear year; 
	
		// user
		try {
			user = biz.getUserHome().findByPersonalID(PIN);
			//debug
			if( user == null ) System.out.println(" USER IS NULL!!??? should cast finderexception");
		} catch (FinderException e) {
			System.out.println("User not found for PIN : "+PIN+" CREATING");
			//create special citizen user by pin
			//find annan kommune1 258
			// get gender
			// get dat of birfth
			try {
				user = biz.createSpecialCitizenByPersonalIDIfDoesNotExist(PIN,"","",PIN,getGenderFromPin(PIN),getBirthDateFromPin(PIN));
				//schoolName = "I annan kommun 1";
			} catch (Exception e2) {
				e2.printStackTrace();
				return false;
			}
		}	
	
		if( !"secret".equals(user.getDescription())) {
			try {
				//school
				//this can only work if there is only one school with this name. add more parameters for other areas
				school = sHome.findBySchoolName(schoolName);
			} catch (FinderException e) {
			//System.out.println("School not found : "+schoolName);
			schoolName = "I annan kommun 1";
			try {
				school = sHome.findBySchoolName(schoolName);
			} catch (FinderException ex) {
				failedSchools.put(schoolName,schoolName);
				return false;
			}
		}		
		
		try {
			//school year	
			if( schoolYear.equals("0") ) schoolYear = "F";
			year = sYearHome.findByYearName(schoolYear);
		} catch (FinderException e) {
			System.out.println("SchoolYear not found : "+schoolYear);
			//TODO Create the SchoolYear
				
			return false;
		}	
			
		//add year to school
		try{
			school.addSchoolYear(year);
		} catch (IDOAddRelationshipException aEx){
			//aEx.printStackTrace();
		}
		
		//school Class		
		SchoolClass sClass = null;
		
		try {	
			sClass = sClassHome.findBySchoolClassNameSchoolSchoolYearSchoolSeason(schoolClass,school,year,season);
		} catch (FinderException e) {
			//e.printStackTrace();
			System.out.println("School Class not found creating...");	
			
			sClass = schoolBiz.storeSchoolClass(schoolClass,school,year,season);
			sClass.store();
			if (sClass == null) {
				return false;
			}				
		}
		
		//school Class member
		SchoolClassMember member = null;
		try {	
			Collection classMembers =  sClassMemberHome.findAllByUserAndSeason(user,season);
				
			Iterator oldClasses = classMembers.iterator();
			while (oldClasses.hasNext()) {
				SchoolClassMember temp = (SchoolClassMember) oldClasses.next();
				try {
					temp.remove();
				} catch (RemoveException e) {
					e.printStackTrace();
					return false;
				}				
			}
		} catch (FinderException f){}
		
		//System.out.println("School Class member not found creating...");	
		member = schoolBiz.storeSchoolClassMember(sClass, user);
		member.store();
		if (member == null) return false;
		
		//schoolClassmember finished
		} else {//remove secret market person from all schools this season
			System.out.println("NackaStudentImportHandler Removing protected citizen from all Classes (pin:"+user.getPersonalID()+")");
			try{		
				Collection classMembers =  sClassMemberHome.findAllByUserAndSeason(user,season);
				
				Iterator oldClasses = classMembers.iterator();
				while (oldClasses.hasNext()) {
					SchoolClassMember temp = (SchoolClassMember) oldClasses.next();
					try {
						temp.remove();
					}
					catch (RemoveException e) {
						
						e.printStackTrace();
						System.out.println("NackaStudentImportHandler FAILED Removing protected citizen from all Classes (pin:"+user.getPersonalID()+")");
			
						return false;
					}
					
					
				}
			}
			catch(FinderException f){
				
			}
		}

    //finished with this user
    user = null;
    return true;
  }

  public void setImportFile(ImportFile file){
    this.file = file;
  }

	private String getUserProperty(int columnIndex){
		String value = null;
		
		if( userValues!=null ){
		
			try {
				value = (String)userValues.get(columnIndex);
			} catch (RuntimeException e) {
				return null;
			}
	 			//System.out.println("Index: "+columnIndex+" Value: "+value);
	 		if( file.getEmptyValueString().equals( value ) ) return null;
		 	else return value;
  		}
  		else return null;
  }

	private IWTimestamp getBirthDateFromPin(String pin){
		//pin format = 190010221208 yyyymmddxxxx
		int dd = Integer.parseInt(pin.substring(6,8));
		int mm = Integer.parseInt(pin.substring(4,6));
		int yyyy = Integer.parseInt(pin.substring(0,4));
		IWTimestamp dob = new IWTimestamp(dd,mm,yyyy);
		return dob;
	}
	
	
	private Gender getGenderFromPin(String pin){
			//pin format = 190010221208 second last number is the gender
			//even number = female
			//odd number = male
			try {
				GenderHome home = (GenderHome) this.getIDOHome(Gender.class);
				if( Integer.parseInt(pin.substring(10,11)) % 2 == 0 ){
					if( female == null ){
						female = home.getFemaleGender();
					}
					return female;
				}
				else{
					if( male == null ){
						male = home.getMaleGender();
					}
					return male;
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
				return null;//if something happened
			}
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