package se.idega.idegaweb.commune.block.importer.data;

import se.idega.idegaweb.commune.block.importer.business.*;
import java.util.Collection;
import java.util.ArrayList;

import java.io.*;

/**
 * <p>Title: IdegaWeb classes</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is"> Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */

public class GenericImportFile implements ImportFile{

  File file;
  private String recordDilimiter = "\n";

  public GenericImportFile(File file) {
    this.file = file;
  }

  /**
  *
  * @return the String value of recordDilimiter.
  */
  public String getRecordDilimiter(){
        return recordDilimiter;
  }

  /**
  *
  * @param aRecordDilimiter - the new value for recordDilimiter
  */
  public void setRecordDilimiter(String aRecordDilimiter){
        recordDilimiter = aRecordDilimiter;
  }


 // public Collection getRecords(){return null;}
 // public Object getRecordAtIndex(int i){return null;}

 /** This method parses the file into records (ArrayList) and returns the complete list.<p>
  *  it throws a NoRecordsFoundException if no records where found.
  */
  public Collection getRecords() throws NoRecordsException{

    try{
      FileReader fr = new FileReader(getFile());
      BufferedReader br = new BufferedReader(fr);
      String line;
      StringBuffer buf = new StringBuffer();
      ArrayList list = new ArrayList();

      int cnt = 0;
      int records = 0;

      Timer clock = new Timer();
      clock.start();

      while ( (line=br.readLine()) != null){
        if( buf == null ){
          buf = new StringBuffer();
        }

        buf.append(line);

        /**@todo this should be an option with a setMethod?**/
        buf.append('\n');

        if( line.indexOf(getRecordDilimiter())!= -1 ){
          records++;
          if( (records % 1000) == 0 ){
            System.out.println("Importer: Reading record nr.: "+records+" from file "+getFile().getName());
          }

         list.add(buf.toString());
         buf = null;
        }

        cnt++;
      }

      line = null;
      buf = null;

      br.close();
      fr = null;
      br = null;

      clock.stop();

      if( records == 0 ){
       throw new NoRecordsException("No records where found in the selected file"+file.getAbsolutePath());
      }

      //System.gc();

      System.out.println("Time for operation: "+clock.getTime()+" ms  OR "+((int)(clock.getTime()/1000))+" s");
      System.out.println("Number of Lines: "+cnt);
      System.out.println("Number of records = "+records);

      return list;
    }
    catch( FileNotFoundException ex ){
      ex.printStackTrace(System.err);
      return null;
    }
    catch( IOException ex ){
      ex.printStackTrace(System.err);
      return null;
    }

  }

  protected File getFile(){
    return file;
  }

}