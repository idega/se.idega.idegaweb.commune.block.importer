package se.idega.idegaweb.commune.block.importer.data;

import java.util.Collection;
import java.util.ArrayList;
import java.io.*;
import se.idega.idegaweb.commune.block.importer.business.*;
import com.idega.util.text.TextSoap;

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
  }

}