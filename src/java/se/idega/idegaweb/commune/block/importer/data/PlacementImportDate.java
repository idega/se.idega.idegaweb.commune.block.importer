package se.idega.idegaweb.commune.block.importer.data;


public interface PlacementImportDate extends com.idega.data.IDOEntity
{
 public java.lang.String getIDColumnName();
 public java.sql.Date getImportDate();
 public com.idega.block.school.data.SchoolClassMember getSchoolClassMember();
 public int getSchoolClassMemberId();
 public void initializeAttributes();
 public void setImportDate(java.sql.Date p0);
 public void setSchoolClassMemberId(int p0);
}
