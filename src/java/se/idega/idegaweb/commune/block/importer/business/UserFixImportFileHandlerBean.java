package se.idega.idegaweb.commune.block.importer.business;
import java.util.ArrayList;
import java.util.List;

import se.idega.idegaweb.commune.business.CommuneUserBusiness;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.business.IBOServiceBean;
import com.idega.user.data.Group;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.Timer;


/**
 * <p>Description: Emergency fixer for Nacka. Takes a file with two columns USERID;PIN and fixes the personalid's and dates of birth in the database</p>
 * <p>Copyright (c) 2003</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is"> Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */



public class UserFixImportFileHandlerBean extends IBOServiceBean implements UserFixImportFileHandler,ImportFileHandler{

  
	private ImportFile file;
	private ArrayList failedRecords;
	private CommuneUserBusiness comUserBiz;

  
  public UserFixImportFileHandlerBean(){}

  public boolean handleRecords(){
  
    Timer clock = new Timer();
    clock.start();

    try {
      //initialize business beans and data homes
      this.comUserBiz = (CommuneUserBusiness) this.getServiceInstance(CommuneUserBusiness.class);
  
			this.failedRecords = new ArrayList();

			System.out.println("Userfix [STARTING] time: "+IWTimestamp.getTimestampRightNow().toString());
       
	
      String item;

      int count = 0;
      while ( !(item=(String)this.file.getNextRecord()).equals("") ) {
        count++;

        
        if( ! processRecord(item) ) {
					this.failedRecords.add(item);
				}
        

        if( (count % 250) == 0 ){
          System.out.println("Userfix processing RECORD ["+count+"] time: "+IWTimestamp.getTimestampRightNow().toString());
        }
        item = null;
      }

      clock.stop();
      System.out.println("Time to handleRecords: "+clock.getTime()+" ms  OR "+((int)(clock.getTime()/1000)));


      return true;
    }
    catch (Exception ex) {
     ex.printStackTrace();
     return false;
    }

  }

  private boolean processRecord(String record){
  	ArrayList list = this.file.getValuesFromRecordString(record);
    return storeUserInfo(list);
  }

 
 

  protected boolean storeUserInfo(ArrayList list){

    try {
			User user = null;
			
			if( !list.isEmpty()){
				int userId = Integer.parseInt((String)list.get(0));
				
				String PIN = "";
				IWTimestamp dateOfBirth = null;
				if(list.size()>=2){
					PIN = (String) list.get(1);
					dateOfBirth = getBirthDateFromPin(PIN);
				}
				
				
				//System.out.println("userid : "+userId+" personalId : "+PIN);
				
				
				user = this.comUserBiz.getUser(userId);
				
				user.setPersonalID(PIN);
				
				if(dateOfBirth!=null){
					user.setDateOfBirth(dateOfBirth.getDate());
				}
				else {
					user.setDateOfBirth(null);
				}

				/**
				 * Save the user to the database
				 */
				user.store();
				
			}
			

			
			//finished with this user
			user = null;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
    return true;
  }



  public void setImportFile(ImportFile file){
    this.file = file;
  }
  
  private IWTimestamp getBirthDateFromPin(String pin){
    //pin format = 190010221208 yyyymmddxxxx
    int dd = Integer.parseInt(pin.substring(6,8));
    int mm = Integer.parseInt(pin.substring(4,6));
    int yyyy = Integer.parseInt(pin.substring(0,4));
    IWTimestamp dob = new IWTimestamp(dd,mm,yyyy);
    return dob;
  }
  
/**
 * @see com.idega.block.importer.business.ImportFileHandler#setRootGroup(Group)
 */
  public void setRootGroup(Group group){}
  
  
	  /**
	 * @see com.idega.block.importer.business.ImportFileHandler#getFailedRecords()
	 */
	public List getFailedRecords(){
		return this.failedRecords;	
	}
	

}