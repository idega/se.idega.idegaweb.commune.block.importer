package se.idega.idegaweb.commune.block.importer.business;


public class MusicSchoolImportFileHandlerHomeImpl extends com.idega.business.IBOHomeImpl implements MusicSchoolImportFileHandlerHome
{
 protected Class getBeanInterfaceClass(){
  return MusicSchoolImportFileHandler.class;
 }


 public MusicSchoolImportFileHandlerBean create() throws javax.ejb.CreateException{
  return (MusicSchoolImportFileHandlerBean) super.createIBO();
 }



}