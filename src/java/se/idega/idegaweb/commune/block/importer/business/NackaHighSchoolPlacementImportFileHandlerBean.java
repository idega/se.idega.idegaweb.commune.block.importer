/*
 * $Id: NackaHighSchoolPlacementImportFileHandlerBean.java,v 1.3 2003/11/11 16:07:58 anders Exp $
 *
 * Copyright (C) 2003 Agura IT. All Rights Reserved.
 *
 * This software is the proprietary information of Agura IT AB.
 * Use is subject to license terms.
 *
 */

package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

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
import com.idega.block.school.data.SchoolStudyPath;
import com.idega.block.school.data.SchoolStudyPathHome;
import com.idega.block.school.data.SchoolType;
import com.idega.block.school.data.SchoolTypeHome;
import com.idega.block.school.data.SchoolYear;
import com.idega.block.school.data.SchoolYearHome;
import com.idega.business.IBOServiceBean;
import com.idega.core.location.data.Commune;
import com.idega.core.location.data.CommuneHome;
import com.idega.user.data.Gender;
import com.idega.user.data.GenderHome;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;

/** 
 * Import logic for placing Nacka high school students.
 * <br>
 * To add this to the "Import handler" dropdown for the import function, execute the following SQL:<br>
 * insert into im_handler values (11, 'Nacka high school placement importer', 
 * 'se.idega.idegaweb.commune.block.importer.business.NackaHighSchoolPlacementImportFileHandlerBean',
 * 'Imports high school placements for students in Nacka.')
 * <br>
 * Note that the "11" value in the SQL might have to be adjusted in the sql, 
 * depending on the number of records already inserted in the table. </p>
 * <p>
 * Last modified: $Date: 2003/11/11 16:07:58 $ by $Author: anders $
 *
 * @author Anders Lindman
 * @version $Revision: 1.3 $
 */
public class NackaHighSchoolPlacementImportFileHandlerBean extends IBOServiceBean implements NackaHighSchoolPlacementImportFileHandler, ImportFileHandler {

	private CommuneUserBusiness communeUserBusiness = null;
	private SchoolBusiness schoolBusiness = null;
  
	private SchoolHome schoolHome = null;
	private SchoolTypeHome schoolTypeHome = null;
	private SchoolYearHome schoolYearHome = null;
	private SchoolClassHome schoolClassHome = null;
	private SchoolClassMemberHome schoolClassMemberHome = null;
	private CommuneHome communeHome = null;
	private SchoolStudyPathHome studyPathHome = null;

	private SchoolSeason season = null;
    
	private ImportFile file;
	private UserTransaction transaction;
  
	private ArrayList userValues;
	private ArrayList failedRecords = null;
	private Map errorLog  = null;

	private final static Timestamp REGISTER_DATE = (new IWTimestamp("2003-07-01")).getTimestamp(); 
	
	private final static String LOC_KEY_HIGH_SCHOOL = "sch_type.school_type_gymnasieskola";
	private final static String LOC_KEY_SPECIAL_HIGH_SCHOOL = "sch_type.school_type_gymnasiesarskola";
	
	private final static int COLUMN_PROVIDER_NAME = 0;
	private final static int COLUMN_HIGH_SCHOOL_TYPE = 1;
//	private final static int COLUMN_PERIOD = 2;
	private final static int COLUMN_SCHOOL_CLASS = 3;
	private final static int COLUMN_SCHOOL_YEAR = 4;
	private final static int COLUMN_STUDY_PATH = 5;
	private final static int COLUMN_PERSONAL_ID = 6;
	private final static int COLUMN_STUDENT_NAME = 7;
	private final static int COLUMN_HOME_COMMUNE = 8;
//	private final static int COLUMN_UPDATED_DATE = 9;
	private final static int COLUMN_ADDRESS = 10;
	private final static int COLUMN_CO_ADDRESS = 11;
	private final static int COLUMN_ZIP_CODE = 12;
	private final static int COLUMN_ZIP_AREA = 13;	

	private Gender female = null;
	private Gender male = null;
	
