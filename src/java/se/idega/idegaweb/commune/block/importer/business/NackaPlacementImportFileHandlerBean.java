/*
 * $Id: NackaPlacementImportFileHandlerBean.java,v 1.2 2003/10/17 15:27:10 anders Exp $
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
import com.idega.block.school.data.SchoolType;
import com.idega.block.school.data.SchoolTypeHome;
import com.idega.business.IBOServiceBean;
import com.idega.user.data.Gender;
import com.idega.user.data.GenderHome;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;

/** 
 * Import logic for placing Nacka students.
 * <br>
 * To add this to the "Import handler" dropdown for the import function, execute the following SQL:<br>
 * insert into im_handler values (5, 'Nacka student placement importer with resources', 
 * 'se.idega.idegaweb.commune.block.importer.business.NackaPlacementImportFileHandlerBean',
 * 'Imports Nacka students with resources and students in other communes.')
 * <br>
 * Note that the "5" value in the SQL might have to be adjusted in the sql, 
 * depending on the number of records already inserted in the table. </p>
 * <p>
 * Last modified: $Date: 2003/10/17 15:27:10 $ by $Author: anders $
 *
 * @author Anders Lindman
 * @version $Revision: 1.2 $
 */
public class NackaPlacementImportFileHandlerBean extends IBOServiceBean implements NackaPlacementImportFileHandler, ImportFileHandler {

	private CommuneUserBusiness biz;
	private SchoolBusiness schoolBiz;
  
	private SchoolYearHome sYearHome;
	private SchoolTypeHome sTypeHome;
	private SchoolHome sHome;
	private SchoolClassHome sClassHome;
	private SchoolClassMemberHome sClassMemberHome;

	private SchoolSeason season = null;
    
	private ImportFile file;
	private UserTransaction transaction;
  
	private ArrayList userValues;
	private Map failedSchools;
	private ArrayList failedRecords = null;

//	private final int COLUMN_PERIOD = 0;  
	private final int COLUMN_SCHOOL_TYPE = 1;  
	private final int COLUMN_SCHOOL_NAME = 2;  
	private final int COLUMN_PERSONAL_ID = 3;  
	private final int COLUMN_STUDENT_NAME = 4;  
	private final int COLUMN_STUDENT_ADDRESS = 5;  
	private final int COLUMN_STUDENT_ZIP_CODE = 6;  
	private final int COLUMN_STUDENT_ZIP_AREA = 7;  
	private final int COLUMN_HOME_COMMUNE = 8;  
	private final int COLUMN_SCHOOL_YEAR = 9;  
	private final int COLUMN_SCHOOL_CLASS = 10;  
	private final int COLUMN_MOTHER_TONGUE = 11;  
	private final int COLUMN_USE_MOTHER_TONGUE = 12;  
	private final int COLUMN_SKILL_LEVEL = 13;  
	private final int COLUMN_USE_SKILL_LEVEL = 14;  

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
		failedRecords = new ArrayList();
		failedSchools = new HashMap();
		transaction = this.getSessionContext().getUserTransaction();
    
