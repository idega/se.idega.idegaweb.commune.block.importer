package se.idega.idegaweb.commune.block.importer.business;
import is.idega.idegaweb.member.business.MemberFamilyLogic;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import se.idega.idegaweb.commune.business.CommuneUserBusiness;

import com.idega.block.importer.data.ImportFile;
import com.idega.business.IBOLookup;
import com.idega.business.IBOServiceBean;
import com.idega.core.business.AddressBusiness;
import com.idega.core.data.Address;
import com.idega.core.data.AddressHome; 
import com.idega.core.data.AddressType;
import com.idega.core.data.Country;
import com.idega.core.data.CountryHome;
import com.idega.core.data.PostalCode;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.Gender;
import com.idega.user.data.GenderHome;
import com.idega.user.data.Group;
import com.idega.user.data.GroupHome;
import com.idega.user.data.User;
import com.idega.user.data.UserHome;
import com.idega.util.IWTimestamp;
import com.idega.util.text.TextSoap;
import com.idega.util.Timer;

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
  private AddressBusiness addressBiz;
  private MemberFamilyLogic relationBiz;
  private CommuneUserBusiness comUserBiz;
  private GroupHome groupHome;
  private Group nackaGroup;
  private ImportFile file;
  private UserTransaction transaction;
  private UserTransaction transaction2;

  private boolean importUsers = true;
  private boolean importAddresses = true;
  private boolean importRelations = true;

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

  private Gender male;
  private Gender female;

  //not needed..yet?
  /*private final String USER_SECTION_STARTS = "01001";
  private final String USER_SECTION_ENDS = RELATIONAL_SECTION_STARTS;
  private final String IMMIGRATION_SECTION_STARTS = "05001";
  private final String IMMIGRATION_SECTION_ENDS = SPECIALCASE_RELATIONAL_SECTION_STARTS;
  private final String OTHER_ADDRESSES_SECTION_STARTS = "07001";*/

  public boolean handleRecords() throws RemoteException{
  	
  	      /**@todo temporary workaround**/
      //((NackaImportFileHandler)handler).setOnlyImportRelations(true);
      //((NackaImportFileHandler)handler).setStartRecord(52000);
      //((NackaImportFileHandler)handler).setImportRelations(false);
     // status = handler.handleRecords();

    //transaction =  this.getSessionContext().getUserTransaction();
    transaction2 =  this.getSessionContext().getUserTransaction();

    Timer clock = new Timer();
    clock.start();

    try {
      //initialize business beans and data homes
      biz = (UserBusiness) this.getServiceInstance(UserBusiness.class);
      relationBiz = (MemberFamilyLogic) this.getServiceInstance(MemberFamilyLogic.class);
      home = biz.getUserHome();
      addressBiz = (AddressBusiness) this.getServiceInstance(AddressBusiness.class);
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
          System.out.println("NackaImportFileHandler processing RECORD ["+count+"] time: "+IWTimestamp.getTimestampRightNow().toString());
        }
        item = null;
      }

      clock.stop();
      System.out.println("Time to handleRecords: "+clock.getTime()+" ms  OR "+((int)(clock.getTime()/1000))+" s");

      // System.gc();
      //success commit changes
      //transaction.commit();


      //store family relations
      if( importRelations){
        storeRelations();
      }

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

    if( importUsers || importAddresses ){
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
    else if( !importUsers && !importAddresses && importRelations){//only store relations
      //the rest e.g. User info and immigration stuff
      userPropertiesMap.putAll(getPropertiesMapFromString(record," "));//PIN number etc.
      //Family relations
      userPropertiesMap.put(RELATIONAL_SECTION_STARTS,getArrayListWithMapsFromStringFragment(record,RELATIONAL_SECTION_STARTS,RELATIONAL_SECTION_ENDS) );
    }

    /**
    * family and other releation stuff
    */
    if( importRelations ) addRelations();


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
    String firstName = getUserProperty("01012","");
    String middleName = getUserProperty("01013","");
    String lastName = getUserProperty("01014","");
    String PIN = getUserProperty("01001");

    Gender gender = getGenderFromPin(PIN);
    IWTimestamp dateOfBirth = getBirthDateFromPin(PIN);

    /**
    * basic user info
    */
    try{
      //System.err.println(firstName);
      user = comUserBiz.createCitizenByPersonalIDIfDoesNotExist(firstName,middleName,lastName,PIN, gender, dateOfBirth);
    }
    catch(Exception e){
      e.printStackTrace();
      return false;
    }

    /**
     * addresses
     */
    //main address
    //country id 187 name Sweden isoabr: SE

      String addressLine = getUserProperty("01033");
      if( (addressLine!=null) && importAddresses ){
        try{

        String streetName = addressBiz.getStreetNameFromAddressString(addressLine);
        String streetNumber = addressBiz.getStreetNumberFromAddressString(addressLine);
        String postalCode = getUserProperty("01034");
        String postalName = getUserProperty("01035");

        Address address = biz.getUsersMainAddress(user);
        Country sweden = ((CountryHome)getIDOHome(Country.class)).findByIsoAbbreviation("SE");
        PostalCode code = addressBiz.getPostalCodeAndCreateIfDoesNotExist(postalCode,postalName,sweden);

        boolean addAddress = false;/**@todo is this necessary?**/

        if( address == null ){
          AddressHome addressHome = addressBiz.getAddressHome();
          address = addressHome.create();
          AddressType mainAddressType = addressHome.getAddressType1();
          address.setAddressType(mainAddressType);
          addAddress = true;
        }

        address.setCountry(sweden);
        address.setPostalCode(code);
        address.setProvince("Nacka");
        address.setCity("Stockholm");
        address.setStreetName(streetName);
        address.setStreetNumber(streetNumber);

        address.store();

        if(addAddress){
          user.addAddress(address);
        }

    }
     catch(Exception e){
      e.printStackTrace();
      return false;
    }

    }



    //extra address
    //special address
    //special extra address
    //foreign adress
    //previous address

    /**
     * citizen info (commune stuff)
     * longitude/lattitude
     */
     String longAndLat = getUserProperty("01025");



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
    ArrayList errors = new ArrayList();

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
            System.out.println("NackaImportFileHandler storing relations ["+count+"] time: "+IWTimestamp.getTimestampRightNow().toString());
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

                    if( relationType != null ){
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
                    }
                    else{
                      errors.add("NackaImporter: Error Relation type not defined for relative ( pin "+relativePIN+" ) of user: "+PIN);
                      //System.out.println("NackaImporter: Error Relation type not defined for relative ( pin "+relativePIN+" ) of user: "+PIN);
                    }

                      //other types
                  }
                  catch (CreateException ex) {
                    errors.add("NackaImporter : Error adding relation for user: "+PIN);
                    //System.out.println("NackaImporter : Error adding relation for user: "+PIN);
                    //ex.printStackTrace();
                  }
                  catch (FinderException ex) {
                    errors.add("NackaImporter : Error relative (pin "+relativePIN+") not found in database for user: "+PIN);
                    //System.out.println("NackaImporter : Error relative (pin "+relativePIN+") not found in database for user: "+PIN);
                    //ex.printStackTrace();
                  }
                }//if relativepin !=null
                else{
                  errors.add("NackaImporter : Error relative has no PIN and skipping for parent user: "+PIN);
                  //System.out.println("NackaImporter : Error relative has no PIN and skipping for parent user: "+PIN);
                }
              }//end while iter2
            }//end if relative
          }//end while iter

          //success commit
          transaction2.commit();

          Iterator err = errors.iterator();
          while (err.hasNext()) {
            String item = (String) err.next();
            System.out.println(item);
          }

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
    setImportRelations(true);
    setImportUsers(false);
    setImportAddresses(false);
  }

  public void setImportRelations(boolean importRelations){
    this.importRelations = importRelations;
  }

  public void setImportUsers(boolean importUsers){
    this.importUsers = importUsers;
  }

  public void setImportAddresses(boolean importAddresses){
    this.importAddresses = importAddresses;
  }

  public void setStartRecord(int startRecord){
    this.startRecord = startRecord;
  }

  private Gender getGenderFromPin(String pin){
    //pin format = 190010221208 second last number is the gender
    //even number = female
    //odd number = male
    try {
      GenderHome home = (GenderHome) this.getIDOHome(Gender.class);
      if( Integer.parseInt(pin.substring(10,11)) % 2 == 0 ){
        if( female == null ){
          female = home.getFemaleGender();
        }
        return female;
      }
      else{
        if( male == null ){
          male = home.getMaleGender();
        }
        return male;
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      return null;//if something happened
    }
  }

  private IWTimestamp getBirthDateFromPin(String pin){
    //pin format = 190010221208 yyyymmddxxxx
    int dd = Integer.parseInt(pin.substring(6,8));
    int mm = Integer.parseInt(pin.substring(4,6));
    int yyyy = Integer.parseInt(pin.substring(0,4));
    IWTimestamp dob = new IWTimestamp(dd,mm,yyyy);
    return dob;
  }
  
  public void setRootGroup(Group group){}
  

  }