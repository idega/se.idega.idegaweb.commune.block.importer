package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;
import javax.ejb.*;

public interface NackaImportFileHandler extends com.idega.business.IBOService,se.idega.idegaweb.commune.block.importer.business.ImportFileHandler
{
  public void setOnlyImportRelations(boolean onlyImportRelations) throws RemoteException;
}
