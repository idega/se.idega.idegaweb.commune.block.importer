package se.idega.idegaweb.commune.block.importer.business;
import com.idega.util.idegaTimestamp;
import se.idega.idegaweb.commune.business.CommuneUserBusiness;
import javax.transaction.*;
import se.idega.idegaweb.commune.block.importer.data.ImportFile;
import com.idega.user.data.*;
import com.idega.idegaweb.IWApplicationContext;
import javax.ejb.CreateException;
import java.rmi.RemoteException;
import com.idega.business.IBOServiceBean;
import javax.ejb.FinderException;
import is.idega.idegaweb.member.business.MemberFamilyLogic;
import com.idega.user.business.UserBusiness;
import com.idega.presentation.IWContext;
import com.idega.business.IBOLookup;
import com.idega.data.*;
import java.io.LineNumberReader;
import com.idega.util.text.TextSoap;
import java.util.*;
import java.io.*;
import javax.transaction.HeuristicMixedException;

/**
 * <p>Title: NackaImportFileHandlerBean</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is"> Eirikur Sveinn Hrafnsson</a>
 * @version 1.0tran
 */



public class NackaImportFileHandlerBean extends IBOServiceBean implements NackaImportFileHandler{

  private Map userPropertiesMap;
  private Map relationsMap;
  private UserBusiness biz;
  private UserHome home;
  private MemberFamilyLogic relationBiz;
  private CommuneUserBusiness comUserBiz;
  private GroupHome groupHome;
  private Group nackaGroup;
  private ImportFile file;
  private UserTransaction transaction;
  private UserTransaction transaction2;

  private boolean onlyImportRelations = false;
  private int startRecord = 0;

  private final String RELATIONAL_SECTION_STARTS = "02000";
  private final String RELATIONAL_SECTION_ENDS = "02999";
  private final String CITIZEN_INFO_SECTION_STARTS = "03000";
  private final String CITIZEN_INFO_SECTION_ENDS = "03999";
  private final String HISTORIC_SECTION_STARTS = "04000";
  private final String HISTORIC_SECTION_ENDS = "04999";
  private final String SPECIALCASE_RELATIONAL_SECTION_STARTS = "06000";
  private final String SPECIALCASE_RELATIONAL_SECTION_ENDS = "06999";

  private final String RELATION_TYPE_CHILD = "B";
  private final String RELATION_TYPE_SPOUSE = "M";
  private final String RELATION_TYPE_CUSTODY = "VF"; //custody relation (child?)
  private final String RELATION_TYPE_FATHER = "FA";
  private final String RELATION_TYPE_MOTHER = "MO";

  //not needed..yet?
  /*private final String USER_SECTION_STARTS = "01001";
  private final String USER_SECTION_ENDS = RELATIONAL_SECTION_STARTS;
  private final String IMMIGRATION_SECTION_STARTS = "05001";
  private final String IMMIGRATION_SECTION_ENDS = SPECIALCASE_RELATIONAL_SECTION_STARTS;
  private final String OTHER_ADDRESSES_SECTION_STARTS = "07001";*/

  public boolean handleRecords() throws RemoteException{

    //transaction =  this.getSessionContext().getUserTransaction();
    transaction2 =  this.getSessionContext().getUserTransaction();

    Timer clock = new Timer();
    clock.start();

    try {
      //initialize business beans and data homes
      biz = (UserBusiness) this.getServiceInstance(UserBusiness.class);
      relationBiz = (MemberFamilyLogic) this.getServiceInstance(MemberFamilyLogic.class);
      home = biz.getUserHome();
      comUserBiz = (CommuneUserBusiness)IBOLookup.getServiceInstance(this.getIWApplicationContext(),CommuneUserBusiness.class);

      //comUserBiz.getRootCitizenGroup();
      //biz.getUserHome().create();

      //groupHome = biz.getGroupHome();

      //if the transaction failes all the users and their relations are removed
      //transaction.begin();

      //iterate through the records and process them
      String item;

      int count = 0;
      while ( !(item=(String)file.getNextRecord()).equals("") ) {
        count++;

        if( count>startRecord ){
          processRecord(item);
        }

        if( (count % 500) == 0 ){
          System.out.println("NackaImportFileHandler processing RECORD ["+count+"] time: "+idegaTimestamp.getTimestampRightNow().toString());
        }
        item = null;
      }

      clock.stop();
      System.out.println("Time to handleRecords: "+clock.getTime()+" ms  OR "+((int)(clock.getTime()/1000))+" s");

      // System.gc();
      //success commit changes
      //transaction.commit();


      //store family relations
      storeRelations();

      return true;
    }
    catch (Exception ex) {
     ex.printStackTrace();

     /*try {
      transaction.rollback();
     }
     catch (SystemException e) {
       e.printStackTrace();
     }*/

     return false;
    }

  }