	/**
	 * Default constructor.
	 */
	public NackaHighSchoolPlacementImportFileHandlerBean() {}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#handleRecords() 
	 */
	public boolean handleRecords() throws RemoteException{
		failedRecords = new ArrayList();
		errorLog = new TreeMap();
		
		transaction = this.getSessionContext().getUserTransaction();
        
		Timer clock = new Timer();
		clock.start();

		try {
			//initialize business beans and data homes
			communeUserBusiness = (CommuneUserBusiness) this.getServiceInstance(CommuneUserBusiness.class);
			schoolBusiness = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
			
			schoolHome = schoolBusiness.getSchoolHome();           
			schoolTypeHome = schoolBusiness.getSchoolTypeHome();
			schoolYearHome = schoolBusiness.getSchoolYearHome();
			schoolClassHome = (SchoolClassHome) this.getIDOHome(SchoolClass.class);
			schoolClassMemberHome = (SchoolClassMemberHome) this.getIDOHome(SchoolClassMember.class);
			communeHome = (CommuneHome) this.getIDOHome(Commune.class);
			studyPathHome = (SchoolStudyPathHome) this.getIDOHome(SchoolStudyPath.class);

			try {
				season = schoolBusiness.getCurrentSchoolSeason();    	
			} catch(FinderException e) {
				e.printStackTrace();
				System.out.println("NackaHighSchoolPlacementHandler: School season is not defined.");
				return false;
			}
            		
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

				if ((count % 100) == 0 ) {
					System.out.println("NackaHighSchoolHandler processing RECORD [" + count + "] time: " + IWTimestamp.getTimestampRightNow().toString());
				}
				
				item = null;
			}
      
			printFailedRecords();

			clock.stop();
			System.out.println("Number of records handled: " + (count - 1));
			System.out.println("Time to handle records: " + clock.getTime() + " ms  OR " + ((int)(clock.getTime()/1000)) + " s");

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
		boolean success = storeUserInfo(count);
		userValues = null;
				
		return success;
	}
  
	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#printFailedRecords() 
	 */
	public void printFailedRecords() {
		System.out.println("\n----------------------------------------------\n");
		if (failedRecords.isEmpty()) {
			System.out.println("All records imported successfully.");
		} else {
			System.out.println("Import failed for these records, please fix and import again:\n");
		}
  
		Iterator iter = failedRecords.iterator();

		while (iter.hasNext()) {
			System.out.println((String) iter.next());
		}

		if (!errorLog.isEmpty()) {
			System.out.println("Errors during import:\n");
		}
		Iterator rowIter = errorLog.keySet().iterator();
		
		while (rowIter.hasNext()) {
			Integer row = (Integer) rowIter.next(); 
			String message = (String) errorLog.get(row);
			System.out.println("Line " + row + ": " + message);
		}	
		
		System.out.println();
	}

