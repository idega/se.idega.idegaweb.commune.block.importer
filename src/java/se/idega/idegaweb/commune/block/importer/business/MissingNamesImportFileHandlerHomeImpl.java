package se.idega.idegaweb.commune.block.importer.business;


public class MissingNamesImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements MissingNamesImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return MissingNamesImportFileHandler.class;
 }


 public MissingNamesImportFileHandler create() throws javax.ejb.CreateException{
  return (MissingNamesImportFileHandler) super.createIBO();
 }



}
