/*
 * $Id: NackaHighSchoolPlacementImportFileHandlerBean.java,v 1.21 2005/05/12 12:11:47 laddi Exp $
 *
 * Copyright (C) 2003 Agura IT. All Rights Reserved.
 *
 * This software is the proprietary information of Agura IT AB.
 * Use is subject to license terms.
 *
 */

package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import se.idega.idegaweb.commune.block.importer.data.PlacementImportDate;
import se.idega.idegaweb.commune.block.importer.data.PlacementImportDateHome;
import se.idega.idegaweb.commune.business.CommuneUserBusiness;
import se.idega.util.Report;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.business.SchoolBusiness;
import com.idega.block.school.data.School;
import com.idega.block.school.data.SchoolCategory;
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
 * Last modified: $Date: 2005/05/12 12:11:47 $ by $Author: laddi $
 *
 * @author Anders Lindman
 * @version $Revision: 1.21 $
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
	private PlacementImportDateHome placementImportDateHome = null;

	private SchoolSeason season = null;
    
	private ImportFile file;
	private UserTransaction transaction;
  
	private ArrayList userValues;
	private ArrayList failedRecords = null;
	private Map errorLog  = null;
	
	private Timestamp firstDayInCurrentMonth = null;
	private Timestamp lastDayInPreviousMonth = null;
	
	private Date today = null;

