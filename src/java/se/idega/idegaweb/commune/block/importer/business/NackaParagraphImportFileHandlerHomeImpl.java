package se.idega.idegaweb.commune.block.importer.business;


public class NackaParagraphImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements NackaParagraphImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return NackaParagraphImportFileHandler.class;
 }


 public NackaParagraphImportFileHandler create() throws javax.ejb.CreateException{
  return (NackaParagraphImportFileHandler) super.createIBO();
 }



}