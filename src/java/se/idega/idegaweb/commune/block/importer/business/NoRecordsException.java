package se.idega.idegaweb.commune.block.importer.business;
import com.idega.core.data.ICFileType;

/**
 * Title: se.idega.idegaweb.commune.block.importer.business.NoRecordsException
 * Description: This is the exception that is thrown when no record is found in an ImportFile
 * Copyright:    Copyright (c) 2002
 * Company:      idega software
 * @author <a href="mailto:eiki@idega.is">Eirikur S. Hrafnsson</a>
 * @version 1.0
 */

public class NoRecordsException extends RuntimeException {

  public NoRecordsException() {
    super();
  }

  public NoRecordsException(String explanation){
    super(explanation);
  }

}
