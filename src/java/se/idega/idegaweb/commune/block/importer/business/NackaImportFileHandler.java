package se.idega.idegaweb.commune.block.importer.business;
import java.util.Iterator;
import java.util.Collection;

/**
 * <p>Title: NackaImportFileHandler</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is"> Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */

public class NackaImportFileHandler implements ImportFileHandler{

  public boolean handleRecords(Collection records){


    Timer clock = new Timer();
    clock.start();

    try {
      Iterator iter = records.iterator();
      int count = 0;
      while (iter.hasNext()) {
        count++;
        String item = (String) iter.next();
        //if( this.importAtATimeLimit >= count ){
        System.out.println("RECORD ["+count+"]");
        //}
      }

      records = null;
      clock.stop();
      System.out.println("Time to handleRecords: "+clock.getTime()+" ms  OR "+((int)(clock.getTime()/1000))+" s");
      return true;
    }
    catch (Exception ex) {
     ex.printStackTrace();
     return false;
    }
    //System.gc();
  }

}
