package se.idega.idegaweb.commune.block.importer.business;
import java.io.LineNumberReader;
import com.idega.util.text.TextSoap;
import java.util.*;
import java.io.*;

/**
 * <p>Title: NackaImportFileHandler</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is"> Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */



public class NackaImportFileHandler implements ImportFileHandler{

  private Map propertiesMap;

  private final String RELATIONAL_SECTION_STARTS = "02000";
  private final String RELATIONAL_SECTION_ENDS = "02999";
  private final String CITIZEN_INFO_SECTION_STARTS = "03000";
  private final String CITIZEN_INFO_SECTION_ENDS = "03999";
  private final String HISTORIC_SECTION_STARTS = "04000";
  private final String HISTORIC_SECTION_ENDS = "04999";
  private final String SPECIALCASE_RELATIONAL_SECTION_STARTS = "06000";
  private final String SPECIALCASE_RELATIONAL_SECTION_ENDS = "06999";

  //not needed..yet?
  /*private final String USER_SECTION_STARTS = "01001";
  private final String USER_SECTION_ENDS = RELATIONAL_SECTION_STARTS;
  private final String IMMIGRATION_SECTION_STARTS = "05001";
  private final String IMMIGRATION_SECTION_ENDS = SPECIALCASE_RELATIONAL_SECTION_STARTS;*/



  private final String OTHER_ADDRESSES_SECTION_STARTS = "07001";

  public boolean handleRecords(Collection records){


    Timer clock = new Timer();
    clock.start();

    try {
      Iterator iter = records.iterator();
      int count = 0;
      while (iter.hasNext()) {
        count++;
        String item = (String) iter.next();
        processRecord(item);

        //if( this.importAtATimeLimit >= count ){
        if( (count % 1000) == 0 ){
          System.out.println("NackaImportFileHandler processing RECORD ["+count+"]");
        }
        //}
      }

      records = null;
      clock.stop();
      System.out.println("Time to handleRecords: "+clock.getTime()+" ms  OR "+((int)(clock.getTime()/1000))+" s");
     // System.gc();

      return true;
    }
    catch (Exception ex) {
     ex.printStackTrace();
     return false;
    }


  }

  private boolean processRecord(String record){
    propertiesMap = new HashMap();

    record = TextSoap.findAndCut(record,"#UP ");

    //Family relations
    propertiesMap.put(RELATIONAL_SECTION_STARTS,getArrayListMapFromStringFragment(record,RELATIONAL_SECTION_STARTS,RELATIONAL_SECTION_ENDS) );
    //Special case relations
    propertiesMap.put(SPECIALCASE_RELATIONAL_SECTION_STARTS, getArrayListMapFromStringFragment(record,SPECIALCASE_RELATIONAL_SECTION_STARTS,SPECIALCASE_RELATIONAL_SECTION_ENDS) );
    //Citizen info
    propertiesMap.put(CITIZEN_INFO_SECTION_STARTS,getArrayListMapFromStringFragment(record,CITIZEN_INFO_SECTION_STARTS,CITIZEN_INFO_SECTION_ENDS) );
    //Historic info
    propertiesMap.put(HISTORIC_SECTION_STARTS,getArrayListMapFromStringFragment(record,HISTORIC_SECTION_STARTS,HISTORIC_SECTION_ENDS) );

    //the rest e.g. User info and immigration stuff
    propertiesMap.putAll(getPropertiesMapFromString(record," "));

    /**@todo
     *  do database stuff for this user
     */

    propertiesMap = null;

    return true;
  }

  protected ArrayList getArrayListMapFromStringFragment(String record, String fragmentStart, String fragmentEnd){
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
    HashMap map = new HashMap();
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

  public void storeUserInfo(Map userMap){
    //user info

    //address
    //main address
    //extra address
    //special address
    //special extra address
    //foreign adress
    //previous address


    //citizen info (commune stuff)

    //family and other releation stuff

  }

  }