package se.idega.idegaweb.commune.block.importer.data;


public interface PlacementImportDateHome extends com.idega.data.IDOHome
{
 public PlacementImportDate create() throws javax.ejb.CreateException;
 public PlacementImportDate findByPrimaryKey(Object pk) throws javax.ejb.FinderException;

}