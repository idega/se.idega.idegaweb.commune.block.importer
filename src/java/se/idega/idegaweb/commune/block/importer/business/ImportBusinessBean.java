package se.idega.idegaweb.commune.block.importer.business;

import java.util.Collection;
import se.idega.idegaweb.commune.block.importer.data.ImportFile;
import com.idega.business.IBOServiceBean;

/**
 * <p>Title: IdegaWeb classes</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is"> Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */

public class ImportBusinessBean extends IBOServiceBean implements ImportBusiness{

  public ImportBusinessBean() {
  }

  public ImportFileHandler getHandlerForImportFile(Class importFileClass){
    /** @todo use reflection to find the right handler => importFile.getClasshName+"Handler" -> instance
     *
     */
    return new NackaImportFileHandler();
  }

  public ImportFileHandler getHandlerForImportFile(String importFileClassName){
    /** @todo use reflection to find the right handler => importFile.getClasshName+"Handler" -> instance
     *
     */
    return new NackaImportFileHandler();
  }

  public boolean importRecords(ImportFile file){
    /** @todo use reflection to find the right handler => importFile.getClasshName+"Handler" -> instance
     *
     */
     if( file == null ){

      System.out.println("FÆL ER NULL!");

     }
    try{
      ImportFileHandler handler = getHandlerForImportFile(file.getClass());

      Collection col = file.getRecords();
      if( col == null ) return false;

      return handler.handleRecords(col);
    }
    catch(NoRecordsException ex){
     ex.printStackTrace();
     return false;
    }
  }


}