package se.idega.idegaweb.commune.block.importer.data;


public class PlacementImportDateHomeImpl extends com.idega.data.IDOFactory implements PlacementImportDateHome
{
 protected Class getEntityInterfaceClass(){
  return PlacementImportDate.class;
 }


 public PlacementImportDate create() throws javax.ejb.CreateException{
  return (PlacementImportDate) super.createIDO();
 }


 public PlacementImportDate findByPrimaryKey(Object pk) throws javax.ejb.FinderException{
  return (PlacementImportDate) super.findByPrimaryKeyIDO(pk);
 }



}