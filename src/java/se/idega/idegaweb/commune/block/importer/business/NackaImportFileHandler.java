package se.idega.idegaweb.commune.block.importer.business;

import javax.ejb.*;

public interface NackaImportFileHandler extends com.idega.business.IBOService
{
 public void setOnlyImportRelations(boolean p0) throws java.rmi.RemoteException;
 public boolean handleRecords()throws java.rmi.RemoteException, java.rmi.RemoteException;
 public void setStartRecord(int p0) throws java.rmi.RemoteException;
 public void setImportFile(se.idega.idegaweb.commune.block.importer.data.ImportFile p0) throws java.rmi.RemoteException;
}
