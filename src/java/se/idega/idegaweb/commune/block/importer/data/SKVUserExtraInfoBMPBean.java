package se.idega.idegaweb.commune.block.importer.data;

import java.sql.Date;

import javax.ejb.FinderException;

import com.idega.data.GenericEntity;
import com.idega.data.IDOQuery;
import com.idega.user.data.User;

public class SKVUserExtraInfoBMPBean extends GenericEntity implements
		SKVUserExtraInfo {

	protected static final String ENTITY_NAME = "skv_user_extra";

	protected static final String COLUMN_USER_ID = "ic_user_id";

	protected static final String COLUMN_USER_CIVIL_STATUS = "skv_user_civil_status_id";

	protected static final String COLUMN_CIVIL_STATUS_DATE = "civil_status_date";

	protected static final String COLUMN_BIRTH_COUNTY = "birth_county";

	protected static final String COLUMN_BIRTH_PARISH = "birth_parish";

	protected static final String COLUMN_FOREIGN_BIRTH_CITY = "foreign_birth_city";

	protected static final String COLUMN_FOREIGN_BIRTH_COUNTRY = "foreign_birth_country";

	protected static final String COLUMN_IMIGRATION_DATE = "imigration_date";

	protected static final String COLUMN_CITIZENSHIP_CODE = "citizenship_code";

	protected static final String COLUMN_CITIZENSHIP_DATE = "citizenship_date";

	public String getEntityName() {
		return ENTITY_NAME;
	}

	public void initializeAttributes() {
		addAttribute(getIDColumnName());
		addOneToOneRelationship(COLUMN_USER_ID, User.class);
		addManyToOneRelationship(COLUMN_USER_CIVIL_STATUS,
				SKVUserCivilStatus.class);
		addAttribute(COLUMN_CIVIL_STATUS_DATE, "Civil status date", Date.class);
		addAttribute(COLUMN_BIRTH_COUNTY, "Birth county", Integer.class);
		addAttribute(COLUMN_BIRTH_PARISH, "Birth parish", String.class);
		addAttribute(COLUMN_FOREIGN_BIRTH_CITY, "Foreign birth city",
				String.class);
		addAttribute(COLUMN_FOREIGN_BIRTH_COUNTRY, "Foreign birth country",
				String.class);
		addAttribute(COLUMN_IMIGRATION_DATE, "Imigration date", Date.class);
		addAttribute(COLUMN_CITIZENSHIP_CODE, "Citizenship code", String.class);
		addAttribute(COLUMN_CITIZENSHIP_DATE, "Citizenship date", Date.class);
	}

	// Setters
	public void setUserID(int userID) {
		setColumn(COLUMN_USER_ID, userID);
	}

	public void setUser(User user) {
		setColumn(COLUMN_USER_ID, user);
	}

	public void setUserCivilStatusID(int statusID) {
		setColumn(COLUMN_USER_CIVIL_STATUS, statusID);
	}

	public void setUserCivilStatus(SKVUserCivilStatus status) {
		setColumn(COLUMN_USER_CIVIL_STATUS, status);
	}

	public void setCivilStatusDate(Date statusDate) {
		setColumn(COLUMN_CIVIL_STATUS_DATE, statusDate);
	}

	public void setBirthCounty(int county) {
		setColumn(COLUMN_BIRTH_COUNTY, county);
	}

	public void setBirthParish(String parish) {
		setColumn(COLUMN_BIRTH_PARISH, parish);
	}

	public void setForeignBirthCity(String city) {
		setColumn(COLUMN_FOREIGN_BIRTH_CITY, city);
	}

	public void setForeignBirthCountry(String country) {
		setColumn(COLUMN_FOREIGN_BIRTH_COUNTRY, country);
	}

	public void setImigrationDate(Date imigrationDate) {
		setColumn(COLUMN_IMIGRATION_DATE, imigrationDate);
	}

	public void setCitizenshipCode(String code) {
		setColumn(COLUMN_CITIZENSHIP_CODE, code);
	}

	public void setCitizenshipDate(Date citizenshipDate) {
		setColumn(COLUMN_CITIZENSHIP_DATE, citizenshipDate);
	}

	// Getters
	public int getUserID() {
		return getIntColumnValue(COLUMN_USER_ID);
	}

	public User getUser() {
		return (User) getColumnValue(COLUMN_USER_ID);
	}

	public int getUserCivilStatusID() {
		return getIntColumnValue(COLUMN_USER_CIVIL_STATUS);
	}

	public SKVUserCivilStatus getUserCivilStatus() {
		return (SKVUserCivilStatus) getColumnValue(COLUMN_USER_CIVIL_STATUS);
	}

	public Date getCivilStatusDate() {
		return getDateColumnValue(COLUMN_CIVIL_STATUS_DATE);
	}

	public int getBirthCounty() {
		return getIntColumnValue(COLUMN_BIRTH_COUNTY);
	}

	public String getBirthParish() {
		return getStringColumnValue(COLUMN_BIRTH_PARISH);
	}

	public String getForeignBirthCity() {
		return getStringColumnValue(COLUMN_FOREIGN_BIRTH_CITY);
	}

	public String getForeignBirthCountry() {
		return getStringColumnValue(COLUMN_FOREIGN_BIRTH_COUNTRY);
	}

	public Date getImigrationDate() {
		return getDateColumnValue(COLUMN_IMIGRATION_DATE);
	}

	public String getCitizenshipCode() {
		return getStringColumnValue(COLUMN_CITIZENSHIP_CODE);
	}

	public Date setCitizenshipDate() {
		return getDateColumnValue(COLUMN_CITIZENSHIP_DATE);
	}

	public Object ejbFindByUser(User user) throws FinderException {
		return ejbFindByUserID(((Integer) user.getPrimaryKey()).intValue());
	}
	
	public Object ejbFindByUserID(int userID) throws FinderException {
		IDOQuery query = this.idoQueryGetSelect();
		query.appendWhereEquals(COLUMN_USER_ID, userID);
		
		return idoFindOnePKByQuery(query);
	}
}