  private boolean processRecord(String record) throws RemoteException{
    userPropertiesMap = new HashMap();

    record = TextSoap.findAndCut(record,"#UP ");

    if( !onlyImportRelations ){
       //Family relations
      userPropertiesMap.put(RELATIONAL_SECTION_STARTS,getArrayListWithMapsFromStringFragment(record,RELATIONAL_SECTION_STARTS,RELATIONAL_SECTION_ENDS) );
      //Special case relations
      userPropertiesMap.put(SPECIALCASE_RELATIONAL_SECTION_STARTS, getArrayListWithMapsFromStringFragment(record,SPECIALCASE_RELATIONAL_SECTION_STARTS,SPECIALCASE_RELATIONAL_SECTION_ENDS) );
      //Citizen info
      userPropertiesMap.put(CITIZEN_INFO_SECTION_STARTS,getArrayListWithMapsFromStringFragment(record,CITIZEN_INFO_SECTION_STARTS,CITIZEN_INFO_SECTION_ENDS) );
      //Historic info
      userPropertiesMap.put(HISTORIC_SECTION_STARTS,getArrayListWithMapsFromStringFragment(record,HISTORIC_SECTION_STARTS,HISTORIC_SECTION_ENDS) );

      //the rest e.g. User info and immigration stuff
      userPropertiesMap.putAll(getPropertiesMapFromString(record," "));

      //System.out.println("storeUserInfo");
      storeUserInfo();
    }
    else{//only store relations
      //the rest e.g. User info and immigration stuff
      userPropertiesMap.putAll(getPropertiesMapFromString(record," "));//PIN number etc.
      //Family relations
      userPropertiesMap.put(RELATIONAL_SECTION_STARTS,getArrayListWithMapsFromStringFragment(record,RELATIONAL_SECTION_STARTS,RELATIONAL_SECTION_ENDS) );
    }

    /**
    * family and other releation stuff
    */
    addRelations();


    userPropertiesMap = null;
    record=null;

    return true;
  }

  protected ArrayList getArrayListWithMapsFromStringFragment(String record, String fragmentStart, String fragmentEnd){
    ArrayList list = null;
    String fragment = getAndCutFragmentFromRecord(record,fragmentStart,fragmentEnd);
    if( fragment != null ){

      LineNumberReader reader = new LineNumberReader(new StringReader(fragment));
      list = new ArrayList();
      String line = null;
      StringBuffer buf = null;

      try{
        while( (line = reader.readLine()) != null ){
          if( buf == null ){ buf = new StringBuffer(); }
          buf.append(line);
          buf.append('\n');
          if( line.indexOf(fragmentEnd)!= -1 ){
            list.add( getPropertiesMapFromString(buf.toString()," ") );
            buf = null;
          }
        }

        reader.close();
        reader = null;
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }

    }

    return list;
  }

