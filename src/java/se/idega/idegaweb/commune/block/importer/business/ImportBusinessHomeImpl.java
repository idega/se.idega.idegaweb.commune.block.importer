package se.idega.idegaweb.commune.block.importer.business;


public class ImportBusinessHomeImpl extends com.idega.business.IBOHomeImpl implements ImportBusinessHome
{
 protected Class getBeanInterfaceClass(){
  return ImportBusiness.class;
 }


 public ImportBusiness create() throws javax.ejb.CreateException{
  return (ImportBusiness) super.createIBO();
 }



}