		try {
			season = ((SchoolSeasonHome)this.getIDOHome(SchoolSeason.class)).findByPrimaryKey(new Integer(2));    	
			//((SchoolChoiceBusiness)this.getServiceInstance(SchoolChoiceBusiness.Class)).getCurrentSeason();
    	} catch(FinderException e) {
			e.printStackTrace();
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
			sTypeHome = schoolBiz.getSchoolTypeHome();
			sClassHome = (SchoolClassHome)this.getIDOHome(SchoolClass.class);
			sClassMemberHome = (SchoolClassMemberHome)this.getIDOHome(SchoolClassMember.class);
      
			//if the transaction failes all the users and their relations are removed
			transaction.begin();

			//iterate through the records and process them
			String item;

			int count = 0;
			while (!(item = (String) file.getNextRecord()).equals("")) {
				count++;
				
				if(!processRecord(item, count)) {
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
	 * Processes one record 
	 */
	private boolean processRecord(String record, int count) throws RemoteException{
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
			if (failedSchools.isEmpty()) {
				System.out.println("All records imported successfully.");
			}
		} else {
			System.out.println("Import failed for these records, please fix and import again:");
		}
  
		Iterator iter = failedRecords.iterator();

		while (iter.hasNext()) {
			System.out.println((String) iter.next());
		}

		if (!failedSchools.isEmpty()) {
			System.out.println("Schools missing from database or have different names:");
		}
		Collection cols = failedSchools.values();
		Iterator schools = cols.iterator();
		
		while (schools.hasNext()) {
			String name = (String) schools.next();
			System.out.println(name);
		}	
	}

	/**
	 * Stores one placement.
	 */
	protected boolean storeUserInfo() throws RemoteException{

		User user = null;

//		String period = getUserProperty(this.COLUMN_PERIOD);  

		String schoolTypeName = getUserProperty(this.COLUMN_SCHOOL_TYPE);
		if (schoolTypeName == null ) return false;

		String schoolName = getUserProperty(this.COLUMN_SCHOOL_NAME);
		if (schoolName == null ) return false;

		String personalId = getUserProperty(this.COLUMN_PERSONAL_ID);
		if (personalId == null) return false;

		String studentName = getUserProperty(this.COLUMN_STUDENT_NAME);
		studentName = studentName == null ? "" : studentName;
		
		String studentFirstName = "";
		String studentLastName = "";		
		if (studentName.length() > 0) {
			int cutPos = studentName.indexOf(',');
			if (cutPos != -1) {
				studentFirstName = studentName.substring(cutPos + 1).trim();
				studentLastName = studentName.substring(0, cutPos).trim(); 
			}
		}

		String studentAddress = getUserProperty(this.COLUMN_STUDENT_ADDRESS);
		studentAddress = studentAddress == null ? "" : studentAddress;

		String studentZipCode = getUserProperty(this.COLUMN_STUDENT_ZIP_CODE);
		studentZipCode = studentZipCode == null ? "" : studentZipCode;

		String studentZipArea = getUserProperty(this.COLUMN_STUDENT_ZIP_AREA);
		studentZipArea = studentZipArea == null ? "" : studentZipArea;

		String homeCommune = getUserProperty(this.COLUMN_HOME_COMMUNE);
		homeCommune = homeCommune == null ? "" : homeCommune;
		
		String schoolYear = getUserProperty(this.COLUMN_SCHOOL_YEAR);
		if (schoolYear == null) return false;
		
		String schoolClass = getUserProperty(this.COLUMN_SCHOOL_CLASS);
		if (schoolClass == null) return false;

		String motherTongue = getUserProperty(this.COLUMN_MOTHER_TONGUE);
		motherTongue = motherTongue == null ? "" : motherTongue;

		String useMotherTongue = getUserProperty(this.COLUMN_USE_MOTHER_TONGUE);
		useMotherTongue = useMotherTongue == null ? "" : useMotherTongue;

		String skillLevel = getUserProperty(this.COLUMN_SKILL_LEVEL);
		skillLevel = skillLevel == null ? "" : skillLevel;

		String useSkillLevel = getUserProperty(this.COLUMN_USE_SKILL_LEVEL);
		useSkillLevel = useSkillLevel == null ? "" : useSkillLevel;

		School school = null;
		SchoolYear year = null;
		SchoolType schoolType = null; 

		// school type
		String typeKey = null;
		if (schoolTypeName.equals("Grundskola")) {
			typeKey = "sch_type.school_type_grundskola";
		} else if (schoolTypeName.substring(2).equals("rskoleklass")) {
			typeKey = "sch_type.school_type_forskoleklass";
		} else if (schoolTypeName.substring(2).equals("rskola")) {
			typeKey = "sch_type.school_type_oblig_sarskola";
		}
		if (typeKey == null) {
			System.out.println("School type: " + schoolTypeName + " not supported.");
			return false;
		}
		
		try {
			schoolType = sTypeHome.findByTypeKey(typeKey);
		} catch (FinderException e) {
			System.out.println("School type: " + schoolTypeName + " not found in database (key = " + typeKey + ").");
			return false;
		}
			
		// user
		try {
			user = biz.getUserHome().findByPersonalID(personalId);
		} catch (FinderException e) {
			System.out.println("User not found for PIN : " + personalId + " CREATING");
			
			try {
				user = biz.createSpecialCitizenByPersonalIDIfDoesNotExist(
						studentFirstName, 
						"",
						studentLastName,
						personalId,
						getGenderFromPin(personalId),
						getBirthDateFromPin(personalId));
			} catch (Exception e2) {
				e2.printStackTrace();
				return false;
			}
		}	
		
		if (studentFirstName.length() > 0) {
			user.setFirstName(studentFirstName);
		}
				
		if (studentLastName.length() > 0) {
			user.setLastName(studentLastName);
		}
				
		if (studentAddress.length() > 0) {
			biz.updateCitizenAddress(((Integer) user.getPrimaryKey()).intValue(), studentAddress, studentZipCode, studentZipArea);
		}
		
		user.store();
				
		String description = user.getDescription();
		if (description == null) {
			description = "";	
		}
		if (!"secret".equals(description)) {
			try {
				//this can only work if there is only one school with this name. add more parameters for other areas
				school = sHome.findBySchoolName(schoolName);
			} catch (FinderException e) {
				failedSchools.put(schoolName,schoolName);
				return false;
			}		
			
			if (schoolYear.equals("0")) {
				schoolYear = "F";
			} 
			try {
				//school year	
				year = sYearHome.findByYearName(schoolYear);
			} catch (FinderException e) {
				System.out.println("SchoolYear not found : " + schoolYear);
				return false;
			}

			Map schoolYears = schoolBiz.getSchoolRelatedSchoolYears(school);
			Iterator iter = schoolYears.values().iterator();
			boolean schoolYearFound = false;
			while (iter.hasNext()) {
				SchoolYear sy = (SchoolYear) iter.next();
				if (sy.getSchoolYearName().equals(schoolYear)) {
					schoolYearFound = true;
					break;
				}
			}
			if (!schoolYearFound) {
				String s = "School year '" + schoolYear + "' not found in school: " + schoolName;
				System.out.println(s);
				failedSchools.put(schoolName, s);
			}
										
			//school Class		
			SchoolClass sClass = null;
			
			try {	
				sClass = sClassHome.findBySchoolClassNameSchoolSchoolYearSchoolSeason(schoolClass,school,year,season);
			} catch (FinderException e) {
				//e.printStackTrace();
				System.out.println("School class not found, creating '" + schoolClass + "' for school '" + schoolName + "'.");	
				
				sClass = schoolBiz.storeSchoolClass(schoolClass, school, year, season);
				sClass.store();
				if (sClass == null) {
					return false;
				}				
			}
			
			//school Class member
			SchoolClassMember member = null;
			try {	
				Collection placements =  sClassMemberHome.findAllByUserAndSeason(user, season);
					
				Iterator oldPlacements = placements.iterator();
				while (oldPlacements.hasNext()) {
					SchoolClassMember placement = (SchoolClassMember) oldPlacements.next();
					SchoolType st = placement.getSchoolClass().getSchoolType();
					if (st.getPrimaryKey().equals(schoolType.getPrimaryKey())) {
						if (placement.getRemovedDate() == null) {
							IWTimestamp yesterday = new IWTimestamp();
							yesterday.addDays(-1);
							placement.setRemovedDate(yesterday.getTimestamp());
						}
					}
				}
			} catch (FinderException f) {}
			
			//System.out.println("School Class member not found creating...");	
			member = schoolBiz.storeSchoolClassMember(sClass, user);
			member.store();
			if (member == null) {
				return false;
			} 
			
		} else {//remove secret market person from all schools this season
			System.out.println("NackaStudentImportHandler Removing protected citizen from all classes (pin:" + user.getPersonalID() + ")");
			try{		
				Collection classMembers =  sClassMemberHome.findAllByUserAndSeason(user, season);
				
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
			} catch(FinderException f) {}
		}

		//finished with this user
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
	 		//System.out.println("Index: "+columnIndex+" Value: "+value);
	 		if (file.getEmptyValueString().equals(value)) {
	 			return null;
	 		} else {
	 			return value;
	 		} 
		} else {
			return null;
  		} 
	}

	private IWTimestamp getBirthDateFromPin(String pin){
		//pin format = 190010221208 yyyymmddxxxx
		int dd = Integer.parseInt(pin.substring(6,8));
		int mm = Integer.parseInt(pin.substring(4,6));
		int yyyy = Integer.parseInt(pin.substring(0,4));
		IWTimestamp dob = new IWTimestamp(dd,mm,yyyy);
		return dob;
	}
	
	private Gender getGenderFromPin(String pin) {
		//pin format = 190010221208 second last number is the gender
		//even number = female
		//odd number = male
		try {
			GenderHome home = (GenderHome) this.getIDOHome(Gender.class);
			if (Integer.parseInt(pin.substring(10, 11)) % 2 == 0) {
				if (female == null) {
					female = home.getFemaleGender();
				}
				return female;
			} else {
				if (male == null) {
					male = home.getMaleGender();
				}
				return male;
			}
		} catch (Exception e) {
			e.printStackTrace();
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