  protected Map getPropertiesMapFromString(String propsString, String seperator){
    Map map = new HashMap();
    String line = null;
    String property = null;
    String value = null;
    int index = -1;

    LineNumberReader reader = new LineNumberReader(new StringReader(propsString));

    try{

      while( (line = reader.readLine()) != null ){

        if( (index = line.indexOf(seperator)) != -1 ){
          property = line.substring(0,index);
          value = line.substring(index+1,line.length());
          map.put( property, value);
          //System.out.println("Param:"+property+" Value:"+value);
        }
      }

      reader.close();
      reader = null;
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    return map;
  }

  private String getAndCutFragmentFromRecord(String record, String start, String end){
    int startIndex = record.indexOf(start);
    int endIndex = record.lastIndexOf(end);

    String fragment = null;
    if( (startIndex != -1) && (endIndex != -1) ){
      StringBuffer buf = new StringBuffer();
      buf.append(record.substring(0,startIndex));
      buf.append(record.substring(endIndex,record.length()));
      fragment = record.substring(startIndex,endIndex+end.length());
      record = buf.toString();
    }

    return fragment;
  }

  protected boolean storeUserInfo() throws RemoteException{

    User user = null;

    //variables
    String firstName = getUserProperty("01012");
    String middleName = getUserProperty("01013");
    String lastName = getUserProperty("01014");
    String PIN = getUserProperty("01001");

    /**
    * basic user info
    */
    try{
      //System.err.println(firstName);
      user = comUserBiz.createCitizenByPersonalIDIfDoesNotExist(firstName,middleName,lastName,PIN);
    }
    catch(Exception e){
      e.printStackTrace();
      return false;
    }

    /**
     * addresses
     */
    //main address
    //extra address
    //special address
    //special extra address
    //foreign adress
    //previous address

    /**
     * citizen info (commune stuff)
     */


    /**
     * Save the user to the database
     */
    //user.store();

    /**
    * Main group relation
    * add to the Nacka root group
    */
    //nackaGroup.addUser(user);

    //finished with this user
    user = null;
    return true;
  }

  protected void addRelations(){
    if( relationsMap == null ) relationsMap = new HashMap();
    ArrayList relatives = (ArrayList)userPropertiesMap.get(RELATIONAL_SECTION_STARTS);
    relationsMap.put(getUserProperty("01001"),relatives);
  }

  protected void storeRelations() throws RemoteException{

   //get keys <- pins
  //get user bean
  //get relative bean
  //if found link with RelationBusiness
  //else skip relative and log somewhere

    Timer clock = new Timer();
    clock.start();
    int count = 0;

    if( relationsMap != null ){
      Iterator iter = relationsMap.keySet().iterator();
      User user;
      User relative;
      String relativePIN;
      String PIN="";
      String relationType;

      try {
        //begin transaction
        transaction2.begin();

        while (iter.hasNext()) {
          ++count;
          if( (count % 500) == 0 ){
            System.out.println("NackaImportFileHandler storing relations ["+count+"] time: "+idegaTimestamp.getTimestampRightNow().toString());
          }

          PIN = (String) iter.next();
          user = null;
          /**@todo
           * IS THE LIST EVER NULL?
           */
            ArrayList relatives = (ArrayList) relationsMap.get(PIN);
            if(relatives!=null){
              user = home.findByPersonalID(PIN);

              Iterator iter2 = relatives.iterator();
              while (iter2.hasNext()) {
                Map relativeMap = (Map) iter2.next();
                relativePIN = (String) relativeMap.get("02001");
                relationType = (String) relativeMap.get("02003");

                /**
                 * @todo use this second parameter if first is missing??? ask kjell
                 */
                //if( relativePIN == null ) relativePIN = (String) item.get("02002"));

                if( relativePIN !=null ){
                  try {
                    relative = home.findByPersonalID(relativePIN);

                    if( relationType.equals(this.RELATION_TYPE_CHILD) ){
                      relationBiz.setAsChildFor(relative,user);
                    }
                    else if( relationType.equals(this.RELATION_TYPE_SPOUSE) ){
                      relationBiz.setAsSpouseFor(relative,user);
                    }
                    else if( relationType.equals(this.RELATION_TYPE_FATHER) ){
                      relationBiz.setAsChildFor(user,relative);
                    }
                    else if( relationType.equals(this.RELATION_TYPE_MOTHER) ){
                      relationBiz.setAsChildFor(user,relative);
                    }
                    else if( relationType.equals(this.RELATION_TYPE_CUSTODY) ){//custody
                    /**
                     * @todo custodian stuff?
                     */
                      relationBiz.setAsChildFor(relative,user);
                    }

                      //other types
                  }
                  catch (CreateException ex) {
                    System.out.println("NackaImporter : Error adding relation for user: "+PIN);
                    //ex.printStackTrace();
                  }
                  catch (FinderException ex) {
                    System.out.println("NackaImporter : Error relative (pin "+relativePIN+") not found in database for user: "+PIN);
                    //ex.printStackTrace();
                  }
                }//if relativepin !=null
                else{
                  System.out.println("NackaImporter : Error relative has no PIN and skipping for parent user: "+PIN);
                }
              }//end while iter2
            }//end if relative
          }//end while iter

          //success commit
          transaction2.commit();

        }
        catch(Exception e){
          if(e instanceof RemoteException){
            throw (RemoteException)e;
          }
          else if(e instanceof FinderException){
            System.out.println("NackaImporter : Error user (pin "+PIN+") not found in database must be an incomplete database");
            System.out.println("NackaImporter : Rollbacking");
            try {
              transaction2.rollback();
            }
            catch (SystemException ec) {
              ec.printStackTrace();
            }
          }
          else{
            e.printStackTrace();
          }
        }
      }//end if relationmap !=null
      else{
        System.out.println("NackaImporter : No relations read");
      }

      clock.stop();
      System.out.println("Time to store relations: "+clock.getTime()+" ms  OR "+((int)(clock.getTime()/1000))+" s");

  }



  protected String getUserProperty(String propertyName){
    return (String) userPropertiesMap.get(propertyName);
  }

  protected String getUserProperty(String propertyName, String StringToReturnIfNotSet){
    String value = getUserProperty(propertyName);
    if(value==null) value = StringToReturnIfNotSet;
    return value;
  }

  public void setImportFile(ImportFile file){
    this.file = file;
  }

  public void setOnlyImportRelations(boolean onlyImportRelations){
    this.onlyImportRelations = onlyImportRelations;
  }

  public void setStartRecord(int startRecord){
    this.startRecord = startRecord;
  }

  }