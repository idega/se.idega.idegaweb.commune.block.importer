package se.idega.idegaweb.commune.block.importer.business;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

//import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import se.idega.idegaweb.commune.business.CommuneUserBusiness;
import se.idega.idegaweb.commune.care.business.AlreadyCreatedException;
import se.idega.idegaweb.commune.childcare.business.ChildCareBusiness;
import se.idega.util.PIDChecker;
import se.idega.util.Report;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.business.SchoolBusiness;
import com.idega.block.school.data.School;
import com.idega.block.school.data.SchoolClass;
import com.idega.block.school.data.SchoolClassHome;
import com.idega.block.school.data.SchoolClassMember;
import com.idega.block.school.data.SchoolClassMemberHome;
//import com.idega.block.school.data.SchoolClassMemberLog;
//import com.idega.block.school.data.SchoolClassMemberLogHome;
import com.idega.block.school.data.SchoolHome;
import com.idega.block.school.data.SchoolType;
import com.idega.business.IBOServiceBean;
import com.idega.data.IDORelationshipException;
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
 * <p>Title: NackaPlacedChildImportFileHandlerBean</p>
 * <p>Description: Imports the child care placements into the database.  
 * To add this to the "Import handler" dropdown for the import function, execute the following SQL:<br>
 * insert into im_handler values(20, 'Sollentuna new child care placement importer', 'se.idega.idegaweb.commune.block.importer.business.SollentunaChildCareImportFileHandlerBean', 'Imports child placements in Sollentuna.')
 * <br>
 * Note that the "20" value in the SQL might have to be adjusted in the sql, 
 * depending on the number of records already inserted in the table. </p>
 * <p>Copyright (c) 2003</p>
 * <p>Company: Idega Software</p>
 * @author Joakim Johnson</a>
 * @version 1.0
 */
