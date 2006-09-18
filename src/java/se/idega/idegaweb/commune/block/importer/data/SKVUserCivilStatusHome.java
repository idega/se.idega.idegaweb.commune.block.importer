package se.idega.idegaweb.commune.block.importer.data;


import javax.ejb.CreateException;
import com.idega.data.IDOHome;
import javax.ejb.FinderException;

public interface SKVUserCivilStatusHome extends IDOHome {
	public SKVUserCivilStatus create() throws CreateException;

	public SKVUserCivilStatus findByPrimaryKey(Object pk) throws FinderException;

	public SKVUserCivilStatus findByStatusCode(String code) throws FinderException;
}