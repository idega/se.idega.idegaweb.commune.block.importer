/**
 * 
 */
package se.idega.idegaweb.commune.block.importer.data;

import java.sql.Date;


import com.idega.data.IDOEntity;
import com.idega.user.data.User;

/**
 * @author bluebottle
 *
 */
public interface SKVUserExtraInfo extends IDOEntity {
	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setUserID
	 */
	public void setUserID(int userID);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setUser
	 */
	public void setUser(User user);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setUserCivilStatusID
	 */
	public void setUserCivilStatusID(int statusID);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setUserCivilStatus
	 */
	public void setUserCivilStatus(SKVUserCivilStatus status);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setCivilStatusDate
	 */
	public void setCivilStatusDate(Date statusDate);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setBirthCounty
	 */
	public void setBirthCounty(int county);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setBirthParish
	 */
	public void setBirthParish(String parish);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setForeignBirthCity
	 */
	public void setForeignBirthCity(String city);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setForeignBirthCountry
	 */
	public void setForeignBirthCountry(String country);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setImigrationDate
	 */
	public void setImigrationDate(Date imigrationDate);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setCitizenshipCode
	 */
	public void setCitizenshipCode(String code);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setCitizenshipDate
	 */
	public void setCitizenshipDate(Date citizenshipDate);

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getUserID
	 */
	public int getUserID();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getUser
	 */
	public User getUser();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getUserCivilStatusID
	 */
	public int getUserCivilStatusID();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getUserCivilStatus
	 */
	public SKVUserCivilStatus getUserCivilStatus();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getCivilStatusDate
	 */
	public Date getCivilStatusDate();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getBirthCounty
	 */
	public int getBirthCounty();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getBirthParish
	 */
	public String getBirthParish();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getForeignBirthCity
	 */
	public String getForeignBirthCity();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getForeignBirthCountry
	 */
	public String getForeignBirthCountry();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getImigrationDate
	 */
	public Date getImigrationDate();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#getCitizenshipCode
	 */
	public String getCitizenshipCode();

	/**
	 * @see se.idega.idegaweb.commune.block.importer.data.SKVUserExtraInfoBMPBean#setCitizenshipDate
	 */
	public Date setCitizenshipDate();

}
