package se.idega.idegaweb.commune.block.importer.business;


public class NackaPlacementImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements NackaPlacementImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return NackaPlacementImportFileHandler.class;
 }


 public NackaPlacementImportFileHandler create() throws javax.ejb.CreateException{
  return (NackaPlacementImportFileHandler) super.createIBO();
 }



}