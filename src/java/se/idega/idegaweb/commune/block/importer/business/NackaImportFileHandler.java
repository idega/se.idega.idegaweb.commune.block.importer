package se.idega.idegaweb.commune.block.importer.business;

import java.rmi.RemoteException;

import com.idega.block.importer.business.ImportFileHandler;
import com.idega.block.importer.data.ImportFile;
import com.idega.user.data.Group;

public interface NackaImportFileHandler extends com.idega.business.IBOService,ImportFileHandler
{
 public void setImportAddresses(boolean p0) throws java.rmi.RemoteException;
 public void setOnlyImportRelations(boolean p0) throws java.rmi.RemoteException;
 public void setImportUsers(boolean p0) throws java.rmi.RemoteException;
 public boolean handleRecords()throws java.rmi.RemoteException, java.rmi.RemoteException;
 public void setImportRelations(boolean p0) throws java.rmi.RemoteException;
 public void setStartRecord(int p0) throws java.rmi.RemoteException;
 public void setImportFile(ImportFile p0) throws java.rmi.RemoteException;
 public void setRootGroup(Group rootGroup) throws RemoteException;
} 
