package se.idega.idegaweb.commune.block.importer.business;

import com.idega.util.text.TextSoap;
import com.idega.business.IBOLookup;
import java.rmi.RemoteException;
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

  public ImportFileHandler getHandlerForImportFile(Class importFileClass) throws RemoteException{
    return (ImportFileHandler)getServiceInstance(importFileClass);
  }

  public ImportFileHandler getHandlerForImportFile(String importFileClassName)  throws RemoteException, ClassNotFoundException{
    return getHandlerForImportFile(Class.forName( TextSoap.findAndReplace(importFileClassName,".data.",".business.")+"Handler"));
  }

  public boolean importRecords(ImportFile file) throws RemoteException{
    try{
      boolean status = false;
      ImportFileHandler handler = getHandlerForImportFile(file.getClass().getName());
      handler.setImportFile(file);
      /**@todo temporary workaround**/
      //((NackaImportFileHandler)handler).setOnlyImportRelations(true);
      //((NackaImportFileHandler)handler).setStartRecord(52000);
      ((NackaImportFileHandler)handler).setImportRelations(false);
      status = handler.handleRecords();

      /*Collection col = file.getRecords();
      if( col == null ) return false;
      status = handler.handleRecords(col);*/



      return status;
    }
    catch(NoRecordsException ex){
     ex.printStackTrace();
     return false;
    }
    catch(ClassNotFoundException ex){
     ex.printStackTrace();
     return false;
    }
  }

}