package se.idega.idegaweb.commune.block.importer.data;

import java.io.*;
import java.util.*;

import com.idega.util.text.TextSoap;

/**
 * <p>Title: IdegaWeb classes</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is"> Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */

public class NackaImportFile extends GenericImportFile implements ImportFile{
  private int importAtATimeLimit = 5;
  private final String DILIMITER = "#POST_SLUT";

  public NackaImportFile(File file) {
    super(file);
  }

 // public Collection getRecords(){return null;}
 // public Object getRecordAtIndex(int i){return null;}
  public boolean parse(){
    try{
        FileReader fr = new FileReader(getFile());
        BufferedReader br = new BufferedReader(fr);
        String line;
        StringBuffer buf = new StringBuffer();
        ArrayList list = new ArrayList();

        int cnt = 0;

        while ( (line=br.readLine()) != null){
          if( buf == null ){
            buf = new StringBuffer();
          }

          buf.append(line);

          if( line.indexOf(DILIMITER)!= -1 ){
           list.add(buf.toString());
           buf = null;
          }

          cnt++;
        }

        System.out.println("Number of Lines: "+cnt);
        System.out.println("Number of records = "+list.size());
        processRecords(list);

        br.close();
        fr = null;
        br = null;

        //Vector users = TextSoap.FindAllBetween(buf.toString(),"#POST_START","#POST_SLUT");
        System.gc();


    }
    catch( FileNotFoundException ex ){
      ex.printStackTrace(System.err);
      return false;
    }
    catch( IOException ex ){
      ex.printStackTrace(System.err);
      return false;
    }

    return true;
  }


  private void processRecords(ArrayList list){

    Iterator iter = list.iterator();
    int count = 0;
    while (iter.hasNext()) {
      count++;
      String item = (String) iter.next();
      if( this.importAtATimeLimit >= count ){
        System.out.println("RECORD ["+count+"] \n"+item);
      }
    }

    list = null;
    System.gc();
  }



}