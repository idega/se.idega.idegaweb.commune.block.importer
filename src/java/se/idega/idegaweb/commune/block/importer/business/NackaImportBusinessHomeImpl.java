package se.idega.idegaweb.commune.block.importer.business;


public class NackaImportBusinessHomeImpl extends com.idega.business.IBOHomeImpl implements NackaImportBusinessHome
{
 protected Class getBeanInterfaceClass(){
  return NackaImportBusiness.class;
 }


 public NackaImportBusiness create() throws javax.ejb.CreateException{
  return (NackaImportBusiness) super.createIBO();
 }



}