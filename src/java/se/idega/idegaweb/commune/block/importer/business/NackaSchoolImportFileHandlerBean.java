package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;

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
import com.idega.data.IDORelationshipException;
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



public class NackaSchoolImportFileHandlerBean extends IBOServiceBean implements NackaSchoolImportFileHandler,ImportFileHandler{



  private SchoolBusiness schoolBiz;
  private SchoolHome sHome;
    
  private ImportFile file;
  private UserTransaction transaction;
  
  private ArrayList schoolValues;
  private ArrayList failedRecords = new ArrayList();
  
  private boolean isPreSchoolFile = false;
  private boolean checkIfPreSchool = true;  

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
  
  SchoolType preSchoolOnly;
	SchoolType caretakerPreSchool;
	SchoolType regularSchool;
	SchoolType schoolWithPreSchoolClass;
			

  	
  public NackaSchoolImportFileHandlerBean(){}
  
  public synchronized boolean handleRecords() throws RemoteException{
    transaction =  this.getSessionContext().getUserTransaction();
    
    Timer clock = new Timer();
    clock.start();

    try {
      //initialize business beans and data homes           
      schoolBiz = (SchoolBusiness) this.getServiceInstance(SchoolBusiness.class);
      sHome = schoolBiz.getSchoolHome();
			
			preSchoolOnly = schoolBiz.getSchoolTypeHome().findByPrimaryKey(new Integer(1));
			caretakerPreSchool = schoolBiz.getSchoolTypeHome().findByPrimaryKey(new Integer(2));
			regularSchool = schoolBiz.getSchoolTypeHome().findByPrimaryKey(new Integer(4));
			schoolWithPreSchoolClass = schoolBiz.getSchoolTypeHome().findByPrimaryKey(new Integer(5));
			

      
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
      
      checkIfPreSchool = true;
      isPreSchoolFile = false;

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
  
  	String schoolArea = getProperty(COLUMN_SCHOOL_AREA);  
    String schoolName = getProperty(this.COLUMN_SCHOOL_NAME);
	  String schoolAddress = getProperty(COLUMN_SCHOOL_ADDRESS);
	  String schoolPostalName = getProperty(COLUMN_SCHOOL_POSTAL_NAME);
	  String schoolPostalCode= getProperty(COLUMN_SCHOOL_POSTAL_CODE);
	  String schoolPhoneNumber = getProperty(COLUMN_SCHOOL_PHONE_NUMBER);
	  
	  String caretakerName = getProperty(COLUMN_CARETAKER_NAME);
	  String caretakerAddress = getProperty(COLUMN_CARETAKER_ADDRESS);
	  String caretakerPostalName = getProperty(COLUMN_CARETAKER_POSTAL_NAME);
	  String caretakerPIN = getProperty(COLUMN_CARETAKER_PIN);
	  String caretakerPostalCode = getProperty(COLUMN_CARETAKER_POSTAL_CODE);
	  String caretakerPhoneNumber = getProperty(COLUMN_CARETAKER_PHONE);
        
   
		if( checkIfPreSchool ){//only done once, the first row cannot contain anything other than the column names
			if(caretakerName!=null) isPreSchoolFile = true;
			checkIfPreSchool = false;
			return true;
		}
	
    boolean found = false;
    boolean caretaker = (isPreSchoolFile && caretakerName!=null);
    
    if( caretaker ){	
    		schoolName = caretakerName;
    		schoolAddress = caretakerAddress;
    		schoolPostalName = caretakerPostalName;
    		schoolPostalCode = caretakerPostalCode;
    		schoolPhoneNumber = caretakerPhoneNumber;
    }
	
		try{
			//create or find the school and update min data
			//this can only work if there is only one school with this name. add more parameters for other areas
			school = sHome.findBySchoolName(schoolName);
			school.setSchoolAddress(schoolAddress);
			school.setSchoolZipCode(schoolPostalCode);
			school.setSchoolZipArea(schoolPostalName);
			school.setSchoolPhone(schoolPhoneNumber);
			found = true;
		}
		catch (FinderException e) {
			int[] nullType = null;
			System.out.println("School not found creating : "+schoolName);	
			school = schoolBiz.createSchool(schoolName,schoolAddress,schoolPostalCode,schoolPostalName,schoolPhoneNumber,-1,nullType); 
		}
		
		
			
		Collection schoolTypes = null;
		SchoolArea area = null;
				
		
		try {
			schoolTypes = school.getSchoolTypes();
		} catch (IDORelationshipException e) {
		}
						
		//add types and areas
		if( isPreSchoolFile && !caretaker){
			//add type
			if(found){

				if( schoolTypes==null || isPreSchoolOnly(schoolTypes)  ){//check if it has any other types or none
					try{
						school.addSchoolType(preSchoolOnly);
					}
					catch(Exception e){}
				}
				else{
					try{
						school.addSchoolType(schoolWithPreSchoolClass);
					}
					catch(Exception e){}
					
				}	
							
				
			}
			else{
				//add only preschool type 1
				try{
					school.addSchoolType(preSchoolOnly);
				}
				catch(Exception e){}
					
				}	
		}
		else if (caretaker){//familie...
			//add type 2
			try{
				school.addSchoolType(caretakerPreSchool);
			}
			catch(Exception e){}
			
			//todo! register principal
			//caretakerPIN
			
			// user = comUserBiz.createCitizenByPersonalIDIfDoesNotExist(firstName,middleName,lastName,PIN, gender, dateOfBirth);
    
		
			
		}
		else{//regular school
			//add type 4
			//what if the school changes from a preschoolonly to a mixed school?
			if( !isPreSchoolOnly(schoolTypes) ){
				try{
						school.addSchoolType(regularSchool);
				}
				catch(Exception e){}
			}		
		}

		
		if( schoolArea!=null ){
			 try{
			 	area = schoolBiz.getSchoolAreaHome().findSchoolAreaByAreaName(schoolArea);
			 }
			 catch (FinderException e) {
			 	
			 	try {
					System.out.println("SchoolArea not found creating : "+schoolArea);	
					area = schoolBiz.getSchoolAreaHome().create();
					area.setSchoolAreaName(schoolArea);
					area.store();
				}catch (CreateException ex) {
					ex.printStackTrace();
			 	}
			}
			
		}
		//set area
		if( area!=null ){
			school.setSchoolAreaId(((Integer)area.getPrimaryKey()).intValue());
		}
		
		school.store();			
		
    //finished with this school
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

	private boolean isPreSchoolOnly(Collection schoolTypes){
		
		if( schoolTypes!=null && !schoolTypes.isEmpty()){
			Iterator iter = schoolTypes.iterator();
				while (iter.hasNext()) {
					SchoolType element = (SchoolType) iter.next();
					if( element.equals(preSchoolOnly) ){
						return true;	
					}
					else return false;
					
				}
		}
			
		return false;
		
	}

  }