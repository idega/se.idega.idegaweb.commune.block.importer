package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

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
	private SchoolSeason schoolSeason;
	
	private ImportFile file;
	//private String line;
	//private StringTokenizer tokenizer;
		
	//private ArrayList schoolValues;
	private ArrayList failedRecords = new ArrayList();
	
	public MusicSchoolImportFileHandlerBean() {}
	
	public boolean handleRecords() throws RemoteException{
		transaction =  this.getSessionContext().getUserTransaction();
		
		try {
			schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
			sHome = schoolBiz.getSchoolHome();
			
			fullStudy = getFullTimeStudySchoolType(schoolBiz);
			halfStudy = getHalfTimeStudySchoolType(schoolBiz);
			SchoolSeasonHome schoolSeasonHome = schoolBiz.getSchoolSeasonHome();
			schoolSeason = schoolSeasonHome.findSeasonByDate(new IWTimestamp().getDate());
			
			transaction.begin();
			
			String item;
			int count = 1;
			
			while ( !(item=(String)file.getNextRecord()).equals("") ) {
				
				if(!processRecord(item)) {
					failedRecords.add(item);
				}
				else {
					System.out.println("Processed record number: "+ (count++));
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
		int index = 1;
		index++;
		String musicSchoolName = file.getValueAtIndexFromRecordString(index++,record);
		String studentSSN = file.getValueAtIndexFromRecordString(index++,record);
		String studentName = file.getValueAtIndexFromRecordString(index++,record);
		index++;
		index++;
		String studentTelNr = file.getValueAtIndexFromRecordString(index++,record);
		String preSchool = file.getValueAtIndexFromRecordString(index++,record);
		String instrument1 = file.getValueAtIndexFromRecordString(index++,record);
		String singing = file.getValueAtIndexFromRecordString(index++,record);
		String level1 = file.getValueAtIndexFromRecordString(index++,record);
		String level1Nr = file.getValueAtIndexFromRecordString(index++,record);
		String instrument2 = file.getValueAtIndexFromRecordString(index++,record);
		String level2Nr = file.getValueAtIndexFromRecordString(index++,record);
		String level2 = file.getValueAtIndexFromRecordString(index++,record);
		String instrument3 = file.getValueAtIndexFromRecordString(index++,record);
		String level3Nr = file.getValueAtIndexFromRecordString(index++,record);
		String level3 = file.getValueAtIndexFromRecordString(index++,record);

		boolean success = storeInfo(musicSchoolName,studentName,studentSSN,studentTelNr,preSchool,
				instrument1,singing,level1,level1Nr,
				instrument2,level2Nr,level2,
				instrument3,level3Nr,level3);

		return success;
	}
	protected boolean storeInfo(String musicSchoolName,
			String studentName,String studentSSN,String studentTelNr,String preSchool,
			String instrument1,String singing,String level1,String level1Nr,
			String instrument2,String level2Nr,String level2,
			String instrument3,String level3Nr,String level3) throws RemoteException{
		
		School musicSchool = null;
		SchoolClass mainClass = null;
		
		SchoolStudyPath firstInstrument = null;
		SchoolStudyPath secondInstrument = null;
		SchoolStudyPath thirdInstrument = null;
		String mainClassName = "mainClass";
		SchoolClassHome schoolClassHome = schoolBiz.getSchoolClassHome();
		
		
		try {
			musicSchool = sHome.findBySchoolName(musicSchoolName);
		}catch(FinderException e) {
			try {
				musicSchool = sHome.create();
				musicSchool.setSchoolName(musicSchoolName);
				musicSchool.store();
				musicSchool.addSchoolType(fullStudy);
				musicSchool.addSchoolType(halfStudy);
			}catch(CreateException ce) {
				musicSchool = null;
			}catch(IDOAddRelationshipException idoe) {
			}
		}
		
		try {
			mainClass = schoolClassHome.findByNameAndSchool(mainClassName, musicSchool);
		}
		catch(FinderException fex) {
			try {
				mainClass = schoolClassHome.create();
				mainClass.setSchoolClassName(mainClassName);
				Integer id = (Integer) musicSchool.getPrimaryKey();
				mainClass.setSchoolId(id.intValue());
				if(schoolSeason != null) {
					Integer seaId = (Integer) schoolSeason.getPrimaryKey();
					mainClass.setSchoolSeasonId(seaId.intValue());
				}
				mainClass.store();
			}
			catch(CreateException cre) {
				mainClass = null;
			}
		}
		
		if (singing != null && !singing.equals(" ") && !(instrument1 != null && !instrument1.equals(" "))) {
			instrument1 = singing;
		}
		if (preSchool != null && !preSchool.equals(" ")) {
			level1 = preSchool;
		}
		
//		student = processStudent(studentSSN, studentTelNr, musicSchool, schoolSeason, mainClass, instrument);
		if(instrument1 != null && !instrument1.equals(" ")) {
			processInstrument(instrument1, musicSchool, mainClass, schoolSeason, firstInstrument, level1Nr, level1, studentName, studentSSN, studentTelNr);
		}
		if(instrument2 != null && !instrument2.equals(" ")) {
			processInstrument(instrument2, musicSchool, mainClass, schoolSeason, secondInstrument, level2Nr, level2, studentName, studentSSN, studentTelNr);
		}
		if(instrument3 != null && !instrument3.equals(" ")) {
			processInstrument(instrument3, musicSchool, mainClass, schoolSeason, thirdInstrument, level3Nr, level3, studentName, studentSSN, studentTelNr);
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
	private SchoolClassMember processStudent(String studentName, String studentSSN, String studentTelNr, School musicSchool, SchoolSeason schoolSeason, SchoolClass mainClass, SchoolStudyPath instrument) throws RemoteException {
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
				studentUser = userBiz.createUserByPersonalIDIfDoesNotExist(studentName,studentSSN,null,null);
			}catch(CreateException crEx) {
				studentUser = null;
			}
			
		}
		if(studentUser != null) {			
			if(studentTelNr != null && !studentTelNr.equals(" ")) {
				PhoneHome phHome = userBiz.getPhoneHome();
				try {
					studentPhone = phHome.findUsersHomePhone(studentUser);
					studentPhone.setNumber(studentTelNr);
					studentPhone.store();
				}
				catch(FinderException fEx) {
					try {
						studentPhone = phHome.create();
						studentPhone.setNumber(studentTelNr);
						studentPhone.store();
					}catch(CreateException crEx) {
						studentPhone = null;
					}
				}
				if(studentPhone != null) {
					try {
						studentUser.addPhone(studentPhone);
						studentUser.store();
					}catch(IDOAddRelationshipException idoEx) {						
					}				
				}
			}
			Integer userID = (Integer) studentUser.getPrimaryKey();
			Integer musicSchoolID = new Integer(-1);
			Integer seasonID = new Integer(-1);
			Integer instrumentID = new Integer(-1);
			
			if(musicSchool != null) {
				musicSchoolID = (Integer) musicSchool.getPrimaryKey();
			}
			if(schoolSeason != null) {
				seasonID = (Integer) schoolSeason.getPrimaryKey();
			}
			if(instrument != null) {
				instrumentID = (Integer) instrument.getPrimaryKey();
			}
			try {
				student = studentHome.findByUserAndSchoolAndSeasonAndStudyPath(userID.intValue(),musicSchoolID.intValue(),seasonID.intValue(),instrumentID.intValue());				
			}catch(FinderException fEx) {
				try {
					student = studentHome.create();
					student.setClassMemberId(userID.intValue());
					if(mainClass != null) {
						Integer mainClassID = (Integer) mainClass.getPrimaryKey();
						//SchoolClassmember (many-to-one) SchoolClass
						student.setSchoolClassId(mainClassID.intValue());					
					}
					student.store();
				}catch(CreateException crEx) {
					student = null;
				}
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
	private void processInstrument(String instrumentCode, School musicSchool, SchoolClass mainClass, SchoolSeason schoolSeason, SchoolStudyPath instrument, String levelNr, String levelString, String studentName, String studentSSN, String studentTelNr) throws RemoteException{
		SchoolStudyPathHome instrumentHome = schoolBiz.getSchoolStudyPathHome();
		SchoolYearHome levelHome = schoolBiz.getSchoolYearHome();
		SchoolYear level = null;
		SchoolYear levelStr = null;
		SchoolClassMember student = null;
		
		try {
			instrument = instrumentHome.findByCode(instrumentCode);
		}catch(FinderException fEx) {
			try {
				instrument = instrumentHome.create();
				instrument.setCode(instrumentCode);
				instrument.setDescription(instrumentCode);
				instrument.setSchoolCategory(schoolBiz.getCategoryMusicSchool());
				instrument.store();
			}
			catch(CreateException crEx) {
				instrument = null;
			}			
		}
		
		if(instrument != null) {
			student = processStudent(studentName,studentSSN,studentTelNr,musicSchool,schoolSeason,mainClass,instrument);
			try {
				instrument.addSchool(musicSchool);
			}
			catch(IDOAddRelationshipException idoEx) {				
			}
			try {
				mainClass.addStudyPath(instrument);
			}
			catch(IDOAddRelationshipException idoEx) {
				
			}
			
			student.setStudyPathId(((Integer)instrument.getPrimaryKey()).intValue());
			student.store();
				
			try {
				if(levelNr != null && !levelNr.equals(" ")) {
					try {
						level = schoolBiz.getSchoolYearHome().findByYearName(schoolBiz.getCategoryMusicSchool(), levelNr);
					}
					catch (FinderException fe) {
						level = levelHome.create();
						level.setSchoolCategory(schoolBiz.getCategoryMusicSchool());
						level.setSchoolYearName(levelNr);
						level.setLocalizedKey("sch_year." + levelNr);
						level.setIsSelectable(false);
						level.store();
					}
					//SchoolClass (many-to-many) SchoolYear
					try {
						mainClass.addSchoolYear(level);
					}
					catch (IDOAddRelationshipException iare) { /*Connection already exists...*/ }
					
					Integer levelID = (Integer) level.getPrimaryKey();
					//SchoolClassMember (many-to-one) SchoolYear
					student.setSchoolYear(levelID.intValue());
					student.store();

				}
				if(levelString != null && !levelString.equals(" ")) {
					try {
						level = schoolBiz.getSchoolYearHome().findByYearName(schoolBiz.getCategoryMusicSchool(), levelString);
					}
					catch (FinderException fe) {
						levelStr = levelHome.create();
						levelStr.setSchoolCategory(schoolBiz.getCategoryMusicSchool());
						levelStr.setSchoolYearName(levelString);
						levelStr.setLocalizedKey("sch_year." + levelString);
						levelStr.setIsSelectable(false);
						levelStr.store();
					}
					if(level == null) {
						//SchoolClass (many-to-many) SchoolYear (only if levelNr is empty)
						try {
							mainClass.addSchoolYear(levelStr);
						}
						catch (IDOAddRelationshipException iare) { /*Connection already exists...*/ }
						
						Integer levelStrID = (Integer) levelStr.getPrimaryKey();
						//SchoolClassMember (many-to-many) SchoolYear
						student.setSchoolYear(levelStrID.intValue());
						student.store();
					}
				}

				
			}catch(CreateException crEx) {
				
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
