/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.data;


import com.idega.data.IDOEntity;

/**
 * @author bluebottle
 *
 */
public interface SKVUserCivilStatus extends IDOEntity {
	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserCivilStatusBMPBean#setStatusCode
	 */
	public void setStatusCode(String code);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserCivilStatusBMPBean#setLocalizedKey
	 */
	public void setLocalizedKey(String key);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserCivilStatusBMPBean#getStatusCode
	 */
	public String getStatusCode();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserCivilStatusBMPBean#getLocalizedKey
	 */
	public String getLocalizedKey();

}
