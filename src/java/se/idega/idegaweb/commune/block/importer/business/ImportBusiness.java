package se.idega.idegaweb.commune.block.importer.business;

import javax.ejb.*;

public interface ImportBusiness extends com.idega.business.IBOService
{
 public se.idega.idegaweb.commune.block.importer.business.ImportFileHandler getHandlerForImportFile(java.lang.String p0) throws java.rmi.RemoteException;
 public se.idega.idegaweb.commune.block.importer.business.ImportFileHandler getHandlerForImportFile(java.lang.Class p0) throws java.rmi.RemoteException;
 public boolean importRecords(se.idega.idegaweb.commune.block.importer.data.ImportFile p0) throws java.rmi.RemoteException;
}
