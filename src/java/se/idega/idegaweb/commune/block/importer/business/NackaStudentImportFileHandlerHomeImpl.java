package se.idega.idegaweb.commune.block.importer.business;


public class NackaStudentImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements NackaStudentImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return NackaStudentImportFileHandler.class;
 }


 public NackaStudentImportFileHandler create() throws javax.ejb.CreateException{
  return (NackaStudentImportFileHandler) super.createIBO();
 }



}