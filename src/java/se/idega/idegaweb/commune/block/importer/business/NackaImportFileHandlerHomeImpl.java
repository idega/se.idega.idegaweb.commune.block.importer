package se.idega.idegaweb.commune.block.importer.business;


public class NackaImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements NackaImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return NackaImportFileHandler.class;
 }


 public NackaImportFileHandler create() throws javax.ejb.CreateException{
  return (NackaImportFileHandler) super.createIBO();
 }



}