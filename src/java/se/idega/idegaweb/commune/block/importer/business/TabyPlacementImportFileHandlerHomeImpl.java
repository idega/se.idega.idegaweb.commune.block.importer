package se.idega.idegaweb.commune.block.importer.business;


public class TabyPlacementImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements TabyPlacementImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return TabyPlacementImportFileHandler.class;
 }


 public TabyPlacementImportFileHandler create() throws javax.ejb.CreateException{
  return (TabyPlacementImportFileHandler) super.createIBO();
 }



}