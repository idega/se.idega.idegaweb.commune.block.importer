package se.idega.idegaweb.commune.block.importer.business;


public class NackaAfterSchoolFixFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements NackaAfterSchoolFixFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return NackaAfterSchoolFixFileHandler.class;
 }


 public NackaAfterSchoolFixFileHandler create() throws javax.ejb.CreateException{
  return (NackaAfterSchoolFixFileHandler) super.createIBO();
 }



}