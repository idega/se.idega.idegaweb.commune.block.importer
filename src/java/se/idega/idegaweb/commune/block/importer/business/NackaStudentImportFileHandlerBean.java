package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.data.*;
import com.idega.block.school.business.*;
import com.idega.business.IBOServiceBean;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.user.data.UserHome;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;

import se.idega.idegaweb.commune.school.business.*;


/**
 * <p>Title: NackaStudentImportFileHandlerBean</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is"> Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */



public class NackaStudentImportFileHandlerBean extends IBOServiceBean implements NackaStudentImportFileHandler{


  private UserBusiness biz;
  private UserHome home;
  private SchoolYearBusiness schoolYearBiz;
  private SchoolBusiness schoolBiz;
  
  private SchoolYearHome sYearHome;
  private SchoolHome sHome;
  private SchoolClassHome sClassHome;
  private SchoolClassMemberHome sClassMemberHome;

  private SchoolSeason season = null;
    
  private ImportFile file;
  private UserTransaction transaction;
  
  private ArrayList userValues;
  private ArrayList failedRecords = new ArrayList();

  private final int COLUMN_SCHOOL_NAME = 0;  
  private final int COLUMN_PERSONAL_ID = 1;
  private final int COLUMN_SCHOOL_YEAR = 2; 
  private final int COLUMN_CLASS = 3; 
  

  	
  public NackaStudentImportFileHandlerBean(){}
  
  public boolean handleRecords() throws RemoteException{
    transaction =  this.getSessionContext().getUserTransaction();
    
    try{
    	season = ((SchoolChoiceBusiness)this.getServiceInstance(SchoolChoiceBusiness.class)).getCurrentSeason();
    }
    catch(FinderException ex){
    	ex.printStackTrace();
    	System.err.println("NackaStudentHandler:Current School season is not defined");
    	return false;
    }
    
    Timer clock = new Timer();
    clock.start();

    try {
      //initialize business beans and data homes
      biz = (UserBusiness) this.getServiceInstance(UserBusiness.class);
      home = biz.getUserHome();
      
      
      schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
      sHome = schoolBiz.getSchoolHome();
           
      schoolYearBiz = (SchoolYearBusiness) this.getServiceInstance(SchoolYearBusiness.class);
      sYearHome = schoolYearBiz.getSchoolYearHome();
      
      sClassHome = (SchoolClassHome)this.getIDOHome(SchoolClass.class);
 	  
 	  sClassMemberHome = (SchoolClassMemberHome)this.getIDOHome(SchoolClassMember.class);
      
  
      //if the transaction failes all the users and their relations are removed
      transaction.begin();

      //iterate through the records and process them
      String item;

      int count = 0;
      while ( !(item=(String)file.getNextRecord()).equals("") ) {
        count++;

		
           if( ! processRecord(item) ) failedRecords.add(item);

        if( (count % 500) == 0 ){
          System.out.println("NackaStudentHandler processing RECORD ["+count+"] time: "+IWTimestamp.getTimestampRightNow().toString());
        }
        
        item = null;
      }
      
      printFailedRecords();

      clock.stop();
      System.out.println("Time to handleRecords: "+clock.getTime()+" ms  OR "+((int)(clock.getTime()/1000))+" s");

      // System.gc();
      //success commit changes
      transaction.commit();


      return true;
    }
    catch (Exception ex) {
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

  private boolean processRecord(String record) throws RemoteException{
    userValues = file.getValuesFromRecordString(record);
    System.out.println("THE RECORD = "+record);

	boolean success = storeUserInfo();
  
    userValues = null;

    return success;
  }
  
  public void printFailedRecords(){
  	System.out.println("Import failed for these records, please fix and import again:");
  
  	Iterator iter = failedRecords.iterator();
  	while (iter.hasNext()) {
		System.out.println((String) iter.next());
	}
	
  	System.out.println("Import failed for these records, please fix and import again:");
  }

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
	
		//school
		//this can only work if there is only one school with this name. add more parameters for other areas
		school = (School) sHome.findAllBySchoolName(schoolName).iterator().next();
		if( school == null ) return false;
		
		//school year	
		if( schoolYear == null ) return false;
		else if( schoolYear.equals("0") ) schoolYear = "F";
		
		year = sYearHome.findByYearName(schoolYear);
	}
	catch (FinderException e) {
		System.out.println("User not found/School not found/SchoolYear not found for PIN : "+PIN);
		return false;
	}	
		
	//school class		
	SchoolClass sClass = null;
	
	try {
		
		sClassHome.findBySchoolClassNameSchoolSchoolYearSchoolSeason(schoolClass,school,year,season);
		
	}catch (FinderException e) {
		//e.printStackTrace();
		System.out.println("School class not found creating...");	
		
		try {
			sClass = schoolBiz.createSchoolClass(schoolClass,school,year,season);
			sClass.store();
			
		}catch (CreateException ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	//school class member
	SchoolClassMember member = null;
	try {
		
		member = sClassMemberHome.findByUserAndSchoolClass(user,sClass);
		
	}catch (FinderException e) {
		//e.printStackTrace();
		System.out.println("School class member not found creating...");	
		
		try {
			member = schoolBiz.createSchoolClassMember(sClass, user);
			member.store();
			
		}catch (CreateException ex) {
			ex.printStackTrace();
			return false;
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


/**
 * Not used
 * @param rootGroup The rootGroup to set
 */
public void setRootGroup(Group rootGroup) {
}

  }