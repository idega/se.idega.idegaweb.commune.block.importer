package se.idega.idegaweb.commune.block.importer.business;


public class MusicSchoolImportFileHandlerBeanHomeImpl extends com.idega.data.IDOFactory implements MusicSchoolImportFileHandlerBeanHome
{
 protected Class getEntityInterfaceClass(){
  return MusicSchoolImportFileHandlerBean.class;
 }


 public MusicSchoolImportFileHandlerBean create() throws javax.ejb.CreateException{
  return (MusicSchoolImportFileHandlerBean) super.createIDO();
 }


 public MusicSchoolImportFileHandlerBean findByPrimaryKey(Object pk) throws javax.ejb.FinderException{
  return (MusicSchoolImportFileHandlerBean) super.findByPrimaryKeyIDO(pk);
 }



}