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

  public NackaImportFile(File file) {
    super(file);
  }

 // public Collection getRecords(){return null;}
 // public Object getRecordAtIndex(int i){return null;}
  public boolean parse(){
    try{
        FileReader fr = new FileReader(getFile());
        BufferedReader br = new BufferedReader(fr);
        int cnt = 0;
        String line;
        StringBuffer buf = new StringBuffer();
        while ( (line=br.readLine()) != null){
          buf.append(line);
          cnt++;
        }
        br.close();
        fr = null;
        br = null;

        Vector users = TextSoap.FindAllBetween(buf.toString(),"#POST_START","#POST_SLUT");
        buf = null;

        System.gc();

        int size = users.size();
        for (int i = 0; i < 4; i++) {
          System.out.println( (String) users.elementAt(i));
          users.removeElementAt(i);
          //users.trimToSize();

        }

        users = null;
        System.gc();
        System.out.println("Number of Lines: "+cnt);
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
}