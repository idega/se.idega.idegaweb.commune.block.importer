package se.idega.idegaweb.commune.block.importer.data;

import java.io.File;
import java.util.ArrayList;

import com.idega.block.importer.data.GenericImportFile;
import com.idega.block.importer.data.ImportFile;

/**
 * <p>Title: IdegaWeb classes</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Idega Software</p>
 * @author <a href="mailto:eiki@idega.is">Eirikur Sveinn Hrafnsson</a>
 * @version 1.0
 */

public class NackaImportFile extends GenericImportFile implements ImportFile{
  private int importAtATimeLimit = 5;
  private ArrayList records;

  public NackaImportFile(File file) {
    super(file);
    setRecordDilimiter("#POST_SLUT");
    setAddNewLineAfterRecord(true);
  }

}