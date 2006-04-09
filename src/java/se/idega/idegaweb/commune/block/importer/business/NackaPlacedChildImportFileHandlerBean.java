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
import com.idega.business.IBOServiceBean;
import com.idega.idegaweb.UnavailableIWContext;
import com.idega.presentation.IWContext;
import com.idega.user.data.Gender;
import com.idega.user.data.GenderHome;
import com.idega.user.data.Group;
import com.idega.user.data.User;
//import com.idega.user.data.UserHome;
import com.idega.util.DateFormatException;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;
/**
 * <p>Title: NackaPlacedChildImportFileHandlerBean</p>
 * <p>Description: Imports the child care queue into the database.  
 * To add this to the "Import handler" dropdown for the import function, execute the following SQL:<br>
 * insert into im_handler values(19, 'Nacka Childcare placement importer', 'se.idega.idegaweb.commune.block.importer.business.NackaPlacedChildImportFileHandlerBean', 'Imports Child placements in Nacka.')
 * <br>
 * Note that the "19" value in the SQL might have to be adjusted in the sql, 
 * depending on the number of records already inserted in the table. </p>
 * <p>Copyright (c) 2003</p>
 * <p>Company: Idega Software</p>
 * @author Joakim Johnson</a>
 * @version 1.0
 */
public class NackaPlacedChildImportFileHandlerBean extends IBOServiceBean 
implements ImportFileHandler, NackaPlacedChildImportFileHandler 
{
	private CommuneUserBusiness biz;
	//private UserHome home;
	private SchoolBusiness schoolBiz;
	private SchoolHome sHome;
	private SchoolClassHome sClassHome;
	private SchoolClassMemberHome sClassMemberHome;
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
	
	public static final String DBV = "Placerade barn";		//This is the name of the class/group that is created for the DBV

	private static final int COLUMN_CHILD_PERSONAL_ID = 0;
	private static final int COLUMN_CHILD_NAME = 1;
	private static final int COLUMN_UNIT = 2;
	private static final int COLUMN_DBV_NAME = 3;
//	private static final int COLUMN_DBV_PERSONAL_ID = 4; //Not used
	private static final int COLUMN_HOURS = 5;
	private static final int COLUMN_START_DATE = 6;
	private static final int COLUMN_END_DATE = 7;
//	private Gender female;
//	private Gender male;
	private Report report;
	private int successCount, failCount, alreadyChoosenCount, count;
	String item;
	
	public NackaPlacedChildImportFileHandlerBean() {
	}

	public boolean handleRecords() {
		this.failedSchools = new ArrayList();
		this.failedRecords = new ArrayList();
		this.notFoundChildren = new ArrayList();
		this.notFoundParent = new ArrayList();
		this.transaction = this.getSessionContext().getUserTransaction();
		this.report = new Report(this.file.getFile().getName());	//Create a report file. I will be located in the Report dir
		//Cero all counters used just for reporting purposes
		this.count = 0;
		this.failCount = 0;
		this.successCount = 0;
		this.alreadyChoosenCount = 0;

		Timer clock = new Timer();
		clock.start();
		try {
			//initialize business beans and data homes
			this.biz = (CommuneUserBusiness) this.getServiceInstance(CommuneUserBusiness.class);
			//home = biz.getUserHome();
			this.schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
			this.sHome = this.schoolBiz.getSchoolHome();
			this.sClassHome = (SchoolClassHome) this.getIDOHome(SchoolClass.class);
			this.sClassMemberHome = (SchoolClassMemberHome) this.getIDOHome(SchoolClassMember.class);
			//if the transaction failes all the users and their relations are removed
			this.transaction.begin();
			//iterate through the records and process them
			this.file.getNextRecord();	//Skip header
			while (!(this.item = (String) this.file.getNextRecord()).equals("")) {
				if (!processRecord(this.item)) {
					this.failedRecords.add(this.item);
				}
				if ((this.count % 50) == 0) {
					System.out.println(
						"NackaPlacedChildHandler processing RECORD [" + this.count + "] time: "
							+ IWTimestamp.getTimestampRightNow().toString());
				}
				this.item = null;
			}
			clock.stop();
			
			printFailedRecords();
			this.report.append("\nNackaQueueHandler processed "+ this.successCount
					+ " records successfuly out of "+ this.count+ "records.\n");
			this.report.append(this.alreadyChoosenCount+" of the selections had already been imported.\n");
			this.report.append("Time to handleRecords: " + clock.getTime() + " ms  OR " + ((int) (clock.getTime() / 1000)) + " s\n");
			this.report.store();
			System.out.println(
				"Time to handleRecords: " + clock.getTime() + " ms  OR " + ((int) (clock.getTime() / 1000)) + " s");
			// System.gc();
			//success commit changes
			this.transaction.commit();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			try {
				this.transaction.rollback();
			} catch (SystemException e) {
				e.printStackTrace();
			}
			return false;
		}
	}
	
	private boolean processRecord(String record) throws RemoteException {
		this.userValues = this.file.getValuesFromRecordString(record);
		//System.out.println("THE RECORD = "+record);
		boolean success = true;
		try {
			success = storeUserInfo();
			if (success) {
				this.successCount++;
				this.count++;
			} else {
				this.report.append("The problems above comes from the following line in the file:\n" + record + "\n");
				this.failCount++;
				this.count++;
			}
		} catch (AlreadyCreatedException e) {
			this.report.append("The following line will not be imported:\n" + record + "\n");
			this.alreadyChoosenCount++;
		}
		this.userValues = null;
		return success;
	}
	
	public void printFailedRecords() {
		if(!this.failedRecords.isEmpty())
		{
			this.report.append("\nImport failed for these records, please fix and import again:\n");
			Iterator iter = this.failedRecords.iterator();
			while (iter.hasNext()) {
				this.report.append((String) iter.next());
			}
		}

		if (!this.failedSchools.isEmpty()) {
			this.report.append("\nChild caretakers missing from database or have different names:\n");
			Iterator schools = this.failedSchools.iterator();
			while (schools.hasNext()) {
				this.report.append((String) schools.next());
			}
		}
		if (!this.notFoundChildren.isEmpty()) {
			this.report.append("\nChildren missing from database or have different names. These have been created:\n");
			Iterator chIterator = this.notFoundChildren.iterator();
			while (chIterator.hasNext()) {
				String name = (String) chIterator.next();
				this.report.append(name + "\n");
			}
		}
		if (!this.notFoundParent.isEmpty()) {
			this.report.append("\nNo parent found for child:\n");
			Iterator parents = this.notFoundParent.iterator();
			while (parents.hasNext()) {
				String name = (String) parents.next();
				this.report.append(name + "\n");
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
			this.report.append("Could not read the personal ID");
			return false;
		}
		String childName = getUserProperty(COLUMN_CHILD_NAME);
		if (childName == null)
		{
			this.report.append("Could not read the Child name");
//			return false;
		}
		String unit = getUserProperty(COLUMN_UNIT);
		String dbv = getUserProperty(COLUMN_DBV_NAME);
//		String dbvpid = getUserProperty(COLUMN_DBV_PERSONAL_ID);

		if(unit != null){
			isDBV = false;
			caretaker = unit;
		} else if(dbv != null){
			caretaker = dbv;
			isDBV = true;
		} else {
			this.report.append("Could not read the childcaretaker for child "+PIN);
			return false;
		}
		String hours = getUserProperty(COLUMN_HOURS);
		String sDate = getUserProperty(COLUMN_START_DATE);
		if (sDate == null) {
			this.report.append("Failed parsing start date for " + childName);
			return false;
		}
		IWTimestamp sDateT = new IWTimestamp();
		try {
			sDateT.setDate(sDate);
		} catch (DateFormatException e1) {
			this.report.append("Failed parsing start date "+sDate+" for " + childName);
			return false;
		}
		String eDate = getUserProperty(COLUMN_END_DATE);
		IWTimestamp eDateT = null;
		if (eDate == null) {
			eDateT = null;
		}
		else {
			try {
				eDateT = new IWTimestamp(eDate);
			}
			catch (Exception e) {
				eDateT = null;
			}
		}

		//database stuff
		School school = null;
		SchoolClass sClass = null;
//		SchoolYear year;
		// user
		try {
			child = this.biz.getUserHome().findByPersonalID(PIN);
			//debug
			if (child == null)
			{
				this.report.append("Could not find child with personal id "+PIN+" in database");
				return false;
			}
		} catch (FinderException e) {
			try {
				this.report.append("Could not find any child with personal id " + PIN + "  ");
				this.report.append("Child name is " + childName);
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
				if (!this.notFoundChildren.contains(childName)) {
					this.notFoundChildren.add(childName);
				}
//				success = false;
			} catch (Exception ex) {
				this.report.append("There was an error while creating special user "+PIN+" "+childName+"\n");
				ex.printStackTrace();
				return false;
			}
		}
		try {
			//school
			//this can only work if there is only one school with this name. add more parameters for other areas
			school = this.sHome.findBySchoolName(caretaker);
		} catch (FinderException e) {
			this.report.append("Could not find any childcare taker with name " + caretaker);
			if (!this.failedSchools.contains(caretaker)) {
				this.failedSchools.add(caretaker);
			}
			return false;
		}
		//school class		
		try {
			sClass = this.sClassHome.findByNameAndSchool(DBV, school);
//			System.out.println("School cls found");
		} catch (FinderException e) {
			this.report.append("School cls for "+school.getName()+" not found creating...");
			sClass = this.schoolBiz.storeSchoolClass(DBV, school, null, null);
			sClass.store();
			if (sClass == null){
				this.report.append("Could not create the class for "+school.getName());
				return false;
			}
		}
		//school cls member
		//SchoolClassMember member = null;
		try {
			Collection classMembers = this.sClassMemberHome.findByStudentAndTypes(((Integer)child.getPrimaryKey()).intValue(), getSchoolTypes());
			Iterator oldClasses = classMembers.iterator();
			while (oldClasses.hasNext()) {
				SchoolClassMember temp = (SchoolClassMember) oldClasses.next();
				if(!temp.getSchoolClass().getSchoolClassName().equals(DBV))
				{
					this.report.append(child.getName()+" is already in childcare "+temp.getSchoolClass().getSchoolClassName()+" at "+temp.getSchoolClass().getSchool().getName());
					if (!isDBV) {
						throw new AlreadyCreatedException();
					}
				} else {
					this.report.append(child.getName()+" is already in childcare "+temp.getSchoolClass().getSchoolClassName()+" at "+temp.getSchoolClass().getSchool().getName());
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
			report.append("Problem creating the class member");
			return false;
		}*/
		//schoolclassmember finished
		
		//Create the contract
		ChildCareBusiness cc = (ChildCareBusiness) getServiceInstance(ChildCareBusiness.class);
		User parent = this.biz.getCustodianForChild(child);
		if (parent == null) {
			this.notFoundParent.add(PIN + ": " + childName);
			return false;
		}
		
		IWContext iwc;
		try {
			iwc = IWContext.getInstance();
			if (this.performer == null) {
				this.performer = iwc.getCurrentUser();
			}
			if (this.locale == null) {
				this.locale = iwc.getCurrentLocale();
			}
				
			int schoolID = Integer.parseInt(school.getPrimaryKey().toString());
			int classID = Integer.parseInt(sClass.getPrimaryKey().toString());
			boolean importDone = cc.importChildToProvider(-1, ((Integer)child.getPrimaryKey()).intValue(), schoolID, classID, hours, -1, -1, null, sDateT, eDateT,
				this.locale, parent, this.performer);
			if (importDone) {
				this.report.append("Contract created for child "+child.getName());
			}
			else {
				this.report.append("Failed to create contract for child "+child.getName());
			}
		} catch (UnavailableIWContext e2) {
			this.report.append("Could not get the IWContext. Cannot create the contract.");
			return false;
		} catch (NumberFormatException e3) {
			this.report.append("NumberFormatException. SchoolID or ClassID is not a number. Cannot create the contract.");
			return false;
		}
		//finished with this user
		child = null;
		return true;
	}
	
	public void setImportFile(ImportFile file) {
		this.file = file;
	}
	
	private Collection getSchoolTypes() throws RemoteException {
		if (this.childcareTypes == null) {
			this.childcareTypes = this.schoolBiz.findAllSchoolTypesForSchool();
		}
		return this.childcareTypes;
	}
	
	private String getUserProperty(int columnIndex) {
		String value = null;
		if (this.userValues != null) {
			try {
				value = (String) this.userValues.get(columnIndex);
			} catch (RuntimeException e) {
				return null;
			}
			//System.out.println("Index: "+columnIndex+" Value: "+value);
			if (this.file.getEmptyValueString().equals(value)) {
				return null;
			}
			else {
				return value;
			}
		}
		else {
			return null;
		}
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
		return this.failedRecords;
	}

	/*private class headerException extends Exception{
		public headerException(){
			super();
		}
		
		public headerException(String s){
			super(s);
		}
	}*/
}
