package se.idega.idegaweb.commune.block.importer.business;


public class NackaHighSchoolPlacementImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements NackaHighSchoolPlacementImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return NackaHighSchoolPlacementImportFileHandler.class;
 }


 public NackaHighSchoolPlacementImportFileHandler create() throws javax.ejb.CreateException{
  return (NackaHighSchoolPlacementImportFileHandler) super.createIBO();
 }



}