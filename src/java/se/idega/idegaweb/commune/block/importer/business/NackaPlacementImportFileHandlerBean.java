/*
 * $Id: NackaPlacementImportFileHandlerBean.java,v 1.11 2003/10/23 13:58:25 anders Exp $
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

import se.idega.idegaweb.commune.accounting.resource.business.ResourceBusiness;
import se.idega.idegaweb.commune.accounting.resource.data.Resource;
import se.idega.idegaweb.commune.accounting.resource.data.ResourceClassMember;
import se.idega.idegaweb.commune.business.CommuneUserBusiness;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.business.SchoolBusiness;
import com.idega.block.school.data.School;
import com.idega.block.school.data.SchoolClass;
import com.idega.block.school.data.SchoolClassHome;
import com.idega.block.school.data.SchoolClassMember;
import com.idega.block.school.data.SchoolClassMemberHome;
import com.idega.block.school.data.SchoolHome;
import com.idega.block.school.data.SchoolSeason;
import com.idega.block.school.data.SchoolType;
import com.idega.block.school.data.SchoolTypeHome;
import com.idega.block.school.data.SchoolYear;
import com.idega.block.school.data.SchoolYearHome;
import com.idega.business.IBOServiceBean;
import com.idega.core.localisation.data.ICLanguage;
import com.idega.core.localisation.data.ICLanguageHome;
import com.idega.core.location.data.Commune;
import com.idega.core.location.data.CommuneHome;
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
 * Last modified: $Date: 2003/10/23 13:58:25 $ by $Author: anders $
 *
 * @author Anders Lindman
 * @version $Revision: 1.11 $
 */
public class NackaPlacementImportFileHandlerBean extends IBOServiceBean implements NackaPlacementImportFileHandler, ImportFileHandler {

	private CommuneUserBusiness biz = null;
	private SchoolBusiness schoolBiz = null;
	private ResourceBusiness resourceBiz = null;
  
	private SchoolYearHome sYearHome = null;
	private SchoolTypeHome sTypeHome = null;
	private SchoolHome sHome = null;
	private SchoolClassHome sClassHome = null;
	private SchoolClassMemberHome sClassMemberHome = null;
	private CommuneHome communeHome = null;
	private ICLanguageHome languageHome = null;

	private SchoolSeason season = null;
    
	private ImportFile file;
	private UserTransaction transaction;
  
	private ArrayList userValues;
	private Map failedSchools;
	private ArrayList failedRecords = null;

	private Resource skillLevel0Resource = null;
	private Resource skillLevel1Resource = null;
	private Resource skillLevel2Resource = null;
	private Resource skillLevel3Resource = null;
	
	private Resource motherTongue1Resource = null;
	private Resource motherTongue2Resource = null;

	private final static String REGISTER_DATE = "2003-07-01";
		
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

	private final int RESOURCE_ID_SKILL_LEVEL_0 = 123;
	private final int RESOURCE_ID_SKILL_LEVEL_1 = 124;
	private final int RESOURCE_ID_SKILL_LEVEL_2 = 125;
	private final int RESOURCE_ID_SKILL_LEVEL_3 = 126;
	private final int RESOURCE_ID_NATIVE_LANGUAGE_1 = 121;
	private final int RESOURCE_ID_NATIVE_LANGUAGE_2 = 122;
	
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
        
		Timer clock = new Timer();
		clock.start();

