package se.idega.idegaweb.commune.block.importer.business;


public class NackaProgmaPlacementImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements NackaProgmaPlacementImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return NackaProgmaPlacementImportFileHandler.class;
 }


 public NackaProgmaPlacementImportFileHandler create() throws javax.ejb.CreateException{
  return (NackaProgmaPlacementImportFileHandler) super.createIBO();
 }



}