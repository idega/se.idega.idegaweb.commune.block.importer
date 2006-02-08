/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.data;


import com.idega.data.IDOHome;

/**
 * @author bluebottle
 *
 */
public interface SKVUserCivilStatusHome extends IDOHome {
	public SKVUserCivilStatus create() throws javax.ejb.CreateException;

	public SKVUserCivilStatus findByPrimaryKey(Object pk)
			throws javax.ejb.FinderException;

}
