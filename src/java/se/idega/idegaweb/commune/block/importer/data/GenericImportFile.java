package se.idega.idegaweb.commune.block.importer.data;

import java.util.Collection;
import java.io.File;

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

  public GenericImportFile(File file) {
    this.file = file;
  }

 // public Collection getRecords(){return null;}
 // public Object getRecordAtIndex(int i){return null;}
  public boolean parse(){return false;}

  protected File getFile(){
    return file;
  }

}