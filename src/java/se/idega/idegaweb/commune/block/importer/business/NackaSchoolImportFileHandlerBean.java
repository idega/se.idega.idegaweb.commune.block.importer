package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.data.*;
import com.idega.block.school.business.*;
import com.idega.business.IBOServiceBean;
import com.idega.data.IDOAddRelationshipException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.user.data.UserHome;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;

import se.idega.idegaweb.commune.school.business.*;


/**
 * <p>Title: NackaSchoolImportFileHandlerBean</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is"> Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */



public class NackaSchoolImportFileHandlerBean extends IBOServiceBean {//implements NackaSchoolImportFileHandler{



  private SchoolBusiness schoolBiz;
  private SchoolHome sHome;
    
  private ImportFile file;
  private UserTransaction transaction;
  
  private ArrayList schoolValues;
  private ArrayList failedRecords = new ArrayList();
  
  private boolean isPreSchoolFile = false;
  private boolean checkIfPreSchool = false;  

//The columns in the file are in this order
//first the regual school or preschool

//Område Area?
//Enhet	Schoolname
//EnhAdr address
//EnhOrt Area postalcode name
//EnhPostnr	postalcode number
//EnhTele	phone
//
//caretaker preschool stuff
//DBV	caretakername  (preschool)
//DBVAdr	address
//DBVOrt	postalarea
//DBVPnr	social security number
//DBVPostnr	postal number
//DBVTele   phone

  private final int COLUMN_SCHOOL_AREA = 0;  
  private final int COLUMN_SCHOOL_NAME = 1;
  private final int COLUMN_SCHOOL_ADDRESS = 2; 
  private final int COLUMN_SCHOOL_POSTAL_NAME = 3; 
  private final int COLUMN_SCHOOL_POSTAL_CODE = 4;  
  private final int COLUMN_SCHOOL_PHONE_NUMBER = 5;
  
  private final int COLUMN_CARETAKER_NAME = 6; 
  private final int COLUMN_CARETAKER_ADDRESS = 7; 
  private final int COLUMN_CARETAKER_POSTAL_NAME = 8; 
  private final int COLUMN_CARETAKER_PIN = 9; 
  private final int COLUMN_CARETAKER_POSTAL_CODE = 10;  
  private final int COLUMN_CARETAKER_PHONE = 11;
  	
  public NackaSchoolImportFileHandlerBean(){}
  
  public boolean handleRecords() throws RemoteException{
    transaction =  this.getSessionContext().getUserTransaction();
    
    Timer clock = new Timer();
    clock.start();

    try {
      //initialize business beans and data homes           
      schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
      sHome = schoolBiz.getSchoolHome();
      
      //if the transaction failes all the users and their relations are removed
      transaction.begin();

      //iterate through the records and process them
      String item;

      int count = 0;
      while ( !(item=(String)file.getNextRecord()).equals("") ) {
        count++;
        		
        if( ! processRecord(item) ) failedRecords.add(item);

        if( (count % 500) == 0 ){
          System.out.println("NackaSchoolHandler processing RECORD ["+count+"] time: "+IWTimestamp.getTimestampRightNow().toString());
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
    schoolValues = file.getValuesFromRecordString(record);
    //System.out.println("THE RECORD = "+record);
    
	boolean success = storeSchoolInfo();
  
    schoolValues = null;

    return success;
  }
  
  public void printFailedRecords(){
  	System.out.println("Import failed for these records, please fix and import again:");
  
  	Iterator iter = failedRecords.iterator();
  	while (iter.hasNext()) {
		System.out.println((String) iter.next());
	}
  }

  protected boolean storeSchoolInfo() throws RemoteException{

    School school = null;

//variables
    String schoolName = getProperty(this.COLUMN_SCHOOL_NAME);
    String caretakerName = getProperty(COLUMN_CARETAKER_NAME);
    
    
    
    
    if(schoolName == null ) return false;


	if( checkIfPreSchool ){
		if(caretakerName!=null) isPreSchoolFile = true;
		
		checkIfPreSchool = false;	
	}
    
	//database stuff
	try{
		//school
		//this can only work if there is only one school with this name. add more parameters for other areas
		school = (School) (sHome.findAllBySchoolName(schoolName).iterator().next());
		if( school == null ) return false;
	}
	catch (FinderException e) {
		System.out.println("School not found creating : "+schoolName);	
		school = schoolBiz.createSchool(schoolName,"","","","",-1);
		return true;
	}		

    //finished with this user
    school = null;
    
    return true;
  }

  public void setImportFile(ImportFile file){
    this.file = file;
  }

	private String getProperty(int columnIndex){
		String value = null;
		
		if( schoolValues!=null ){
		
			try {
				value = (String)schoolValues.get(columnIndex);
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

/**
 * @see com.idega.block.importer.business.ImportFileHandler#getFailedRecords()
 */
public List getFailedRecords(){
	return failedRecords;	
}

  }