public class SollentunaChildCareImportFileHandlerBean extends IBOServiceBean 
implements ImportFileHandler
, SollentunaChildCareImportFileHandler{
	private CommuneUserBusiness biz;
	//private UserHome home;
	private SchoolBusiness schoolBiz;
	private SchoolHome sHome;
	private SchoolClassHome sClassHome;
	private SchoolClassMemberHome sClassMemberHome;
//	private SchoolClassMemberLogHome sClassMemberLogHome;
	private ImportFile file;
	private UserTransaction transaction;
	private ArrayList userValues;
	private List failedSchools;
	private List failedRecords;
	private List notFoundChildren;
	private List notFoundParent;
	private Collection childcareTypes;
	private User performer;
	private Locale locale;

	/*
	private static final int COLUMN_CHILD_PERSONAL_ID = 0;
	private static final int COLUMN_CHILD_NAME = 1;
	private static final int COLUMN_UNIT = 2;
	private static final int COLUMN_GROUP_NAME = 3;
	private static final int COLUMN_HOURS = 4;
	private static final int COLUMN_PLACEMENT_FROM = 5;
	private static final int COLUMN_PLACEMENT_TO = 6;
	private static final int COLUMN_START_DATE = 7;
	*/
	private static final int COLUMN_CHILD_PERSONAL_ID = 0;
	private static final int COLUMN_CHILD_NAME = 1;
	private static final int COLUMN_UNIT_ID = 2;
	private static final int COLUMN_UNIT = 3;
	private static final int COLUMN_GROUP_ID = 4;
	private static final int COLUMN_GROUP_NAME = 5;
	private static final int COLUMN_HOURS = 6;
	private static final int COLUMN_PLACEMENT_FROM = 7;
	private static final int COLUMN_PLACEMENT_TO = 8;
	private static final int COLUMN_START_DATE = 9;
	private static final int COLUMN_END_DATE = 10;
	
//	private Gender female;
//	private Gender male;
	private Report report;
	private int successCount, failCount, alreadyChoosenCount, count;
	String item;
	int schoolTypeID = -1;
	
	public SollentunaChildCareImportFileHandlerBean() {
	}

	public boolean handleRecords() {
		failedSchools = new ArrayList();
		failedRecords = new ArrayList();
		notFoundChildren = new ArrayList();
		notFoundParent = new ArrayList();
		transaction = this.getSessionContext().getUserTransaction();
		report = new Report(file.getFile().getName());	//Create a report file. I will be located in the Report dir
		//Zero all counters used just for reporting purposes
		//IWBundle bundle = getIWMainApplication().getBundle("se.idega.idegaweb.commune");
		//schoolTypeID =  Integer.parseInt(bundle.getProperty("child_care_school_type", "2"));
		count = 0;
		failCount = 0;
		successCount = 0;
		alreadyChoosenCount = 0;

		Timer clock = new Timer();
		clock.start();
		try {
			//initialize business beans and data homes
			biz = (CommuneUserBusiness) this.getServiceInstance(CommuneUserBusiness.class);
			//home = biz.getUserHome();
			schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
			sHome = schoolBiz.getSchoolHome();
			sClassHome = (SchoolClassHome) this.getIDOHome(SchoolClass.class);
			sClassMemberHome = (SchoolClassMemberHome) this.getIDOHome(SchoolClassMember.class);
//			sClassMemberLogHome = (SchoolClassMemberLogHome) this.getIDOHome(SchoolClassMemberLog.class);
			//if the transaction failes all the users and their relations are removed
			transaction.begin();
			//iterate through the records and process them
			file.getNextRecord();	//Skip header
			while (!(item = (String) file.getNextRecord()).equals("")) {
				if (!processRecord(item))
					failedRecords.add(item);
				if ((count % 50) == 0) {
					System.out.println(
						"NackaPlacedChildHandler processing RECORD [" + count + "] time: "
							+ IWTimestamp.getTimestampRightNow().toString());
				}
				item = null;
			}
			clock.stop();
			
			printFailedRecords();
			report.append("\nNackaQueueHandler processed "+ successCount
					+ " records successfuly out of "+ count+ "records.\n");
			report.append(alreadyChoosenCount+" of the selections had already been imported.\n");
			report.append("Time to handleRecords: " + clock.getTime() + " ms  OR " + ((int) (clock.getTime() / 1000)) + " s\n");
			report.store();
			System.out.println(
				"Time to handleRecords: " + clock.getTime() + " ms  OR " + ((int) (clock.getTime() / 1000)) + " s");
			// System.gc();
			//success commit changes
			transaction.commit();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				transaction.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			return false;
		}
	}
	
	private boolean processRecord(String record) throws RemoteException {
		userValues = file.getValuesFromRecordString(record);
		//System.out.println("THE RECORD = "+record);
		boolean success = true;
		try {
			success = storeUserInfo();
			if (success) {
				successCount++;
				count++;
			} else {
				report.append("The problems above comes from the following line in the file:\n" + record + "\n");
				failCount++;
				count++;
			}
		} catch (AlreadyCreatedException e) {
			report.append("The following line will not be imported:\n" + record + "\n");
			alreadyChoosenCount++;
		}
		userValues = null;
		return success;
	}
	
	public void printFailedRecords() {
		if(!failedRecords.isEmpty())
		{
			report.append("\nImport failed for these records, please fix and import again:\n");
			Iterator iter = failedRecords.iterator();
			while (iter.hasNext()) {
				report.append((String) iter.next());
			}
		}

		if (!failedSchools.isEmpty()) {
			report.append("\nChild caretakers missing from database or have different names:\n");
			Iterator schools = failedSchools.iterator();
			while (schools.hasNext()) {
				report.append((String) schools.next());
			}
		}
		if (!notFoundChildren.isEmpty()) {
			report.append("\nChildren missing from database or have different names. These have been created:\n");
			Iterator chIterator = notFoundChildren.iterator();
			while (chIterator.hasNext()) {
				String name = (String) chIterator.next();
				report.append(name + "\n");
			}
		}
		if (!notFoundParent.isEmpty()) {
			report.append("\nNo parent found for child:\n");
			Iterator parents = notFoundParent.iterator();
			while (parents.hasNext()) {
				String name = (String) parents.next();
				report.append(name + "\n");
			}
		}
	}
	
	protected boolean storeUserInfo() throws RemoteException, AlreadyCreatedException {
		User child = null;
		//variables
		String caretaker = "";
		boolean isDBV = false;
		
		String PIN = getUserProperty(COLUMN_CHILD_PERSONAL_ID);
		if (PIN == null)
		{
			report.append("Could not read the personal ID");
			return false;
		}
		if (!PIDChecker.getInstance().isValid(PIN, false)) {
			report.append("Personal ID is invalid: " + PIN);
			return false;
		}
		PIN = PIDChecker.getInstance().trim(PIN);

		String childName = getUserProperty(COLUMN_CHILD_NAME);
		if (childName == null)
		{
			report.append("Could not read the Child name");
//			return false;
		}
		String unitId = getUserProperty(COLUMN_UNIT_ID);
		if (unitId == null) {
			report.append("Could not read the unit ID");
			return false;
		}
		
		String unit = getUserProperty(COLUMN_UNIT);
//		String dbvpid = getUserProperty(COLUMN_DBV_PERSONAL_ID);

		if(unit != null){
			isDBV = false;
			caretaker = unit;
		}
		else {
			report.append("Could not read the childcaretaker for child "+PIN);
			return false;
		}
		
		String groupId = getUserProperty(COLUMN_GROUP_ID);
		if (groupId == null) {
			report.append("Could not read the group ID");
			return false;
		}
		
		String groupName = getUserProperty(COLUMN_GROUP_NAME);
		
		String hours = getUserProperty(COLUMN_HOURS);
		String placementDate = getUserProperty(COLUMN_PLACEMENT_FROM);
		if (placementDate == null) {
			report.append("Failed parsing placement for " + childName);
			return false;
		}
		IWTimestamp placementFrom = new IWTimestamp();
		try {
			placementFrom.setDate(formatDate(placementDate));
		} catch (DateFormatException e1) {
			report.append("Failed parsing placement date "+placementDate+" for " + childName);
			return false;
		}
		String placementEndDate = getUserProperty(COLUMN_PLACEMENT_TO);
		IWTimestamp placementTo = null;
		if (placementEndDate == null) {
			placementTo = null;
		}
		else {
			try {
				placementTo = new IWTimestamp(formatDate(placementEndDate));
			}
			catch (Exception e) {
				placementTo = null;
			}
		}
		String sDate = getUserProperty(COLUMN_START_DATE);
		if (sDate == null) {
			report.append("Failed parsing start date for " + childName);
			return false;
		}
		IWTimestamp sDateT = new IWTimestamp();
		try {
			sDateT.setDate(formatDate(sDate));
		} catch (DateFormatException e1) {
			report.append("Failed parsing start date "+sDate+" for " + childName);
			return false;
		}

		String eDate = getUserProperty(COLUMN_END_DATE);
		IWTimestamp eDateT = null;
		if (eDate != null) {
			try {
				eDateT = new IWTimestamp();
				eDateT.setDate(formatDate(eDate));
			} catch (DateFormatException e1) {
				report.append("Failed parsing contract end date "+eDate+" for " + childName);
				return false;
			}
		}

		//database stuff
		School school = null;
		SchoolClass sClass = null;
//		SchoolYear year;
		// user
		try {
			child = biz.getUserHome().findByPersonalID(PIN);
			//debug
			if (child == null)
			{
				report.append("Could not find child with personal id "+PIN+" in database");
				return false;
			}
		} catch (FinderException e) {
			try {
				report.append("Could not find any child with personal id " + PIN + "  ");
				report.append("Child name is " + childName);
				//create a temporary user for the child
				//initialize business beans and data homes
				CommuneUserBusiness biz;
				biz = (CommuneUserBusiness) this.getServiceInstance(CommuneUserBusiness.class);
				String firstName = null, lastName = null;
				Gender gender;
				GenderHome genderHome = (GenderHome) getIDOHome(Gender.class);
				char genderChar = PIN.charAt(PIN.length()-2);
				String maleStr = "13579";
				if(maleStr.indexOf(genderChar)>-1){
					gender = genderHome.getMaleGender();
				}else {
					gender = genderHome.getFemaleGender();
				}
				IWTimestamp dateOfBirth = new IWTimestamp();
				dateOfBirth.setDate(PIN);
				
				int com = childName.indexOf(',');
				if(com > -1){
					lastName = childName.substring(0,com).trim();
					firstName = childName.substring(com+1).trim();
				}
				
				child = biz.createSpecialCitizen(firstName, "", lastName, PIN, gender, dateOfBirth);
				if (!notFoundChildren.contains(childName)) {
					notFoundChildren.add(childName);
				}
//				success = false;
			} catch (Exception ex) {
				report.append("There was an error while creating special user "+PIN+" "+childName+"\n");
				ex.printStackTrace();
				return false;
			}
		}
		try {
			//school
			//this can only work if there is only one school with this name. add more parameters for other areas
			school = sHome.findBySchoolName(caretaker);
		} catch (FinderException e) {
			report.append("Could not find any childcare taker with name " + caretaker);
			if (!failedSchools.contains(caretaker)) {
				failedSchools.add(caretaker);
			}
			return false;
		}
		if (school.getProviderStringId() == null || school.getProviderStringId().equals("null")) {
			school.setProviderStringId(unitId);
			school.store();
		}
		// set school type id
		if (school != null){
			try {
				Collection schoolTypes = school.getSchoolTypes();
				Iterator iterTypes = schoolTypes.iterator();
				
				//iterator through the school types but there is only one per provider :)
				while (iterTypes.hasNext()) {
					SchoolType type = (SchoolType) iterTypes.next();					
					schoolTypeID = ((Integer) type.getPrimaryKey()).intValue();
				}
				
			}
			catch (IDORelationshipException e){
				log (e);
			}
		}
		//school Class		
		try {
			sClass = sClassHome.findByNameAndSchool(groupName, school);
//			System.out.println("School cls found");
		} catch (FinderException e) {
			report.append("School cls for "+school.getName()+" not found creating...");
			sClass = schoolBiz.storeSchoolClass(groupName, school, null, null);
			sClass.store();
			if (sClass == null){
				report.append("Could not create the Class for "+school.getName());
				return false;
			}
		}
		if (sClass.getGroupStringId() == null || sClass.getGroupStringId().equals("null")) {
			sClass.setGroupStringId(groupId);
			sClass.store();
		}
		
		//school cls member
		//SchoolClassMember member = null;
		try {
			Collection classMembers = sClassMemberHome.findByStudentAndTypes(((Integer)child.getPrimaryKey()).intValue(), getSchoolTypes());
			Iterator oldClasses = classMembers.iterator();
			while (oldClasses.hasNext()) {
				SchoolClassMember temp = (SchoolClassMember) oldClasses.next();
				if(!temp.getSchoolClass().getSchoolClassName().equals(groupName))
				{
					report.append(child.getName()+" is already in childcare "+temp.getSchoolClass().getSchoolClassName()+" at "+temp.getSchoolClass().getSchool().getName());
					if (!isDBV)
						throw new AlreadyCreatedException();
				} else {
					report.append(child.getName()+" is already in childcare "+temp.getSchoolClass().getSchoolClassName()+" at "+temp.getSchoolClass().getSchool().getName());
				}
//				try {
//					temp.remove();
//				} catch (RemoveException e) {
//					report.append("problem removing old placement for the child "+e.toString());
//					e.printStackTrace();
//					return false;
//				}
			}
		} catch (FinderException f) {
		}
//		report.append("School cls member not found creating...");
		/*member = schoolBiz.storeSchoolClassMember(sClass, child);
		member.store();
		if (member == null)
		{
			report.append("Problem creating the Class member");
			return false;
		}*/
		//schoolclassmember finished
		
		//Create the contract
		ChildCareBusiness cc = (ChildCareBusiness) getServiceInstance(ChildCareBusiness.class);
		User parent = biz.getCustodianForChild(child);
		if (parent == null) {
			notFoundParent.add(PIN + ": " + childName);
		}
		
		IWContext iwc;
		try {
			iwc = IWContext.getInstance();
			if (performer == null)
				performer = iwc.getCurrentUser();
			if (locale == null)
				locale = iwc.getCurrentLocale();
			
			if (parent == null) {
				parent = performer;
			}
				
			int schoolID = Integer.parseInt(school.getPrimaryKey().toString());
			int classID = Integer.parseInt(sClass.getPrimaryKey().toString());
			boolean importDone = cc.importChildToProvider(-1, ((Integer)child.getPrimaryKey()).intValue(), schoolID, classID, hours, -1, schoolTypeID, null, sDateT, eDateT,
				locale, parent, performer);
			if (importDone) {
				try {
					SchoolClassMember member = cc.getLatestPlacement(((Integer)child.getPrimaryKey()).intValue(), schoolID);
					member.setRegisterDate(placementFrom.getTimestamp());
					if (placementTo != null) {
						member.setRemovedDate(placementTo.getTimestamp());
					}
					member.store();
					
//					SchoolClassMemberLog log = sClassMemberLogHome.create();
//					log.setSchoolClassMember(member);
//					log.setSchoolClass(new Integer(member.getSchoolClassId()));
//					log.setStartDate(placementFrom.getDate());
//					if (placementTo != null) {
//						log.setEndDate(placementTo.getDate());
//						log.setUserTerminating(performer);
//					}
//					log.setUserPlacing(performer);
//					log.store();
				}
				catch (FinderException fe) {
					report.append("Placement not found for child " + child.getName());
				}
//				catch (CreateException e) {
//					report.append("Could not create placement log for child " + child.getName());					
//				}
				report.append("Contract created for child "+child.getName());
			}
			else
				report.append("Failed to create contract for child "+child.getName());
		} catch (UnavailableIWContext e2) {
			report.append("Could not get the IWContext. Cannot create the contract.");
			return false;
		} catch (NumberFormatException e3) {
			report.append("NumberFormatException. SchoolID or ClassID is not a number. Cannot create the contract.");
			return false;
		}
		//finished with this user
		child = null;
		return true;
	}

	private String formatDate(String s) {
		String date = "";
		try {
			date = s.substring(0, 4) + s.substring(5, 7) + s.substring(8, 10);
		} catch (Exception e) {}
		return date;
	}
	
	public void setImportFile(ImportFile file) {
		this.file = file;
	}
	
	private Collection getSchoolTypes() throws RemoteException {
		if (childcareTypes == null)
			childcareTypes = schoolBiz.findAllSchoolTypesForSchool();
		return childcareTypes;
	}
	
	private String getUserProperty(int columnIndex) {
		String value = null;
		if (userValues != null) {
			try {
				value = (String) userValues.get(columnIndex);
			} catch (RuntimeException e) {
				return null;
			}
			//System.out.println("Index: "+columnIndex+" Value: "+value);
			if (file.getEmptyValueString().equals(value))
				return null;
			else
				return value;
		} else
			return null;
	}
	/**
	 * Rturns the value from getQueueProperty() parsed into an int
	 * @param columnIndex column to be parsed
	 * @return int value of the column. 0 is returned, if no value or unparsable value is found.
	 */
//	private int getIntQueueProperty(int columnIndex) throws NumberFormatException {
//		String sValue = getUserProperty(columnIndex);
//		return Integer.parseInt(sValue);
//	}
/*	
	private IWTimestamp getBirthDateFromPin(String pin) {
		//pin format = 190010221208 yyyymmddxxxx
		int dd = Integer.parseInt(pin.substring(6, 8));
		int mm = Integer.parseInt(pin.substring(4, 6));
		int yyyy = Integer.parseInt(pin.substring(0, 4));
		IWTimestamp dob = new IWTimestamp(dd, mm, yyyy);
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
		} catch (Exception ex) {
			ex.printStackTrace();
			return null; //if something happened
		}
	}
*/	
	/**
	 * Not used
	 * @param rootGroup The rootGroup to set
	 */
	public void setRootGroup(Group rootGroup) {
	}
	
	/**
	 * @see com.idega.block.importer.business.ImportFileHandler#getFailedRecords()
	 */
	public List getFailedRecords() {
		return failedRecords;
	}
}