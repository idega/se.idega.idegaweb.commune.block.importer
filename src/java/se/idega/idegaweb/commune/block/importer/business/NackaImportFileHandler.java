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

  private Map propertiesMap,multiValuePropertiesMap;

  private final String RELATIONAL_SECTION_STARTS = "02000";
  private final String RELATIONAL_SECTION_ENDS = "02999";

  private final String USER_SECTION_STARTS = "01001";
  private final String USER_SECTION_ENDS = RELATIONAL_SECTION_STARTS;

  private final String CITIZEN_INFO_SECTION_STARTS = "03000";
  private final String CITIZEN_INFO_SECTION_ENDS = "03999";

  private final String HISTORIC_SECTION_STARTS = "04000";
  private final String HISTORIC_SECTION_ENDS = "04999";

  private final String SPECIALCASE_RELATIONAL_SECTION_STARTS = "06000";
  private final String SPECIALCASE_RELATIONAL_SECTION_ENDS = "06999";

  private final String IMMIGRATION_SECTION_STARTS = "05001";
  private final String IMMIGRATION_SECTION_ENDS = SPECIALCASE_RELATIONAL_SECTION_STARTS;



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
    record = TextSoap.findAndCut(record,"#UP ");



    propertiesMap = getPropertiesMapFromString(record," ");


/*
    RELATIONAL_SECTION_STARTS.la
    CITIZEN_INFO_SECTION_STARTS
    HISTORIC_SECTION_STARTS
    IMMIGRATION_SECTION_STARTS
    SPECIALCASE_RELATIONAL_SECTION_STARTS
    OTHER_ADDRESSES_SECTION_STARTS
*/


    return true;
  }

  protected Map getPropertiesMapFromString(String propsString, String seperator){
    HashMap map = new HashMap();
    LineNumberReader reader = new LineNumberReader(new StringReader(propsString));

    String line = null;
    int index = -1;
    String property = null;
    String value = null;

    try{

      while( (line = reader.readLine()) != null ){

        if( (index = line.indexOf(seperator)) != -1 ){
          property = line.substring(0,index);
          value = line.substring(index+1,line.length());
          map.put( property, value);
          //System.out.println("Param:"+property+" Value:"+value);
        }
      }

    }
    catch (Exception ex) {
      ex.printStackTrace();
    }




    return map;
  }

  private String addToMultyPropertiesMapAndCutFragment(String record, String start, String end){
    int startIndex = record.indexOf(start);
    int endIndex = record.lastIndexOf(end);

    String fragment = null;
    if( (startIndex != -1) && (endIndex != -1) ){

      StringBuffer buf = new StringBuffer();

    }


    return record;

  }

  }