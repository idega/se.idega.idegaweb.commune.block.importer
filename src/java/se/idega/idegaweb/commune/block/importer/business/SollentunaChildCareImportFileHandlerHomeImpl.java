package se.idega.idegaweb.commune.block.importer.business;


public class SollentunaChildCareImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements SollentunaChildCareImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return SollentunaChildCareImportFileHandler.class;
 }


 public SollentunaChildCareImportFileHandler create() throws javax.ejb.CreateException{
  return (SollentunaChildCareImportFileHandler) super.createIBO();
 }



}