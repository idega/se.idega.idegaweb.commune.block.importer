package se.idega.idegaweb.commune.block.importer.data;

import java.io.*;
import java.util.Collection;

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
        while ( br.readLine() != null){
          cnt++;
        }

        br.close();
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