//	private final static Timestamp REGISTER_DATE = (new IWTimestamp("2003-07-01")).getTimestamp(); 
	
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
	// extra column 10 in new import file (Updsign)
	private final static int COLUMN_ADDRESS = 11;
	private final static int COLUMN_CO_ADDRESS = 12;
	private final static int COLUMN_ZIP_CODE = 13;
	private final static int COLUMN_ZIP_AREA = 14;	

	private Gender female = null;
	private Gender male = null;

	private Report report = null;

	/**
	 * Default constructor.
	 */
	public NackaHighSchoolPlacementImportFileHandlerBean() {}

	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#handleRecords() 
	 */
	public boolean handleRecords(){
		failedRecords = new ArrayList();
		errorLog = new TreeMap();
		report = new Report(file.getFile().getName());	// Create a report file. It will be located in the Report dir
		
		IWTimestamp t = IWTimestamp.RightNow();
		t.setAsDate();
		today = t.getDate();
		t.setDay(1);
		firstDayInCurrentMonth = t.getTimestamp();
		t.addDays(-1);
		lastDayInPreviousMonth = t.getTimestamp();
		
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
			placementImportDateHome = (PlacementImportDateHome) this.getIDOHome(PlacementImportDate.class);

			try {
				season = schoolBusiness.getCurrentSchoolSeason(schoolBusiness.getCategoryElementarySchool());    	
			} catch(FinderException e) {
				e.printStackTrace();
				println("NackaHighSchoolPlacementHandler: School season is not defined.");
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
//					break;
				} 

				if ((count % 100) == 0 ) {
					System.out.println("NackaHighSchoolHandler processing RECORD [" + count + "] time: " + IWTimestamp.getTimestampRightNow().toString());
				}
				
				item = null;
			}

			if (!failed) {
				if (!terminateOldPlacements()) {
					failed = true;
				}
			}
			
			printFailedRecords();

			clock.stop();
			println("Number of records handled: " + (count - 1));
			println("Time to handle records: " + clock.getTime() + " ms  OR " + ((int)(clock.getTime()/1000)) + " s");

			//success commit changes
			if (!failed) {
				transaction.commit();
			} else {
				transaction.rollback(); 
			}

			report.store(false);
			
			return !failed;
			
		} catch (Exception e) {
			e.printStackTrace();
			try {
				transaction.rollback();
			} catch (SystemException e2) {
				e2.printStackTrace();
			}

			report.store(false);
			
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
		println("\n----------------------------------------------\n");
		if (failedRecords.isEmpty()) {
			println("All records imported successfully.");
		} else {
			println("Import failed for these records, please fix and import again:\n");
		}
  
		Iterator iter = failedRecords.iterator();

		while (iter.hasNext()) {
			println((String) iter.next());
		}

		if (!errorLog.isEmpty()) {
			println("Errors during import:\n");
		}
		Iterator rowIter = errorLog.keySet().iterator();
		
		while (rowIter.hasNext()) {
			Integer row = (Integer) rowIter.next(); 
			String message = (String) errorLog.get(row);
			println("Line " + row + ": " + message);
		}	
		
		println("");
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
			println("User not found for PIN : " + personalId + " CREATING");
			
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
		boolean hasSchoolType = false;
		try {
		Iterator schoolTypeIter = schoolBusiness.getSchoolRelatedSchoolTypes(school).values().iterator();
			while (schoolTypeIter.hasNext()) {
				SchoolType st = (SchoolType) schoolTypeIter.next();
				if (st.getPrimaryKey().equals(schoolType.getPrimaryKey())) {
					hasSchoolType = true;
					break;
				}
			}
		} catch (Exception e) {}
		
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
		try {
			schoolYearFoundInSchool = m.containsKey(schoolYear.getPrimaryKey());
		} catch (Exception e) {}
		
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
			println("School Class not found, creating '" + schoolClassName + "' for high school '" + providerName + "'.");	
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
		Timestamp registerDate = firstDayInCurrentMonth;
		
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
							int studyPathId = placement.getStudyPathId();
							int newStudyPathId = ((Integer) studyPath.getPrimaryKey()).intValue();
							int schoolYearId = placement.getSchoolYearId();
							int newSchoolYearId = ((Integer) schoolYear.getPrimaryKey()).intValue();
							if ((scId == schoolClassId) && (studyPathId == newStudyPathId)
									&& (schoolYearId == newSchoolYearId)) {
								member = placement;
							} else {
								IWTimestamp t1 = new IWTimestamp(placement.getRegisterDate());
								t1.setAsDate();
								IWTimestamp t2 = new IWTimestamp(firstDayInCurrentMonth);
								t2.setAsDate();
								if (t1.equals(t2)) {
									try {
										PlacementImportDate p = null;
										try {
											p = placementImportDateHome.findByPrimaryKey(placement.getPrimaryKey());
										} catch (FinderException e) {}
										if (p != null) {
											p.remove();
										}
										placement.remove();
									} catch (RemoveException e) {
										log(e);
									}
								} else {								
									placement.setRemovedDate(lastDayInPreviousMonth);
									placement.store();
								}
								registerDate = firstDayInCurrentMonth;
							}
						}
					}
				}
			}
		} catch (FinderException f) {}

		if (member == null) {		
			try {
				member = schoolClassMemberHome.create();
			} catch (CreateException e) {
				errorLog.put(new Integer(row), "School Class member could not be created for personal id: " + personalId);	
				return false;				
			}
			member.setSchoolClassId(((Integer) schoolClass.getPrimaryKey()).intValue());
			member.setClassMemberId(((Integer) user.getPrimaryKey()).intValue());
			member.setRegisterDate(registerDate);
			member.setRegistrationCreatedDate(IWTimestamp.getTimestampRightNow());
			member.setSchoolYear(((Integer) schoolYear.getPrimaryKey()).intValue()); 
			member.setSchoolTypeId(((Integer) schoolType.getPrimaryKey()).intValue());
			member.setStudyPathId(((Integer) studyPath.getPrimaryKey()).intValue());
			member.store();
		}
		
		PlacementImportDate p = null;
		try {
			p = placementImportDateHome.findByPrimaryKey(member.getPrimaryKey());
		} catch (FinderException e) {}
		if (p == null) {
			try {
				p = placementImportDateHome.create();
				p.setSchoolClassMemberId(((Integer) member.getPrimaryKey()).intValue());
			} catch (CreateException e) {
				errorLog.put(new Integer(row), "Could not create import date from school class member: " + member.getPrimaryKey());	
				return false;								
			}
		}
		p.setImportDate(today);
		p.store();
		
		return true;
	}

	/**
	 * Terminates all all high school placements not included in the import (except Nacka Gymnasium placements). 
	 */
	protected boolean terminateOldPlacements() {
		println("NackaHighSchoolPlacementHandler: Starting termination of old placements...");
		boolean success = true;
		School schoolA = null;
		School schoolB = null;
		School schoolC = null;
		try {
			schoolA = schoolHome.findBySchoolName(NackaProgmaPlacementImportFileHandlerBean.SCHOOL_NAME_A);
		} catch (FinderException e) {
			println("NackaHighSchoolPlacementHandler: School '" + NackaProgmaPlacementImportFileHandlerBean.SCHOOL_NAME_A + "' not found.");
			return false;
		}
		try {
			schoolB = schoolHome.findBySchoolName(NackaProgmaPlacementImportFileHandlerBean.SCHOOL_NAME_B);
		} catch (FinderException e) {
			println("NackaHighSchoolPlacementHandler: School '" + NackaProgmaPlacementImportFileHandlerBean.SCHOOL_NAME_B + "' not found.");
			return false;
		}
		try {
			schoolC = schoolHome.findBySchoolName(NackaProgmaPlacementImportFileHandlerBean.SCHOOL_NAME_C);
		} catch (FinderException e) {
			println("NackaHighSchoolPlacementHandler: School '" + NackaProgmaPlacementImportFileHandlerBean.SCHOOL_NAME_C + "' not found.");
			return false;
		}
		int schoolIdA = ((Integer) schoolA.getPrimaryKey()).intValue();
		int schoolIdB = ((Integer) schoolB.getPrimaryKey()).intValue();
		int schoolIdC = ((Integer) schoolC.getPrimaryKey()).intValue();
		String[] schoolIds = new String[] {String.valueOf(schoolIdA), String.valueOf(schoolIdB), String.valueOf(schoolIdC)};

		
		SchoolCategory highSchoolCategory = null;
		try {
			highSchoolCategory = schoolBusiness.getCategoryHighSchool();
		} catch (RemoteException e) {
			println("NackaHighSchoolPlacementHandler: High school category not found.");
			return false;			
		}
		
		Collection placements = null;
		try {
			placements = schoolClassMemberHome.findActiveByCategorySeasonAndSchools(highSchoolCategory, season, schoolIds, true); 
		} catch (FinderException e) {
			println("NackaHighSchoolPlacementHandler: Error finding placements.");
			return false;			
		}
		
		Iterator iter = placements.iterator();
		IWTimestamp now = IWTimestamp.RightNow();
		now.setAsDate();
		while (iter.hasNext()) {
			SchoolClassMember member = (SchoolClassMember) iter.next();
			IWTimestamp placementDate = null;
			try {
				PlacementImportDate p = placementImportDateHome.findByPrimaryKey(member.getPrimaryKey());
				placementDate = new IWTimestamp(p.getImportDate());
				placementDate.setAsDate();
			} catch (FinderException e) {}
			if (placementDate == null || placementDate.isEarlierThan(now)) {
				member.setRemovedDate(lastDayInPreviousMonth);
				member.store();				
				println("Terminating placement with id: " + member.getPrimaryKey());
			}
		}

		println("NackaHighSchoolPlacementHandler: Termination of old placements finished.");
		return success;
	}
	
	/*
	 * Prints the specified string to standard output and to import log.
	 */
	private void println(String s) {
		System.out.println(s);
		report.append(s);
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
