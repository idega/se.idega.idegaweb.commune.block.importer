package se.idega.idegaweb.commune.block.importer.business;


public interface MusicSchoolImportFileHandlerBeanHome extends com.idega.data.IDOHome
{
 public MusicSchoolImportFileHandlerBean create() throws javax.ejb.CreateException;
 public MusicSchoolImportFileHandlerBean findByPrimaryKey(Object pk) throws javax.ejb.FinderException;

}