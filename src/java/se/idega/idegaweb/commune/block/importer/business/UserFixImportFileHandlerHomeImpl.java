package se.idega.idegaweb.commune.block.importer.business;


public class UserFixImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements UserFixImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return UserFixImportFileHandler.class;
 }


 public UserFixImportFileHandler create() throws javax.ejb.CreateException{
  return (UserFixImportFileHandler) super.createIBO();
 }



}