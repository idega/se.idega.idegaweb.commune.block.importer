package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.block.school.business.SchoolBusiness;
import com.idega.block.school.data.School;
import com.idega.block.school.data.SchoolArea;
import com.idega.block.school.data.SchoolHome;
import com.idega.block.school.data.SchoolType;
import com.idega.block.school.data.SchoolTypeHome;
import com.idega.business.IBORuntimeException;
import com.idega.business.IBOServiceBean;
import com.idega.data.IDORelationshipException;
import com.idega.user.data.Group;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;


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
  
  private boolean isPreSchoolFile = true;
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
  //private final int COLUMN_CARETAKER_PIN = 9; 
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
			
			//preSchoolOnly = schoolBiz.getSchoolTypeHome().findByPrimaryKey(new Integer(1));
			preSchoolOnly = getPreSchoolSchoolType(schoolBiz);
			caretakerPreSchool = getCareTakerSchoolType(schoolBiz);
			regularSchool = getElementarySchoolType(schoolBiz);
			schoolWithPreSchoolClass = getElementarySchoolWithPreschoolClassSchoolType(schoolBiz);
			

      
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

/**
 * @param schoolBiz
 * @return
 */
private SchoolType getElementarySchoolType(SchoolBusiness schoolBiz) {
	String schoolTypeKey="sch_type.school_type_forskoleklass";
	return getSchoolType(schoolBiz,schoolTypeKey,"PreSchoolClass",true);
}

/**
 * @param schoolBiz
 * @return
 */
private SchoolType getElementarySchoolWithPreschoolClassSchoolType(SchoolBusiness schoolBiz) {
	String schoolTypeKey="sch_type.school_type_grundskola";
	return getSchoolType(schoolBiz,schoolTypeKey,"ElementarySchool",true);
}

/**
 * @param schoolBiz
 * @return
 */
private SchoolType getCareTakerSchoolType(SchoolBusiness schoolBiz) {
	String schoolTypeKey="sch_type.school_type_familjedaghem";
	return getSchoolType(schoolBiz,schoolTypeKey,"CareTaker",false);
}

/**
 * @return
 */
private SchoolType getPreSchoolSchoolType(SchoolBusiness schoolBiz) {
	String schoolTypeKey="sch_type.school_type_forskola";
	return getSchoolType(schoolBiz,schoolTypeKey,"PreSchool",false);
}

/**
 * @param childcareCategory if false then the category is set to childcare, else it is set to elementaryschool
* @return A SchoolType and creates it if it does not exist.
*/
private SchoolType getSchoolType(
	SchoolBusiness schoolBiz,
	String schoolTypeKey,
	String SchoolTypeName,
	boolean elementarySchoolCategory) {
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

				if(elementarySchoolCategory){
					type.setSchoolCategory(schoolBiz.getElementarySchoolSchoolCategory());					
				}
				else{
					type.setSchoolCategory(schoolBiz.getChildCareSchoolCategory());
				}
				type.store();
			} catch (Exception e) {
				//e.printStackTrace();
				throw new IBORuntimeException(e);
			}
		}
	} catch (RemoteException re) {
		re.printStackTrace();
	}
	return type;
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
	  //String caretakerPIN = getProperty(COLUMN_CARETAKER_PIN);
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