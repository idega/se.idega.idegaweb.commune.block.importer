package se.idega.idegaweb.commune.block.importer.business;


public class NackaSchoolImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements NackaSchoolImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return NackaSchoolImportFileHandler.class;
 }


 public NackaSchoolImportFileHandler create() throws javax.ejb.CreateException{
  return (NackaSchoolImportFileHandler) super.createIBO();
 }



}