		try {
			//initialize business beans and data homes
			biz = (CommuneUserBusiness) this.getServiceInstance(CommuneUserBusiness.class);
			//home = biz.getUserHome();      
			schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
			resourceBiz = (ResourceBusiness) this.getServiceInstance(ResourceBusiness.class);

			sHome = schoolBiz.getSchoolHome();           
			sYearHome = schoolBiz.getSchoolYearHome();
			sTypeHome = schoolBiz.getSchoolTypeHome();
			sClassHome = (SchoolClassHome) this.getIDOHome(SchoolClass.class);
			sClassMemberHome = (SchoolClassMemberHome) this.getIDOHome(SchoolClassMember.class);
			communeHome = (CommuneHome) this.getIDOHome(Commune.class);
			languageHome = (ICLanguageHome) this.getIDOHome(ICLanguage.class);			

			try {
				season = schoolBiz.getCurrentSchoolSeason();    	
			} catch(FinderException e) {
				e.printStackTrace();
				System.out.println("NackaPlacementHandler: School season is not defined");
				return false;
			}
      
      		// Get resources (change primary keys to the correct values)
      		
			System.out.println("ID for resource language skill level 0 = " + RESOURCE_ID_SKILL_LEVEL_0);
			System.out.println("ID for resource language skill level 1 = " + RESOURCE_ID_SKILL_LEVEL_1);
			System.out.println("ID for resource language skill level 2 = " + RESOURCE_ID_SKILL_LEVEL_2);
			System.out.println("ID for resource language skill level 3 = " + RESOURCE_ID_SKILL_LEVEL_3);
			System.out.println("ID for resource native language 1-5 = " + RESOURCE_ID_NATIVE_LANGUAGE_1);
			System.out.println("ID for resource native language 6-9 = " + RESOURCE_ID_NATIVE_LANGUAGE_2);
			
			skillLevel0Resource = resourceBiz.getResourceByPrimaryKey(new Integer(RESOURCE_ID_SKILL_LEVEL_0));
			if (skillLevel0Resource == null) {
				System.out.println("Resource for language skill level 0 not found.");
				return false;
			}
			skillLevel1Resource = resourceBiz.getResourceByPrimaryKey(new Integer(RESOURCE_ID_SKILL_LEVEL_1));
			if (skillLevel1Resource == null) {
				System.out.println("Resource for language skill level 1 not found.");
				return false;
			}
			skillLevel2Resource = resourceBiz.getResourceByPrimaryKey(new Integer(RESOURCE_ID_SKILL_LEVEL_2));
			if (skillLevel2Resource == null) {
				System.out.println("Resource for language skill level 2 not found.");
				return false;
			}
			skillLevel3Resource = resourceBiz.getResourceByPrimaryKey(new Integer(RESOURCE_ID_SKILL_LEVEL_3));
			if (skillLevel3Resource == null) {
				System.out.println("Resource for language skill level 4 not found.");
				return false;
			}
			motherTongue1Resource = resourceBiz.getResourceByPrimaryKey(new Integer(RESOURCE_ID_NATIVE_LANGUAGE_1));
			if (motherTongue1Resource == null) {
				System.out.println("Resource for mother tongue 1-5 not found.");
				return false;
			}
			motherTongue2Resource = resourceBiz.getResourceByPrimaryKey(new Integer(RESOURCE_ID_NATIVE_LANGUAGE_2));
			if (motherTongue2Resource == null) {
				System.out.println("Resource for mother tongue 6-9 not found.");
				return false;
			}
      		
			//if the transaction failes all the users and their relations are removed
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

				if ((count % 200) == 0 ) {
					System.out.println("NackaStudentHandler processing RECORD [" + count + "] time: " + IWTimestamp.getTimestampRightNow().toString());
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
	protected boolean storeUserInfo() throws RemoteException {

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

		String homeCommuneName = getUserProperty(this.COLUMN_HOME_COMMUNE);
		homeCommuneName = homeCommuneName == null ? "" : homeCommuneName;
		
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
		String schoolYearPrefix = "";
		if (schoolTypeName.equals("Grundskola")) {
			typeKey = "sch_type.school_type_grundskola";
		} else if (schoolTypeName.substring(2).equals("rskoleklass")) {
			typeKey = "sch_type.school_type_forskoleklass";
		} else if (schoolTypeName.substring(2).equals("rskola")) {
			typeKey = "sch_type.school_type_oblig_sarskola";
			schoolYearPrefix = "S";
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
		boolean isNewUser = false;
		boolean updateUser = false;
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
				isNewUser = true;
			} catch (Exception e2) {
				e2.printStackTrace();
				return false;
			}
		}	
		
		if (isNewUser) {
			try {
				Commune homeCommune = communeHome.findByCommuneName(homeCommuneName);
				Integer communeId = (Integer) homeCommune.getPrimaryKey();
				biz.updateCitizenAddress(((Integer) user.getPrimaryKey()).intValue(), studentAddress, studentZipCode, studentZipArea, communeId);
			} catch (FinderException e) {
				System.out.println("Commune not found: " + homeCommuneName);
				return false;
			}
			updateUser = true;
		}

		if (motherTongue.length() > 0) {
			try {
				ICLanguage nativeLanguage = languageHome.findByDescription(motherTongue);
				user.setNativeLanguage(nativeLanguage);
				updateUser = true;		
			} catch (FinderException e) {
				System.out.println("Language with code: " + motherTongue + " not found.");
				return false;
			}
		}
		
		if (updateUser) {
			user.store();
		}
				
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

			Iterator schoolTypeIter = schoolBiz.getSchoolRelatedSchoolTypes(school).values().iterator();
			boolean hasSchoolType = false;
			while (schoolTypeIter.hasNext()) {
				SchoolType st = (SchoolType) schoolTypeIter.next();
				if (st.getPrimaryKey().equals(schoolType.getPrimaryKey())) {
					hasSchoolType = true;
					break;
				}
			}
			if (!hasSchoolType) {
				String s = "School type '" + schoolTypeName + "' not found in school: " + schoolName;
				System.out.println(s);
				failedSchools.put(schoolName, s);
			}
			
			if (schoolYear.equals("0")) {
				schoolYear = "F";
			} else {
				schoolYear = schoolYearPrefix + schoolYear; 
			}
			try {
				//school year	
				year = sYearHome.findByYearName(schoolYear);
			} catch (FinderException e) {
				System.out.println("School year not found: " + schoolYear);
				return false;
			}

			Map schoolYears = schoolBiz.getSchoolRelatedSchoolYears(school);
			Iterator schoolYearIter = schoolYears.values().iterator();
			boolean schoolYearFound = false;
			while (schoolYearIter.hasNext()) {
				SchoolYear sy = (SchoolYear) schoolYearIter.next();
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
				sClass = sClassHome.findBySchoolClassNameSchoolSchoolYearSchoolSeason(schoolClass, school, year, season);
			} catch (FinderException e) {
				//e.printStackTrace();
				System.out.println("School class not found, creating '" + schoolClass + "' for school '" + schoolName + "'.");	
				
				sClass = schoolBiz.storeSchoolClass(schoolClass, school, year, season);
				sClass.setSchoolTypeId(((Integer) schoolType.getPrimaryKey()).intValue());
				if (sClass == null) {
					return false;
				}				
				sClass.store();
			}
			
			//school Class member
			SchoolClassMember member = null;
			boolean createNewPlacement = true;
			try {
				Collection placements = sClassMemberHome.findAllByUserAndSeason(user, season);
				if (placements != null) {
					Iterator oldPlacements = placements.iterator();
					while (oldPlacements.hasNext()) {
						SchoolClassMember placement = (SchoolClassMember) oldPlacements.next();
						SchoolType st = placement.getSchoolClass().getSchoolType();
						if (st != null && ((Integer) st.getPrimaryKey()).equals((Integer) schoolType.getPrimaryKey())) {
							if (placement.getRemovedDate() == null) {
								int oldSchoolId = placement.getSchoolClass().getSchoolId();
								int newSchoolId = ((Integer) school.getPrimaryKey()).intValue();
								if (oldSchoolId != newSchoolId) { 
									IWTimestamp yesterday = new IWTimestamp();
									yesterday.addDays(-1);
									placement.setRemovedDate(yesterday.getTimestamp());
									placement.store();
									Collection c = resourceBiz.getResourcePlacementsByMemberId((Integer) placement.getPrimaryKey());
									Iterator resourceMemberIter = c.iterator();
									while (resourceMemberIter.hasNext()) {
										ResourceClassMember m = (ResourceClassMember) resourceMemberIter.next();
										m.setEndDate(yesterday.getDate());
										m.store();
									}
								} else {
									createNewPlacement = false;
									placement.setSchoolClassId(((Integer)sClass.getPrimaryKey()).intValue());
									placement.store();
									member = placement;
								}
								break;
							}
						}
					}
				}
			} catch (FinderException f) {}

			if (createNewPlacement) {			
				member = schoolBiz.storeSchoolClassMember(sClass, user);
				if (member == null) {
					System.out.println("School class member could not be created for personal id: " + personalId);	
					return false;
				}
				IWTimestamp registerDate = new IWTimestamp(REGISTER_DATE);
				member.setRegisterDate(registerDate.getTimestamp());
				member.setRegistrationCreatedDate(IWTimestamp.getTimestampRightNow());
				member.store();
			}
			
			int memberId = ((Integer) member.getPrimaryKey()).intValue();
			int resourceId = -1;
			
			boolean createMotherTongueResource = useMotherTongue.equals("X");
			Resource motherTongueResource = null;
			if (schoolYear.charAt(0) >= '6') {
				motherTongueResource = motherTongue2Resource;
			} else {
				motherTongueResource = motherTongue1Resource;
			}
			resourceId = ((Integer) motherTongueResource.getPrimaryKey()).intValue();
			Collection rm = resourceBiz.getResourcePlacementsByMemberId((Integer) member.getPrimaryKey());
			Iterator rmIter = rm.iterator();
			while (rmIter.hasNext()) {
				ResourceClassMember m = (ResourceClassMember) rmIter.next();
				int mId = m.getResourceFK();
				if ((mId == RESOURCE_ID_NATIVE_LANGUAGE_1) || (mId == RESOURCE_ID_NATIVE_LANGUAGE_2)) {
					if ((resourceId != mId) || !createMotherTongueResource) {
						IWTimestamp yesterday = new IWTimestamp();
						yesterday.addDays(-1);
						m.setEndDate(yesterday.getDate());
						m.store();
					} else {
						createMotherTongueResource = false;
					}
					break;
				}
			}
			if (createMotherTongueResource) {
				try {
					resourceBiz.createResourcePlacement(resourceId, memberId, REGISTER_DATE);
				} catch (Exception e) {
					System.out.println("Could not create resource placement (" + motherTongue + ") for personal id: " + personalId);
					return false;
				}
			}
			
			boolean createSkillLevelResource = useSkillLevel.equals("X");
			char level = '4';
			try {
				level = skillLevel.charAt(0);
			} catch (Exception e) {}
			Resource skillLevelResource = null;
			switch (level) {
				case '0':
					skillLevelResource = skillLevel0Resource;
					break;			
				case '1':
					skillLevelResource = skillLevel1Resource;
					break;			
				case '2':
					skillLevelResource = skillLevel2Resource;
					break;			
				case '3':
					skillLevelResource = skillLevel3Resource;
					break;
//				default:
//					System.out.println("Could not create resource placement (" + skillLevel + ") for personal id: " + personalId);
//					return false;
			}
			if (skillLevelResource != null) {
				resourceId = ((Integer) skillLevelResource.getPrimaryKey()).intValue();
			}

			rm = resourceBiz.getResourcePlacementsByMemberId((Integer) member.getPrimaryKey());
			rmIter = rm.iterator();
			while (rmIter.hasNext()) {
				ResourceClassMember m = (ResourceClassMember) rmIter.next();
				int mId = m.getResourceFK();
				if ((mId == RESOURCE_ID_SKILL_LEVEL_0) || 
						(mId == RESOURCE_ID_SKILL_LEVEL_1) ||
						(mId == RESOURCE_ID_SKILL_LEVEL_2) ||
						(mId == RESOURCE_ID_SKILL_LEVEL_3)) {
					if ((resourceId != mId) || !createSkillLevelResource) {
						IWTimestamp yesterday = new IWTimestamp();
						yesterday.addDays(-1);
						m.setEndDate(yesterday.getDate());
						m.store();											
					} else {
						createSkillLevelResource = false;
					}
				}
			}
			if (createSkillLevelResource) {
				try {
					resourceBiz.createResourcePlacement(resourceId, memberId, REGISTER_DATE);
				} catch (Exception e) {
					System.out.println("Could not create resource placement (skill level) for personal id: " + personalId);
					return false;
				}				
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