	/**
	 * Stores one placement.
	 */
	protected boolean storeUserInfo(int row) throws RemoteException {

		User user = null;
		SchoolType schoolType = null;
		School school = null;

		String providerName = getUserProperty(COLUMN_PROVIDER_NAME);
		if (providerName == null) {
			errorLog.put(new Integer(row), "The name of the high school is empty.");
			return false;
		}

		String schoolClassName = getUserProperty(COLUMN_SCHOOL_CLASS);
		if (schoolClassName == null) {
			errorLog.put(new Integer(row), "The class name is empty.");
			return false;
		}

		String schoolYearName = getUserProperty(COLUMN_SCHOOL_YEAR);
		if (schoolYearName == null) {
			errorLog.put(new Integer(row), "The school year is empty.");
		}

		String studyPathCode = getUserProperty(COLUMN_STUDY_PATH);
		if (studyPathCode == null) {
			studyPathCode = "";
		}
		
		String personalId = getUserProperty(COLUMN_PERSONAL_ID);
		if (personalId == null) {
			errorLog.put(new Integer(row), "The personal id is empty.");
			return false;
		}
		
		String studentName = getUserProperty(COLUMN_STUDENT_NAME);
		if (studentName == null) {
			studentName = "";
		}
		String studentFirstName = "";
		String studentLastName = "";		
		if (studentName.length() > 0) {
			int cutPos = studentName.indexOf(',');
			if (cutPos != -1) {
				studentFirstName = studentName.substring(cutPos + 1).trim();
				studentLastName = studentName.substring(0, cutPos).trim(); 
			}
		}
		
		String homeCommuneCode = getUserProperty(COLUMN_HOME_COMMUNE);
		if (homeCommuneCode == null) {
			homeCommuneCode = "";
		}

		String address = getUserProperty(COLUMN_ADDRESS);
		if (address == null) {
			address = "";
		}
		
		String coAddress = getUserProperty(COLUMN_CO_ADDRESS);
		if (coAddress == null) {
			coAddress = "";
		}
		
		String zipCode = getUserProperty(COLUMN_ZIP_CODE);
		if (zipCode == null) {
			zipCode = "";
		}
		
		String zipArea = getUserProperty(COLUMN_ZIP_AREA);
		if (zipArea == null) {
			zipArea = "";
		}
		
		String highSchoolType = getUserProperty(COLUMN_HIGH_SCHOOL_TYPE);
		if (highSchoolType == null) {
			errorLog.put(new Integer(row), "The high school type is empty.");
			return false;
		}
				
		// user		
		boolean isNewUser = false;
		try {
			user = communeUserBusiness.getUserHome().findByPersonalID(personalId);
		} catch (FinderException e) {
			System.out.println("User not found for PIN : " + personalId + " CREATING");
			
			try {
				user = communeUserBusiness.createSpecialCitizenByPersonalIDIfDoesNotExist(
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
				Commune homeCommune = communeHome.findByCommuneCode(homeCommuneCode);
				Integer communeId = (Integer) homeCommune.getPrimaryKey();
				communeUserBusiness.updateCitizenAddress(((Integer) user.getPrimaryKey()).intValue(), address, zipCode, zipArea, communeId);
			} catch (FinderException e) {
				errorLog.put(new Integer(row), "Commune not found: " + homeCommuneCode);
				return false;
			}
			user.store();
		}
				
		// school type
		String typeKey = null;
		String schoolYearPrefix = "G";
		if (highSchoolType.equals("GY")) {
			typeKey = LOC_KEY_HIGH_SCHOOL;
		} else {
			typeKey = LOC_KEY_SPECIAL_HIGH_SCHOOL;
			schoolYearPrefix += "S";
		}
		
		try {
			schoolType = schoolTypeHome.findByTypeKey(typeKey);
		} catch (FinderException e) {
			errorLog.put(new Integer(row), "School type: " + highSchoolType + " not found in database (key = " + typeKey + ").");
			return false;
		}
				
		// school
		try {
			school = schoolHome.findBySchoolName(providerName);
		} catch (FinderException e) {
			errorLog.put(new Integer(row), "Cannot find school with name '" + providerName + "'");
			return false;
		}
		
		// school type
		Iterator schoolTypeIter = schoolBusiness.getSchoolRelatedSchoolTypes(school).values().iterator();
		boolean hasSchoolType = false;
		while (schoolTypeIter.hasNext()) {
			SchoolType st = (SchoolType) schoolTypeIter.next();
			if (st.getPrimaryKey().equals(schoolType.getPrimaryKey())) {
				hasSchoolType = true;
				break;
			}
		}
		if (!hasSchoolType) {
			errorLog.put(new Integer(row), "School type '" + highSchoolType + "' not found in high school: " + providerName);
			return false;
		}

		// school year
		SchoolYear schoolYear = null;
		schoolYearName = schoolYearPrefix + schoolYearName;
		try {
			schoolYear = schoolYearHome.findByYearName(schoolYearName);
		} catch (FinderException e) {
			errorLog.put(new Integer(row), "School year: " + schoolYearName + " not found in database.");
		}
		boolean schoolYearFoundInSchool = false;
		Map m = schoolBusiness.getSchoolRelatedSchoolYears(school);
		schoolYearFoundInSchool = m.containsKey(schoolYear.getPrimaryKey());
		if (!schoolYearFoundInSchool) {
			errorLog.put(new Integer(row), "School year: '" + schoolYearName + "' not found in school: '" + providerName  + "'.");
			return false;
		}

		// study path
		SchoolStudyPath studyPath = null;
		try {
			studyPath = studyPathHome.findByCode(studyPathCode);
		} catch (Exception e) {
			errorLog.put(new Integer(row), "Cannot find study path: " + studyPathCode);
			return false;
		}
		
		// school Class		
		SchoolClass schoolClass = null;
		try {	
			int schoolId = ((Integer) school.getPrimaryKey()).intValue();
			int seasonId = ((Integer) season.getPrimaryKey()).intValue();
			Collection c = schoolClassHome.findBySchoolAndSeason(schoolId, seasonId);
			Iterator iter = c.iterator();
			while (iter.hasNext()) {
				SchoolClass sc = (SchoolClass) iter.next();
				if (sc.getName().equals(schoolClassName)) {
					schoolClass = sc;
					break;
				}
			}
			if (schoolClass == null) {
				throw new FinderException();
			}				
		} catch (Exception e) {
			System.out.println("School Class not found, creating '" + schoolClassName + "' for high school '" + providerName + "'.");	
			int schoolId = ((Integer) school.getPrimaryKey()).intValue();
			int schoolTypeId = ((Integer) schoolType.getPrimaryKey()).intValue();
			int seasonId = ((Integer) season.getPrimaryKey()).intValue();
			try {
				schoolClass = schoolClassHome.create();
				schoolClass.setSchoolClassName(schoolClassName);
				schoolClass.setSchoolId(schoolId);
				schoolClass.setSchoolTypeId(schoolTypeId);
				schoolClass.setSchoolSeasonId(seasonId);
				schoolClass.setValid(true);
				schoolClass.store();
				schoolClass.addSchoolYear(schoolYear);
			} catch (Exception e2) {}

			if (schoolClass == null) {
				errorLog.put(new Integer(row), "Could not create school Class: " + schoolClassName);
				return false;
			}				
		}
		
		// school Class member
		int schoolClassId = ((Integer) schoolClass.getPrimaryKey()).intValue();
		SchoolClassMember member = null;
		try {
			Collection placements = schoolClassMemberHome.findByStudent(user);
			if (placements != null) {
				Iterator placementsIter = placements.iterator();
				while (placementsIter.hasNext()) {
					SchoolClassMember placement = (SchoolClassMember) placementsIter.next();
					SchoolType st = placement.getSchoolClass().getSchoolType();					
					String stKey = "";
					
					if (st != null) {
						stKey = st.getLocalizationKey();
					}
					
					if (stKey.equals(LOC_KEY_HIGH_SCHOOL) ||
							stKey.equals(LOC_KEY_SPECIAL_HIGH_SCHOOL)) {
						if (placement.getRemovedDate() == null) {
							int scId = placement.getSchoolClassId();
							if (scId == schoolClassId) {
								member = placement;
							} else {
								IWTimestamp yesterday = new IWTimestamp();
								yesterday.addDays(-1);
								placement.setRemovedDate(yesterday.getTimestamp());
								placement.store();
							}
							placement.store();
						}
					}
				}
			}
		} catch (FinderException f) {}

		if (member == null) {			
			member = schoolBusiness.storeSchoolClassMember(schoolClass, user);
			if (member == null) {
				errorLog.put(new Integer(row), "School Class member could not be created for personal id: " + personalId);	
				return false;
			}
		}
		
		member.setRegisterDate(REGISTER_DATE);
		member.setRegistrationCreatedDate(IWTimestamp.getTimestampRightNow());
		member.setSchoolYear(((Integer) schoolYear.getPrimaryKey()).intValue()); 
		member.setSchoolTypeId(((Integer) schoolType.getPrimaryKey()).intValue());
		member.setStudyPathId(((Integer) studyPath.getPrimaryKey()).intValue());
		member.store();

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
}
