package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

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
import com.idega.block.school.data.SchoolSeasonHome;
import com.idega.block.school.data.SchoolStudyPath;
import com.idega.block.school.data.SchoolStudyPathHome;
import com.idega.block.school.data.SchoolType;
import com.idega.block.school.data.SchoolTypeHome;
import com.idega.block.school.data.SchoolYear;
import com.idega.block.school.data.SchoolYearHome;
import com.idega.business.IBORuntimeException;
import com.idega.business.IBOServiceBean;
import com.idega.core.contact.data.Phone;
import com.idega.core.contact.data.PhoneHome;
import com.idega.data.IDOAddRelationshipException;
import com.idega.data.IDORelationshipException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;

/**
 * 
 * Description: <br>
 * Copyright: Idega Software 2004 <br>
 * Company: Idega Software <br>
 * @author <a href="mailto:birna@idega.is">Birna Iris Jonsdottir</a>
 */

public class MusicSchoolImportFileHandlerBean extends IBOServiceBean implements ImportFileHandler,MusicSchoolImportFileHandler{

	private UserTransaction transaction;
	private SchoolBusiness schoolBiz;
	private SchoolHome sHome;
	
	private SchoolType fullStudy;
	private SchoolType halfStudy;
	
	private ImportFile file;
	private String line;
	private StringTokenizer tokenizer;
		
	private ArrayList schoolValues;
	private ArrayList failedRecords = new ArrayList();
	
	public MusicSchoolImportFileHandlerBean() {}
	
	public boolean handleRecords() throws RemoteException{
		transaction =  this.getSessionContext().getUserTransaction();
		
		try {
			schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
			sHome = schoolBiz.getSchoolHome();
			
			fullStudy = getFullTimeStudySchoolType(schoolBiz);
			halfStudy = getHalfTimeStudySchoolType(schoolBiz);
			
			transaction.begin();
			
			String item;
			
			while ( !(item=(String)file.getNextRecord()).equals("") ) {
				
				if(!processRecord(item)) {
					failedRecords.add(item);
				}				
			}
			
			transaction.commit();
			return true;
		}catch (Exception ex) {
			ex.printStackTrace();

			try {
				transaction.rollback();
			}
			catch (SystemException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * The record is:
	 * nr,musicSchoolName,studentSSN,studentTelNr,preSchool,
	 * instrument,singing,level,levelNr,
	 * instrument2,level2Nr,level2,
	 * instument3,level3Nr,level3
	 */
	private boolean processRecord(String record) throws RemoteException{
		//data elements from file
		String nr = file.getValueAtIndexFromRecordString(1,record);
		String musicSchoolName = file.getValueAtIndexFromRecordString(2,record);
		String studentSSN = file.getValueAtIndexFromRecordString(3,record);
		String studentTelNr = file.getValueAtIndexFromRecordString(4,record);
		String preSchool = file.getValueAtIndexFromRecordString(5,record);
		String instrument1 = file.getValueAtIndexFromRecordString(6,record);
		String singing = file.getValueAtIndexFromRecordString(7,record);
		String level1 = file.getValueAtIndexFromRecordString(8,record);
		String level1Nr = file.getValueAtIndexFromRecordString(9,record);
		String instrument2 = file.getValueAtIndexFromRecordString(10,record);
		String level2Nr = file.getValueAtIndexFromRecordString(11,record);
		String level2 = file.getValueAtIndexFromRecordString(12,record);
		String instrument3 = file.getValueAtIndexFromRecordString(13,record);
		String level3Nr = file.getValueAtIndexFromRecordString(14,record);
		String level3 = file.getValueAtIndexFromRecordString(15,record);

		boolean success = storeInfo(nr,musicSchoolName,studentSSN,studentTelNr,preSchool,
				instrument1,singing,level1,level1Nr,
				instrument2,level2Nr,level2,
				instrument3,level3Nr,level3);

		return success;
	}
	protected boolean storeInfo(String nr,String musicSchoolName,
			String studentSSN,String studentTelNr,String preSchool,
			String instrument1,String singing,String level1,String level1Nr,
			String instrument2,String level2Nr,String level2,
			String instrument3,String level3Nr,String level3) throws RemoteException{
		
		School musicSchool = null;
		SchoolClass mainClass = null;
		SchoolSeason schoolSeason = null;
		SchoolClassMember student = null;
		SchoolStudyPath firstInstrument = null;
		SchoolStudyPath secondInstrument = null;
		SchoolStudyPath thirdInstrument = null;
		String mainClassName = "mainClass";
		SchoolClassHome schoolClassHome = schoolBiz.getSchoolClassHome();
		SchoolSeasonHome schoolSeasonHome = schoolBiz.getSchoolSeasonHome();
		
		
		try {
			musicSchool = sHome.findBySchoolName(musicSchoolName);
		}catch(FinderException e) {
			try {
				musicSchool = sHome.create();
				musicSchool.setSchoolName(musicSchoolName);
				musicSchool.addSchoolType(fullStudy);
				musicSchool.addSchoolType(halfStudy);
				musicSchool.store();
				try {
					mainClass = schoolClassHome.create();
					mainClass.setSchoolClassName(mainClassName);
					Integer id = (Integer) musicSchool.getPrimaryKey();
					mainClass.setSchoolId(id.intValue());
					schoolSeason = schoolSeasonHome.findSeasonByDate(new IWTimestamp().getDate());
					if(schoolSeason != null) {
						Integer seaId = (Integer) schoolSeason.getPrimaryKey();
						mainClass.setSchoolSeasonId(seaId.intValue());
					}
					mainClass.store();
				}catch(CreateException cre) {
					mainClass = null;
				}catch(FinderException fex) {
				}
			}catch(CreateException ce) {
				musicSchool = null;
			}catch(IDOAddRelationshipException idoe) {
			}
		}		
		
		student = processStudent(studentSSN, studentTelNr, musicSchool, mainClass);

		if(student != null) {
			if(instrument1 != null && !instrument1.equals("")) {
				processInstrument(instrument1, musicSchool, mainClass, student, firstInstrument, level1Nr, level1);
			}
			if(instrument2 != null && !instrument2.equals("")) {
				processInstrument(instrument2, musicSchool, mainClass, student, secondInstrument, level2Nr, level2);
			}
			if(instrument3 != null && !instrument3.equals("")) {
				processInstrument(instrument3, musicSchool, mainClass, student, thirdInstrument, level3Nr, level3);
			}
		}
		
		return true;
	}
	
	/**
	 * Processes the music school student info. Gets a user by ssn from ic_user table, creates a new student (SchoolClassMember),
	 * stores the students phone number in the ic_user table, connects SchoolClass to the student (the schoolSeason is accessable
	 * through the schoolClass).
	 * @param studentSSN - students social security number - used to find the student in ic_user table
	 * @param studentTelNr - stored in the ic_user table
	 * @param musicSchool - the musicSchool
	 * @param mainClass - this is the main (the only) class in the current music school
	 * @return student (SchoolClassMember) - null if failed to create student
	 * @throws RemoteException
	 */
	private SchoolClassMember processStudent(String studentSSN, String studentTelNr, School musicSchool, SchoolClass mainClass) throws RemoteException {
		SchoolClassMemberHome studentHome = schoolBiz.getSchoolClassMemberHome();
		UserBusiness userBiz = (UserBusiness) this.getServiceInstance(UserBusiness.class);
		SchoolClassMember student = null;
		User studentUser = null;
		Phone studentPhone = null;
		try {
			studentUser = userBiz.getUser(studentSSN);
		}catch(FinderException ce) {
			//studentUser = null;
			//this is temporary for testing on an empty database:
			try {
				studentUser = userBiz.createUserByPersonalIDIfDoesNotExist(null,studentSSN,null,null);
			}catch(CreateException crEx) {
				studentUser = null;
			}
			
		}
		if(studentUser != null) {
			Integer studentID = (Integer) studentUser.getPrimaryKey();
			PhoneHome phHome = userBiz.getPhoneHome();
			try {
				studentPhone = phHome.create();
				studentPhone.setNumber(studentTelNr);
				studentPhone.store();
				
				studentUser.addPhone(studentPhone);
				studentUser.store();
				
				Integer musicSchoolID = (Integer) musicSchool.getPrimaryKey();
				try {
					student = studentHome.findByUserAndSchool(studentID.intValue(),musicSchoolID.intValue());
				}catch(FinderException fEx) {
					student = studentHome.create();
					student.setClassMemberId(studentID.intValue());
				}
				if(mainClass != null) {
					Integer mainClassID = (Integer) mainClass.getPrimaryKey();
					student.setSchoolClassId(mainClassID.intValue());					
				}
				student.store();
				
				
			}catch(CreateException crEx) {
				student = null;
			}catch(IDOAddRelationshipException idoex) {
				System.out.println("cannot add phone to studentUser!");
			}
		}
		return student;
	}

	/**
	 * Processes the instrument (SchoolStudyPath). Creates it if it doesn't exist.
	 * Connects instrument to musicSchool if it doesn't exist for musicSchool
	 * Connects instrument to mainClass if it doesn't exist for mainClass
	 * Connects instrument to student (SchoolClassMember)
	 * Connects the level to the mainClass, the student and the instrument
	 * @param instrument1
	 * @param musicSchool
	 * @param student
	 * @param firstInstrument
	 * @param instrumentHome
	 */
	private void processInstrument(String instrumentCode, School musicSchool, SchoolClass mainClass, SchoolClassMember student, SchoolStudyPath instrument, String levelNr, String levelString) throws RemoteException{
		SchoolStudyPathHome instrumentHome = schoolBiz.getSchoolStudyPathHome();
		SchoolYearHome levelHome = schoolBiz.getSchoolYearHome();
		SchoolYear level = null;
		SchoolYear levelStr = null;
		
		try {
			instrument = instrumentHome.findByCode(instrumentCode);
		}catch(FinderException fEx) {
			try {
				instrument = instrumentHome.create();
				instrument.setCode(instrumentCode);
				instrument.store();
			}catch(CreateException crEx) {
				instrument = null;
			}			
		}
		if(instrument != null) {
			try {
				Collection i = musicSchool.findRelatedStudyPaths();
				Iterator iIter = i.iterator();
				boolean b = false;
				while(iIter.hasNext()) {
					SchoolStudyPath inst = (SchoolStudyPath) iIter.next();
					if(inst.getCode() == instrument.getCode()) {
						b = true;
					}
				}
				if(!b) {
					//SchoolStudyPath (many-to-many) School
					instrument.addSchool(musicSchool);
				}			
				//SchoolClassMember (many-to-many) SchoolStudyPath
				student.addToSchoolStudyPath(instrument);	
				Collection in = mainClass.findRelatedStudyPaths();
				Iterator inIter = in.iterator();
				while(inIter.hasNext()) {
					SchoolStudyPath inst = (SchoolStudyPath) inIter.next();
					if(inst.getCode() == instrument.getCode()) {
						instrument = inst;
					}
				}
				//SchoolClass (many-to-many) SchoolStudyPath
				mainClass.addStudyPath(instrument);
			}catch(IDOAddRelationshipException idoEx) {				
			}	catch(IDORelationshipException iEx) {
				
			}
			try {
				if(levelNr != null && !levelNr.equals("")) {
					level = levelHome.create();
					level.setSchoolCategory(schoolBiz.getCategoryMusicSchool());
					level.setSchoolYearName(levelNr);
					level.store();
					//SchoolClass (many-to-many) SchoolYear
					mainClass.addSchoolYear(level);
					//SchoolStudyPath (many-to-many) SchoolYear
					instrument.addSchoolYear(level);
					//SchoolClassMember (many-to-many) SchoolYear
					student.addSchoolYear(level);

				}
				if(levelString != null && !levelString.equals("")) {
					levelStr = levelHome.create();
					levelStr.setSchoolCategory(schoolBiz.getCategoryMusicSchool());
					levelStr.setSchoolYearName(levelString);
					levelStr.store();
					if(level == null) {
						//SchoolClass (many-to-many) SchoolYear (only if levelNr is empty)
						mainClass.addSchoolYear(levelStr);
						//SchoolStudyPath (many-to-many) SchoolYear
						instrument.addSchoolYear(levelStr);
						//SchoolClassMember (many-to-many) SchoolYear
						student.addSchoolYear(levelStr);
					}
				}
				
				
			}catch(CreateException crEx) {
				
			}catch(IDOAddRelationshipException idoEx) {
				
			}
		}
	}

	private SchoolType getHalfTimeStudySchoolType(SchoolBusiness schoolBiz) {
		String schoolTypeKey="sch_type.music_school_half_time_study";
		return getSchoolType(schoolBiz,schoolTypeKey,"MusicSchHalf");
	}
	private SchoolType getFullTimeStudySchoolType(SchoolBusiness schoolBiz) {
		String schoolTypeKey="sch_type.music_school_full_time_study";
		return getSchoolType(schoolBiz,schoolTypeKey,"MusicSchFull");
	}
	
	
	/**
	 * If school type with schoolTypeKey does not exist it is created
	 * @param schoolBiz
	 * @param schoolTypeKey
	 * @param SchoolTypeName
	 * @return the school type - 
	 */
	private SchoolType getSchoolType(
			SchoolBusiness schoolBiz,
			String schoolTypeKey,
			String SchoolTypeName) {
		SchoolType type=null;
		try {
			SchoolTypeHome stHome = schoolBiz.getSchoolTypeHome();
			try {
				type = stHome.findByTypeKey(schoolTypeKey);
			} catch (FinderException fe) {
				try {
					type = stHome.create();
					type.setLocalizationKey(schoolTypeKey);
					type.setSchoolTypeName(SchoolTypeName);
					type.setCategory(schoolBiz.getCategoryMusicSchool());					
					type.store();
				} catch (Exception e) {
					throw new IBORuntimeException(e);
				}
			}
		} catch (RemoteException re) {
			re.printStackTrace();
		}
		return type;
	}
	
	public void setImportFile(ImportFile file) throws RemoteException{
		this.file = file;
	}
	public void setRootGroup(Group rootGroup) throws RemoteException{
	}
	public List getFailedRecords() throws RemoteException{
		return failedRecords;
	}
	
}
